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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import com.igormaznitsa.meta.common.utils.GetUtils;

/**
 * The Mojo wraps the 'tool' command.
 */
@Mojo(name = "tool", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangToolMojo extends AbstractGolangMojo {

  /**
   * The Command to be executed.
   */
  @Parameter(name = "command", required = true)
  private String command;

  /**
   * Command arguments.
   */
  @Parameter(name = "args")
  private String [] args;
  
  @Nullable
  @MustNotContainNull
  public String [] getArgs(){
    return this.args == null ? null : this.args.clone();
  }
  
  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getTailArguments() {
    final List<String> result = new ArrayList<>();
    result.add(this.command);
    for(final String s : GetUtils.ensureNonNull(this.args, ArrayUtils.EMPTY_STRING_ARRAY)) {
      result.add(s);
    }
    return result.toArray(new String[result.size()]);
  }

  @Nonnull
  public String getCommand() {
    return this.command;
  }
  
  @Override
  @Nonnull
  public String getGoCommand() {
    return "tool";
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCommandFlags() {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }
  
}
