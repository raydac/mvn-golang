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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Mojo wraps the 'list' command.
 *
 * @since 2.3.8
 */
@Mojo(name = "list", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangListMojo extends AbstractModuleAware {

  /**
   * Alternate format for the list, using the syntax of package template.
   */
  @Parameter(name = "format")
  private String format = null;

  /**
   * The flag causes the package data to be printed in JSON format instead of using the template format.
   */
  @Parameter(name = "json")
  private boolean json;

  /**
   * The flag causes list to list modules instead of packages.
   */
  @Parameter(name = "listModules")
  private boolean listModules;

  public boolean isListModules() {
    return this.listModules;
  }

  public boolean isJson() {
    return this.json;
  }

  @Nonnull
  @MustNotContainNull
  @Override
  protected String[] getAdditionalCommandFlags() {
    final List<String> result = new ArrayList<>();
    Collections.addAll(result, super.getAdditionalCommandFlags());

    if (this.getFormat() != null && this.getFormat().trim().length() != 0) {
      if (result.contains("-f")) {
        this.getLog().warn("Format ignored because detected already defined '-f' option");
      } else {
        result.add("-f");
        result.add(this.getFormat().trim());
      }
    }

    if (this.isJson()) {
      if (result.contains("-json")) {
        this.getLog().warn("Json flag ignored because detected already defined '-json' option");
      } else {
        result.add("-json");
      }
    }

    if (this.isListModules()) {
      if (result.contains("-m")) {
        this.getLog().warn("Module flag ignored because detected already defined '-m' option");
      } else {
        result.add("-m");
      }
    }

    return result.toArray(new String[0]);
  }

  @Nullable
  public String getFormat() {
    return this.format;
  }

  @Nullable
  @Override
  protected String getSkipMojoPropertySuffix() {
    return "list";
  }

  @Override
  @Nonnull
  public String getGoCommand() {
    return "list";
  }


}
