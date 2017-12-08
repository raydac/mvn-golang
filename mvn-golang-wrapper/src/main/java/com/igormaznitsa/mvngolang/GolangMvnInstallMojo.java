/*
 * Copyright 2016 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.mvngolang;

import static com.igormaznitsa.mvngolang.utils.IOUtils.closeSilently;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Locale;

/**
 * The Mojo packs all found source and resource project folders and create new artifact in the local repository.
 *
 * @since 2.1.0
 */
@Mojo(name = "mvninstall", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangMvnInstallMojo extends AbstractMojo {

    @Parameter(property = "localRepository", required = true, readonly = true)
    protected ArtifactRepository localRepository;

    @Component
    protected ArtifactInstaller installer;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    /**
     * Compression level of zip file. Must be 1..9
     *
     * @since 2.1.0
     */
    @Parameter(name = "compression", defaultValue = "9")
    private int compression;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final File archive = compressProjectFiles();
            try {
                this.installer.install(archive, project.getArtifact(), this.localRepository);
            } finally {
                FileUtils.deleteQuietly(archive);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Detected unexpected IOException, check the log!", ex);
        } catch (ArtifactInstallationException ex) {
            throw new MojoFailureException("Can't install the artifact!");
        }
    }

    private void safeCopyDirectory(@Nullable final String src, @Nonnull final File dst) throws IOException {
        if (src == null || src.isEmpty()) {
            return;
        }
        final File srcFile = new File(src);
        if (srcFile.isDirectory()) {
            if (getLog().isDebugEnabled()) {
                getLog().debug(String.format("Copying %s => %s", srcFile.getAbsolutePath(), dst.getAbsolutePath()));
            }
            FileUtils.copyDirectoryToDirectory(srcFile, dst);
        }
    }

    private void saveEffectivePom(@Nonnull final File folder) throws IOException {
        final Model model = this.project.getModel();
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(new File(folder, "pom.xml"), false), "UTF-8");
            new MavenXpp3Writer().write(writer, model);
            if (getLog().isDebugEnabled()) {
                getLog().debug("Effective pom has been written");
            }
        } finally {
            closeSilently(writer);
        }
    }

    @Nonnull
    private File compressProjectFiles() throws IOException {
        final Artifact artifact = this.project.getArtifact();
        final File resultZip = new File(this.project.getBuild().getDirectory(), artifact.getArtifactId() + '-' + artifact.getVersion() + '.' + artifact.getType());
        if (resultZip.isFile() && !resultZip.delete()) {
            throw new IOException("Can't delete file : " + resultZip);
        }

        final File folderToPack = new File(".tmp_pack_folder_" + Long.toHexString(System.currentTimeMillis()).toUpperCase(Locale.ENGLISH));
        if (folderToPack.isDirectory()) {
            FileUtils.deleteDirectory(folderToPack);
        }
        if (!folderToPack.mkdirs()) {
            throw new IOException("Can't create temp folder : " + folderToPack);
        }

        try {
            saveEffectivePom(folderToPack);

            FileUtils.copyFileToDirectory(this.project.getFile(), folderToPack);
            safeCopyDirectory(this.project.getBuild().getSourceDirectory(), folderToPack);
            safeCopyDirectory(this.project.getBuild().getTestSourceDirectory(), folderToPack);

            for (final Resource res : this.project.getBuild().getResources()) {
                safeCopyDirectory(res.getDirectory(), folderToPack);
            }

            for (final Resource res : this.project.getBuild().getTestResources()) {
                safeCopyDirectory(res.getDirectory(), folderToPack);
            }

            if (getLog().isDebugEnabled()) {
                getLog().debug(String.format("Packing folder %s to %s", folderToPack.getAbsolutePath(), resultZip.getAbsolutePath()));
            }

            ZipUtil.pack(folderToPack, resultZip, Math.min(9, Math.max(1, this.compression)));

        } finally {
            FileUtils.deleteQuietly(folderToPack);
        }

        return resultZip;
    }

}
