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
import static com.igormaznitsa.meta.common.utils.Assertions.assertNotNull;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.mvngolang.utils.ProxySettings;

/**
 * The Mojo wraps the 'build' command.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GolangBuildMojo extends AbstractPackageGolangMojo {

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
   * @since 2.1.3
   */
  @Parameter(name = "buildMode", defaultValue = "default")
  private String buildMode;

  /**
   * Strip result file. Symbol table and DWARF will be removed from the result file.
   * @since 2.1.3
   */
  @Parameter(name="strip", defaultValue = "false")
  private boolean strip;
  
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

  public void setBuildode(@Nullable final String buildMode) {
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

  @Nonnull
  public String getResultName() {
    return assertNotNull(this.resultName);
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
      getLog().info("Build mode : "+this.buildMode);
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
        resultFile.setExecutable(true);
      }
      catch (SecurityException ex) {
        getLog().warn("Security exception during setting executable flag : " + resultFile);
      }

      getLog().info("The Result file has been successfuly created : " + resultFile);
    }
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getCommandFlags() {
    final List<String> flags = new ArrayList<String>();
 
    flags.add("-buildmode="+this.buildMode);
 
    if (this.strip) {
      flags.add("-ldflags");
      flags.add("-s -w");
    }
    
    flags.add("-o");
    flags.add(getResultFile().getAbsolutePath());
    
    return flags.toArray(new String[flags.size()]);
  }

}
