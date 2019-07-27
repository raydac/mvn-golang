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

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import static com.igormaznitsa.mvngolang.utils.IOUtils.closeSilently;
import com.igormaznitsa.mvngolang.utils.MavenUtils;
import com.igormaznitsa.mvngolang.utils.ProxySettings;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.transfer.artifact.install.ArtifactInstaller;
import org.apache.maven.shared.transfer.repository.RepositoryManager;
import org.zeroturnaround.zip.ZipUtil;

/**
 * The Mojo packs all found source and resource project folders and create new
 * artifact in the local repository.
 *
 * @since 2.1.0
 */
@Mojo(name = "mvninstall", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangMvnInstallMojo extends AbstractGoDependencyAwareMojo {

  /**
   * Special file contains list of mvn-golang artifacts which must be resolved
   * and used in build.
   *
   * @since 2.3.0
   */
  public static final String MVNGOLANG_DEPENDENCIES_FILE = ".mvn-golang-dependencies";

  /**
   * Special file contains list of project source and resource folders which
   * play role in project build.
   *
   * @since 2.3.3
   */
  public static final String MVNGOLANG_BUILD_FOLDERS_FILE = ".mvn-golang-build-folders";

  @Component
  protected RepositoryManager repositoryManager;

  @Component
  protected ArtifactInstaller installer;

  @Component
  protected MavenProjectHelper projectHelper;

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

  @Override
  protected String getSkipMojoPropertySuffix() {
    return "install";
  }
  
  @Override
  public boolean isSkip() {
    return super.isSkip() 
            || Boolean.parseBoolean(MavenUtils.findProperty(this.getProject(), "maven.install.skip", "false"));
  }

  @Override
  protected boolean doMainBusiness(@Nonnull final ProxySettings proxySettings, final int maxAttempts) throws InterruptedException, MojoFailureException, MojoExecutionException, IOException {
    final File archive = compressProjectFiles();
    this.getProject().getArtifact().setFile(archive);
    return false;
  }

  private void safeCopyDirectory(@Nullable final String src, @Nonnull final File dst, @Nullable @MustNotContainNull final List<File> dstList) throws IOException {
    if (!(src == null || src.isEmpty())) {
      final File srcFile = new File(src);
      if (srcFile.isDirectory()) {
        if (getLog().isDebugEnabled()) {
          getLog().debug(String.format("Copying %s => %s", srcFile.getAbsolutePath(), dst.getAbsolutePath()));
        }
        FileUtils.copyDirectoryToDirectory(srcFile, dst);
        if (dstList != null) {
          dstList.add(new File(dst, srcFile.getName()));
        }
      }
    }
  }

  private void saveEffectivePom(@Nonnull final File folder) throws IOException {
    final Model model = this.getProject().getModel();
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
    final Artifact artifact = this.getProject().getArtifact();

    File buildFolder = new File(this.getProject().getBuild().getDirectory());
    if (!buildFolder.isDirectory() && !buildFolder.mkdirs()) {
      this.getLog().error("Can't create build folder: " + buildFolder);
      throw new IOException("Can't create build folder: " + buildFolder);
    }

    File resultZip = new File(buildFolder, artifact.getArtifactId() + '-' + artifact.getVersion() + '.' + artifact.getType());
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

    final File mvnGolangDependencyListFile = new File(folderToPack, MVNGOLANG_DEPENDENCIES_FILE);
    final File mvnGolangBuildFolderListFile = new File(folderToPack, MVNGOLANG_BUILD_FOLDERS_FILE);
    try {
      saveEffectivePom(folderToPack);

      final List<File> buildFolders = new ArrayList<>();

      FileUtils.copyFileToDirectory(this.getProject().getFile(), folderToPack);

      safeCopyDirectory(this.getProject().getBuild().getTestSourceDirectory(), folderToPack, null);

      for (final Resource res : this.getProject().getBuild().getTestResources()) {
        safeCopyDirectory(res.getDirectory(), folderToPack, null);
      }

      for (final Resource res : this.getProject().getBuild().getResources()) {
        safeCopyDirectory(res.getDirectory(), folderToPack, buildFolders);
      }

      safeCopyDirectory(this.getSources(false).getAbsolutePath(), folderToPack, buildFolders);

      if (getLog().isDebugEnabled()) {
        getLog().debug(String.format("Packing folder %s to %s", folderToPack.getAbsolutePath(), resultZip.getAbsolutePath()));
      }

      if (mvnGolangBuildFolderListFile.isFile()) {
        this.getLog().warn("Skip build source folder list descriptor create because detected existing one: " + MVNGOLANG_BUILD_FOLDERS_FILE);
      } else {
        if (buildFolders.isEmpty()) {
          this.getLog().warn("Skip build source folder list descriptor because there is not any source or resource folders to be used for build");
        } else {
          final String rooPath = FilenameUtils.separatorsToUnix(FilenameUtils.normalize(folderToPack.getAbsolutePath()));
          final StringBuilder buffer = new StringBuilder();
          for (final File f : buildFolders) {
            String relativePath = FilenameUtils.separatorsToUnix(FilenameUtils.normalize(f.getAbsolutePath())).substring(rooPath.length() + 1);
            if (buffer.length() > 0) {
              buffer.append('\n');
            }
            this.getLog().debug("Add build folder into descriptor: " + relativePath);
            buffer.append(relativePath);
          }
          final String fileContent = buffer.toString();
          this.getLog().debug("Formed list of mvn-golang project source and resource build folders\n---------" + fileContent + "---------");
          FileUtils.writeStringToFile(mvnGolangBuildFolderListFile, fileContent, StandardCharsets.UTF_8);
        }
      }

      if (mvnGolangDependencyListFile.isFile()) {
        this.getLog().warn("Skip dependency descriptor create because detected existing one: " + MVNGOLANG_DEPENDENCIES_FILE);
      } else {
        final List<Artifact> golangDependencies = new ArrayList<>();
        MavenProject currentProject = this.getProject();
        while (currentProject != null && !Thread.currentThread().isInterrupted()) {
          final Set<Artifact> dependencies = currentProject.getDependencyArtifacts();
          if (dependencies != null) {
            for (final Artifact a : dependencies) {
              if (AbstractGolangMojo.GOARTIFACT_PACKAGING.equals(a.getType())) {
                golangDependencies.add(a);
              }
            }
          }
          currentProject = currentProject.getParent();
        }

        final StringBuilder buffer = new StringBuilder();
        for (final Artifact a : golangDependencies) {
          buffer.append(MavenUtils.makeArtifactRecord(a)).append('\n');
        }
        final String flagFileContent = buffer.toString();
        this.getLog().debug("Formed list of mvn-golang dependencies\n---------" + flagFileContent + "---------");
        FileUtils.writeStringToFile(mvnGolangDependencyListFile, flagFileContent, StandardCharsets.UTF_8);
      }

      this.getLog().debug("Restoring all backup go.mod in prepared folder to pack");
      restoreAllBackupGoMod(folderToPack);

      ZipUtil.pack(folderToPack, resultZip, Math.min(9, Math.max(1, this.compression)));
    } finally {
      FileUtils.deleteQuietly(folderToPack);
    }

    return resultZip;
  }

  private void restoreAllBackupGoMod(@Nonnull final File folder) throws IOException {
    final File backup = new File(folder, GO_MOD_FILE_NAME_BAK);
    if (backup.isFile()) {
      final File gomod = new File(folder, GO_MOD_FILE_NAME);
      if (gomod.isFile() && !gomod.delete()) {
        throw new IOException("Can't delete " + gomod);
      }
      if (!backup.renameTo(gomod)) {
        throw new IOException("Can't rename " + backup);
      }
    }

    for (final File f : folder.listFiles(new FileFilter() {
      @Override
      public boolean accept(@Nonnull final File pathname) {
        return pathname.isDirectory() && !Files.isSymbolicLink(pathname.toPath());
      }
    })) {
      restoreAllBackupGoMod(f);
    }
  }

}
