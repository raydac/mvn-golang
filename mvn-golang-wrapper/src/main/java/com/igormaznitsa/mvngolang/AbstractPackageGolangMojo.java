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
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.mvngolang.utils.IOUtils;
import com.igormaznitsa.mvngolang.utils.MavenUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.zeroturnaround.zip.ZipUtil;

public abstract class AbstractPackageGolangMojo extends AbstractGolangMojo {

  /**
   * List of packages.
   */
  @Parameter(name = "packages")
  private String[] packages;

  /**
   * Find artifacts generated by Mvn-Golang among scope dependencies, unpack
   * them and add unpacked folders into GOPATHduring execution.
   *
   * @since 2.3.0
   */
  @Parameter(name = "scanDependencies", defaultValue = "true")
  private boolean scanDependencies = true;

  /**
   * Path to the folder where all found mvn-golang dependencies will be
   * unpacked.
   *
   * @since 2.3.0
   */
  @Parameter(name = "unpackDependencyFolder", defaultValue = "${project.build.directory}${file.separator}.__deps__")
  private String unpackDependencyFolder;

  @Nonnull
  public String getUnpackDependencyFolder() {
    return GetUtils.ensureNonNull(this.unpackDependencyFolder, this.getProject().getBuild().getDirectory() + File.separator + "$$$deps$$$");
  }

  @Nullable
  @MustNotContainNull
  protected String[] getDefaultPackages() {
    return null;
  }

  @Nullable
  @MustNotContainNull
  public String[] getPackages() {
    return this.packages == null ? this.getDefaultPackages() : this.packages.clone();
  }

  /**
   * Internal variable to keep GOPATH part containing folders of unpacked
   * mvn-golang dependencies.
   *
   * @since 2.3.0
   */
  private String extraGoPathSectionInOsFormat = "";

  public boolean isScanDependencies() {
    return this.scanDependencies;
  }

  public void setScanDependencies(final boolean flag) {
    this.scanDependencies = flag;
  }

  public void setPackages(@Nullable @MustNotContainNull final String[] value) {
    this.packages = value;
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getTailArguments() {
    return GetUtils.ensureNonNull(getPackages(), ArrayUtils.EMPTY_STRING_ARRAY);
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCommandFlags() {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Nonnull
  @Override
  protected final String getSpecialPartOfGoPath() {
    return this.extraGoPathSectionInOsFormat;
  }

  @Override
  public final void doInit() throws MojoFailureException, MojoExecutionException {
    if (this.isScanDependencies()) {
      getLog().info("Scanning maven dependencies");
      final List<File> foundArtifacts;
      
      try {
        foundArtifacts = MavenUtils.scanForMvnGoArtifacts(
                this.getProject(),
                this,
                this.getSession(),
                this.getExecution(),
                this.getArtifactResolver(),
                this.getRemoteRepositories());
      } catch (ArtifactResolverException ex) {
        throw new MojoFailureException("Can't resolve artifact", ex);
      }
      
      if (foundArtifacts.isEmpty()) {
        getLog().debug("Mvn golang dependencies are not found");
        this.extraGoPathSectionInOsFormat = "";
      } else {
        getLog().debug("Found mvn-golang artifactis: " + foundArtifacts);
        final File targetFolder = new File(this.getUnpackDependencyFolder());
        getLog().debug("Depedencies will be unpacked into folder: " + targetFolder);
        final List<File> unpackedFolders = unpackArtifactsIntoFolder(foundArtifacts, targetFolder);

        final String preparedExtraPartForGoPath = IOUtils.makeOsFilePathWithoutDuplications(unpackedFolders.toArray(new File[0]));
        getLog().debug("Prepared dependency path for GOPATH: " + preparedExtraPartForGoPath);
        this.extraGoPathSectionInOsFormat = preparedExtraPartForGoPath;
      }
    } else {
      getLog().info("Maven dependency scanning is off");
    }
  }

  @Nonnull
  @MustNotContainNull
  private List<File> unpackArtifactsIntoFolder(@Nonnull @MustNotContainNull final List<File> zippedArtifacts, @Nonnull final File targetFolder) throws MojoExecutionException {
    final List<File> resultFolders = new ArrayList<>();

    if (!targetFolder.isDirectory() && !targetFolder.mkdirs()) {
      throw new MojoExecutionException("Can't create folder to unpack dependencies: " + targetFolder);
    }

    for (final File zipFile : zippedArtifacts) {
      final File outDir = new File(targetFolder, FilenameUtils.getBaseName(zipFile.getName()));
      if (outDir.isDirectory()) {
        getLog().debug("Dependency already unpacked: " + outDir);
        resultFolders.add(outDir);
      } else {
        try {
          getLog().debug("Unpack dependency archive: " + zipFile);
          ZipUtil.unpack(zipFile, outDir, StandardCharsets.UTF_8);
          resultFolders.add(outDir);
        } catch (Exception ex) {
          throw new MojoExecutionException("Can't unpack dependency archive '" + zipFile.getName() + "' into folder '" + targetFolder + '\'', ex);
        }
      }
    }
    return resultFolders;
  }
}
