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
import com.igormaznitsa.mvngolang.utils.ProxySettings;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.zeroturnaround.exec.ProcessExecutor;

/**
 * Minimalistic mojo provides way to call external JFrog CLI executable tool with environment
 * provided by the Golang wrapper and wrapped GoSDK.
 * JFrog CLI executable file can be downloaded
 * from <a href="https://jfrog.com/getcli/">the web page</a>
 * Documentation of the JFrog CLI can be found
 * <a href="https://www.jfrog.com/confluence/display/CLI/JFrog+CLI">here</a>.
 *
 * @since 2.3.0
 */
@Mojo(name = "jfrog-cli", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class GolangJfrogCliMojo extends AbstractGolangMojo {

  /**
   * Path to a JFrog CLI executable file. Must be defined.
   */
  @Parameter(name = "cliPath", required = true)
  private String cliPath;

  /**
   * The product on which you wish to execute the command:
   * <ul>
   * <li><b>rt</b>- JFrog Artifactory</li>
   * <li><b>bt</b>- JFrog Bintray </li>
   * <li><b>mc</b>- JFrog Mission Control</li>
   * <li><b>xr</b>- JFrog Xray</li>
   * </ul>
   */
  @Parameter(name = "target", defaultValue = "rt")
  private String target = "rt";

  /**
   * The command to execute. Note that you can use either the full command name
   * or its abbreviation.
   */
  @Parameter(name = "command", defaultValue = "go")
  private String command = "go";

  /**
   * A set of arguments corresponding to the command.
   */
  @Parameter(name = "arguments")
  private List<String> arguments = new ArrayList<>();

  @Nonnull
  @MustNotContainNull
  public List<String> getArguments() {
    return this.arguments;
  }

  @Nonnull
  public String getCliPath() {
    return this.cliPath;
  }

  @Nonnull
  public String getTarget() {
    return this.target;
  }

  @Nonnull
  public String getCommand() {
    return this.command;
  }

  @Nonnull
  @Override
  protected ProcessExecutor prepareExecutor(@Nullable final ProxySettings proxySettings) throws IOException, MojoFailureException, MojoExecutionException {
    this.initConsoleBuffers();
    
    final File goRoot = this.findGoRoot(proxySettings);
    final String gobin = this.getGoBin();
    final File[] gopathParts = findGoPath(true);

    final List<String> cliList = new ArrayList<>();

    cliList.add(this.getCliPath());
    cliList.add(this.getTarget());
    cliList.add(this.getCommand());

    this.getLog().info("JFrog CLI: "+ this.getCliPath());
    this.getLog().info("   Target: "+ this.getTarget());
    this.getLog().info("  Command: "+ this.getCommand());
    
    for (final String option : this.getArguments()) {
      cliList.add(option);
    }

    this.getLog().debug("Prepared CLI: " + cliList);

    final ProcessExecutor result = new ProcessExecutor(cliList);

    final File sourcesFile = getSources(isSourceFolderRequired());
    logOptionally("GoLang project sources folder : " + sourcesFile);

    if (sourcesFile.isDirectory()) {
      result.directory(sourcesFile);
    }

    this.registerEnvVars(result, goRoot, gobin, sourcesFile, gopathParts);
    this.registerOutputBuffers(result);
    
    return result;
  }

}
