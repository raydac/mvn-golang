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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.transfer.artifact.install.ArtifactInstaller;
import org.apache.maven.shared.transfer.repository.RepositoryManager;

/**
 * The Mojo packs all found source and resource project folders and create new
 * artifact in the local repository.
 *
 * @since 2.1.0
 */
@Mojo(name = "mvninstall", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangMvnInstallMojo extends AbstractMojo {

  @Component
  protected RepositoryManager repositoryManager;

  @Component
  protected ArtifactInstaller installer;

  @Component
  protected MavenProjectHelper projectHelper;

  @Parameter(readonly = true, required = true, defaultValue = "${project}")
  private MavenProject project;

  @Parameter(readonly = true, required = true, defaultValue = "${session}")
  private MavenSession session;

  /**
   * Class replacing artifact extension by 'zip'.
   *
   * @since 2.2.1
   */
  private class ZipExtensionArtifactHandlerAdapter implements ArtifactHandler {

    private final ArtifactHandler delegate;

    private ZipExtensionArtifactHandlerAdapter(@Nonnull final ArtifactHandler delegate) {
      this.delegate = delegate;
    }

    @Nonnull
    @Override
    public String getExtension() {
      final String oldExtension = this.delegate.getExtension();
      getLog().debug("Replacing artifact extension '" + oldExtension + "' by 'zip' extension");
      return "zip";
    }

    @Nonnull
    @Override
    public String getDirectory() {
      return this.delegate.getDirectory();
    }

    @Nonnull
    @Override
    public String getClassifier() {
      return this.delegate.getClassifier();
    }

    @Nonnull
    @Override
    public String getPackaging() {
      return this.delegate.getPackaging();
    }

    @Override
    public boolean isIncludesDependencies() {
      return this.delegate.isIncludesDependencies();
    }

    @Nonnull
    @Override
    public String getLanguage() {
      return this.delegate.getLanguage();
    }

    @Override
    public boolean isAddedToClasspath() {
      return this.delegate.isAddedToClasspath();
    }
  }

  /**
   * Set this to 'true' to bypass artifact install.
   *
   * @since 2.1.8
   */
  @Parameter(property = "maven.install.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Compression level of zip file. Must be 1..9
   *
   * @since 2.1.0
   */
  @Parameter(name = "compression", defaultValue = "9")
  private int compression;

  public void setCompression(final int level) {
    this.compression = level;
  }

  public int getCompression() {
    return this.compression;
  }

  public boolean isSkip() {
    return this.skip;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!this.isSkip()) {
      try {
        final File archive = compressProjectFiles();
        this.project.getArtifact().setFile(archive);
        this.project.getArtifact().setArtifactHandler(new ZipExtensionArtifactHandlerAdapter(this.project.getArtifact().getArtifactHandler()));
      } catch (IOException ex) {
        throw new MojoExecutionException("Detected unexpected IOException, check the log!", ex);
      }
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
