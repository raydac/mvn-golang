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

import javax.annotation.Nonnull;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.ArrayUtils;

/**
 * The Mojo allows to run a program, it wraps <b>run</b> command.
 * 
 * @since 2.0.0
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangRunMojo extends AbstractPackageGolangMojo {

  /**
   * If the parameter is defined then <b>-exec</b> will be used with the parameter value.
   * @since 2.0.0
   */
  @Parameter(name = "xprog")
  private String xprog;
  
  @Override
  public boolean isSourceFolderRequired() {
    return true;
  }

  @Override
  @Nonnull
  @MustNotContainNull
  protected String[] getExtraBuildFlags() {
    String [] result = ArrayUtils.EMPTY_STRING_ARRAY;
    if (this.xprog != null){
      result = new String[]{"-exec",this.xprog};
    }
    return result;
  }
  
  @Override
  @Nonnull
  public String getGoCommand() {
    return "run";
  }

  @Override
  public boolean enforcePrintOutput() {
    return true;
  }

}
