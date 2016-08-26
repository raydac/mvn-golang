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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.mvngolang.utils.StringComparatorABC;

/**
 * The Mojo wraps the 'test' command.
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class GolangTestMojo extends AbstractPackageGolangMojo {

  /**
   * List of test binary flags.
   */
  @Parameter(name = "testFlags")
  private String[] testFlags;

  @Override
  protected String[] getDefaultPackages() {
    return new String[]{'.' + File.separator + "..."};
  }

  @Nullable
  @MustNotContainNull
  public String[] getTestFlags() {
    return this.testFlags == null ? null : this.testFlags.clone();
  }

  @Override
  public boolean isSourceFolderRequired() {
    return true;
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getOptionalExtraTailArguments() {
    return GetUtils.ensureNonNull(this.testFlags, ArrayUtils.EMPTY_STRING_ARRAY);
  }

  @Override
  @Nonnull
  public String getGoCommand() {
    return "test";
  }

  @Override
  public boolean enforcePrintOutput() {
    return isVerbose();
  }

}
