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

import com.igormaznitsa.mvngolang.utils.ProxySettings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * The Mojo wraps the 'clean' command.
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangCleanMojo extends AbstractGoPackageAndDependencyAwareMojo {

  /**
   * Clean all folders provided by Go Path. <b>Be careful with the flag!</b>
   *
   * @since 2.1.1
   */
  @Parameter(name = "cleanGoPath", defaultValue = "false")
  private boolean cleanGoPath;

  /**
   * Delete plugin Golang store folder It is folder where the plugin keeps
   * cached SDKs and packages.
   *
   * @since 2.1.1
   */
  @Parameter(name = "deleteStoreFolder", defaultValue = "false")
  private boolean deleteStoreFolder;

  @Override
  public boolean isSourceFolderRequired() {
    return true;
  }

  @Override
  @Nonnull
  public String getGoCommand() {
    return "clean";
  }

  @Override
  public void beforeExecution(@Nullable final ProxySettings proxySettings) throws MojoFailureException, MojoExecutionException {
    super.beforeExecution(proxySettings);
    if (this.isModuleMode() && this.getBuildFlags().length == 0) {
      this.addTmpBuildFlagIfNotPresented("-modcache");
    }
  }

  private void deleteStoreFolder() throws MojoFailureException {
    try {
      final File goStoreFolder = new File(getStoreFolder());
      if (goStoreFolder.isDirectory()) {
        getLog().info("Deleting the Store Folder : " + goStoreFolder);
        FileUtils.deleteDirectory(goStoreFolder);
      } else {
        getLog().info("The Store Folder does not found : " + goStoreFolder);
      }
    } catch (IOException ex) {
      throw new MojoFailureException("Can't delete the Store Folder", ex);
    }
  }

  private void cleanGoPath() throws MojoFailureException {
    try {
      final File[] goPathFolders = findGoPath(false);
      for (final File f : goPathFolders) {
        if (f.isDirectory()) {
          getLog().warn("Cleaning the Go Path folder : " + f);
          FileUtils.cleanDirectory(f);
        } else {
          getLog().info("Can't find GOPATH folder : " + f);
        }
      }
    } catch (IOException ex) {
      throw new MojoFailureException("Can't clean the Go Path folder", ex);
    }
  }

  @Override
  public void afterExecution(@Nullable final ProxySettings proxySettings, final boolean error) throws MojoFailureException, MojoExecutionException {
    if (!error) {
      final File directory;
      if (getProject().getPackaging().equals(GOARTIFACT_PACKAGING)) {
        directory = new File(getProject().getBasedir(), "bin");
      } else {
        directory = new File(getProject().getBuild().getDirectory());
      }

      if (directory.isDirectory()) {
        try {
          getLog().info("Deleting folder : " + directory);
          FileUtils.deleteDirectory(directory);
        } catch (IOException ex) {
          throw new MojoFailureException("Can't delete folder", ex);
        }
      } else {
        getLog().info(String.format("Folder %s is not found", directory.getAbsolutePath()));
      }

      final File reportFolderFile = new File(getReportsFolder());
      if (reportFolderFile.isDirectory()) {
        try {
          getLog().info("Deleting report folder : " + reportFolderFile);
          FileUtils.deleteDirectory(reportFolderFile);
        } catch (IOException ex) {
          throw new MojoExecutionException("Can't delete report folder : " + reportFolderFile);
        }
      } else {
        getLog().debug("There is no report folder : " + reportFolderFile);
      }

      if (this.cleanGoPath) {
        cleanGoPath();
      }

      if (this.deleteStoreFolder) {
        deleteStoreFolder();
      }
    }
  }
}
