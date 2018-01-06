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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Mojo allows to run a program, it wraps <b>run</b> command.
 *
 * @since 2.0.0
 */
@SuppressWarnings("SpellCheckingInspection")
@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangRunMojo extends AbstractPackageGolangMojo {

  /**
   * If the parameter is defined then <b>-exec</b> will be used with the parameter value.
   *
   * @since 2.0.0
   */
  @Parameter(name = "xprog")
  private String xprog;

  /**
   * Command arguments. They follow package names in command line..
   *
   * @since 2.1.7
   */
  @Parameter(name = "args")
  private String[] args;

  /**
   * Get command line arguments.
   *
   * @return array of arguments, must not be null
   * @since 2.1.7
   */
  @Nonnull
  @MustNotContainNull
  public String[] getArgs() {
    return this.args == null ? ArrayUtils.EMPTY_STRING_ARRAY : this.args.clone();
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getTailArguments() {
    final List<String> result = new ArrayList<>(Arrays.asList(super.getTailArguments()));
    result.addAll(Arrays.asList(this.getArgs()));
    return result.toArray(new String[result.size()]);
  }

  @Override
  public boolean isSourceFolderRequired() {
    return true;
  }

  @Override
  @Nonnull
  @MustNotContainNull
  protected String[] getExtraBuildFlags() {
    String[] result = ArrayUtils.EMPTY_STRING_ARRAY;
    if (this.xprog != null) {
      result = new String[] {"-exec", this.xprog};
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
