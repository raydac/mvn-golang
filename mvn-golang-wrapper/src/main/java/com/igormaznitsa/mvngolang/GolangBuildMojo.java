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
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.mvngolang.utils.ProxySettings;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.igormaznitsa.meta.common.utils.Assertions.assertNotNull;
import static java.util.Objects.requireNonNull;

/**
 * The Mojo wraps the 'build' command.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangBuildMojo extends AbstractGoPackageAndDependencyAwareMojo {

  /**
   * Target folder where to place the result file.
   */
  @Parameter(name = "resultFolder", defaultValue = "${project.build.directory}")
  private String resultFolder;

  /**
   * Name of the result file. NB! Keep in mind that in the case of gomobile you
   * must define right extension!
   * <b>By default it uses ${project.build.finalName}</b>
   */
  @Parameter(name = "resultName", defaultValue = "${project.build.finalName}")
  private String resultName;

  /**
   * Build mode indicates which kind of object file is to be built.
   *
   * @since 2.1.3
   */
  @Parameter(name = "buildMode", defaultValue = "default")
  private String buildMode;

  /**
   * Strip result file. Symbol table and DWARF will be removed from the result
   * file.
   *
   * @since 2.1.3
   */
  @Parameter(name = "strip", defaultValue = "false")
  private boolean strip;

  /**
   * List of linker flags.
   * <pre>
   * {@code
   *      <ldFlags>
   *        <ldFlag>-a</ldFlag>
   *        <ldFlag>main.prodVersion=1.2.3</ldFlag>
   *      </ldFlags>
   * }
   * </pre>
   *
   * @since 2.1.3
   */
  @Parameter(name = "ldFlags")
  private String[] ldFlags;

  @MustNotContainNull
  @Nonnull
  public List<String> getLdflagsAsList() {
    return this.ldFlags == null ? new ArrayList<String>() : new ArrayList<>(Arrays.asList(this.ldFlags));
  }

  public boolean isStrip() {
    return this.strip;
  }

  public void setStrip(final boolean flag) {
    this.strip = flag;
  }

  @Nonnull
  public String getBuildMode() {
    return this.buildMode;
  }

  public void setBuilMode(@Nullable final String buildMode) {
    this.buildMode = GetUtils.ensureNonNull(buildMode, "default");
  }

  @Nonnull
  private File getResultFile() {
    return new File(getResultFolder(), this.resultName);
  }

  @Nonnull
  public String getResultFolder() {
    return assertNotNull(this.resultFolder);
  }

  public void setResultFolder(@Nonnull final String folder) {
    this.resultFolder = assertNotNull(folder);
  }

  @Nonnull
  public String getResultName() {
    return assertNotNull(this.resultName);
  }

  public void setResultName(@Nonnull final String resultName) {
    this.resultName = assertNotNull(resultName);
  }

  @Override
  @Nonnull
  public String getGoCommand() {
    return "build";
  }

  @Override
  public void beforeExecution(@Nullable final ProxySettings proxySettings) throws MojoFailureException {
    final File folder = new File(getResultFolder());

    if (!folder.isDirectory() && !folder.mkdirs()) {
      throw new MojoFailureException("Can't create folder : " + folder);
    }

    if (isVerbose() || !"default".equals(this.buildMode)) {
      getLog().info("Build mode : " + this.buildMode);
    }

    final String[] currentPackages = this.getPackages();

    if (currentPackages != null && currentPackages.length > 1) {
      this.getLog().warn("Target file is ignored because allowed only when compiling a single package,");
    }
  }

  @Override
  public void afterExecution(@Nullable final ProxySettings proxySettings, final boolean error) throws MojoFailureException {
    if (!error) {
      final File resultFile = getResultFile();
      // check that it exists
      if (!resultFile.isFile()) {
        throw new MojoFailureException("Can't find generated target file : " + resultFile);
      }
      // softly try to make it executable
      try {
        if (!resultFile.setExecutable(true)) {
          getLog().warn("Can't make result file executable : " + resultFile);
        }
      } catch (SecurityException ex) {
        getLog().warn("Security exception during executable flag set : " + resultFile);
      }

      getLog().info("The Result file has been successfuly created : " + resultFile);
    }
  }

  @Override
  public boolean isCommandSupportVerbose() {
    return true;
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCommandFlags() {
    final List<String> flags = new ArrayList<>();

    flags.add("-buildmode=" + this.buildMode);

    final List<String> linkerflags = this.getLdflagsAsList();

    if (this.strip) {
      if (!linkerflags.contains("-s")) {
        linkerflags.add("-s");
      }
      if (!linkerflags.contains("-w")) {
        linkerflags.add("-w");
      }
    }

    if (!linkerflags.isEmpty()) {
      flags.add("-ldflags");
      final StringBuilder buffer = new StringBuilder();
      for (final String s : linkerflags) {
        if (buffer.length() > 0) {
          buffer.append(' ');
        }
        buffer.append(s);
      }
      flags.add(buffer.toString());
    }

    if (requireNonNull(getPackages()).length < 2) {
      flags.add("-o");
      flags.add(getResultFile().getAbsolutePath());
    }

    return flags.toArray(new String[flags.size()]);
  }

}
