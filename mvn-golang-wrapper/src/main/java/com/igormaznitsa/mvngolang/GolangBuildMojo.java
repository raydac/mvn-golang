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

import javax.annotation.Nonnull;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import com.igormaznitsa.meta.common.utils.GetUtils;

/**
 * The Mojo wraps the 'build' command.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangBuildMojo extends AbstractGolangMojo {

  /**
   * Target folder where to place the result file.
   */
  @Parameter(name = "target", defaultValue = "${project.build.directory}")
  private String target;

  /**
   * Name of the result file.
   */
  @Parameter(name = "name", defaultValue = "undefined", required = true)
  private String name;
  
  /**
   * List of packages to be built.
   */
  @Parameter(name="packages")
  private String [] packages;
  
  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCLITailArgs() {
    return GetUtils.ensureNonNull(this.packages, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  @Nonnull
  private File getTargetFile(){
    return new File(getTarget(),this.name);
  }
  
  @Nonnull
  public String getTarget() {
    return this.target;
  }

  @Nonnull
  public String getName() {
    return this.name;
  }

  @Override
  @Nonnull
  public String getCommand() {
    return "build";
  }

  @Override
  public void afterExecution(final boolean error) throws MojoFailureException {
    if (!error){
      final File targetFile = getTargetFile();
      if (!targetFile.isFile()) {
        throw new MojoFailureException("Can't find generated target file : "+targetFile);
      }
    }
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCommandFlags() {
    return new String[]{"-o", getTargetFile().getAbsolutePath()};
  }
  
}
