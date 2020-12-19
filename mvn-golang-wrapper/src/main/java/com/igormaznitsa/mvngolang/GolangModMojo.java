/*
 * Copyright 2019 Igor Maznitsa.
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
import com.igormaznitsa.meta.common.utils.Assertions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * The Mojo wraps the 'mod' command.
 *
 * @since 2.3.3
 */
@Mojo(name = "mod", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangModMojo extends AbstractGoDependencyAwareMojo {

  /**
   * Command to be executed. Must be defined.
   */
  @Parameter(name = "command", required = true)
  private String command;

  /**
   * Arguments for the command.
   */
  @Parameter(name = "arguments")
  private String[] arguments;

  @Override
  protected boolean doesNeedSessionLock() {
    return this.getSession().isParallel();
  }

  @Override
  public boolean isModuleMode() {
    return true;
  }

  @Nonnull
  @MustNotContainNull
  @Override
  public String[] getCommandFlags() {
    return new String[] {Assertions.assertNotNull(this.command)};
  }

  @Nonnull
  @MustNotContainNull
  @Override
  public String[] getTailArguments() {
    return this.arguments == null ? new String[0] : this.arguments;
  }

  @Nonnull
  public String getCommand() {
    return this.command;
  }

  public void setCommand(@Nonnull final String value) {
    this.command = value;
  }

  @Override
  @Nonnull
  public String getGoCommand() {
    return "mod";
  }

  @Nullable
  @Override
  protected String getSkipMojoPropertySuffix() {
    return "mod";
  }

  @Override
  public boolean isEnforcePrintOutput() {
    return true;
  }

}
