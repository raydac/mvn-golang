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

import java.io.File;
import java.io.IOException;
import javax.annotation.Nonnull;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.FileUtils;

/**
 * The Mojo wraps the 'clean' command.
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangCleanMojo extends AbstractPackageGolangMojo {

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
  public void afterExecution(final boolean error) throws MojoFailureException {
    if (!error) {
      final File directory;
      if (getProject().getPackaging().equals("mvn-golang")) {
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
    }
  }
}
