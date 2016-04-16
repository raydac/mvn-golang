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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import com.igormaznitsa.meta.annotation.MustNotContainNull;

/**
 * The Mojo wraps the 'get' command.
 */
@Mojo(name = "get", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangGetMojo extends AbstractPackageGolangMojo {

  private static final Pattern PATTERN_NO_SUBMODULE_MAPPING_FOUND_IN_GIT = Pattern.compile("no\\s+submodule\\s+mapping\\s+found\\s+in\\s+.gitmodules for path\\s+\\'([\\S]+?)\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  private static final Pattern PATTERN_EXTRACT_PACKAGE_AND_STATUS = Pattern.compile("^package ([\\S]+?)\\s*:\\s*exit\\s+status\\s+([\\d]+?)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  /**
   * Flag to make attempt to fix detected Git cache error and re-execute the command. It will try to execute
   * <pre>'git rm -r --cached .'</pre> just in the package directory to clear cache.
   */
  @Parameter(name = "autofixGitCache", defaultValue = "false")
  private boolean autofixGitCache;

  public boolean isAutoFixGitCache() {
    return this.autofixGitCache;
  }

  @Override
  @Nonnull
  public String getGoCommand() {
    return "get";
  }

  @Override
  public boolean enforcePrintOutput() {
    return true;
  }

  @Override
  public boolean isMojoMustNotBeExecuted() throws MojoFailureException {
    final String [] packages = getTailArguments();
    final boolean result;
    if (packages.length == 0) {
      getLog().info("There are not packages to get");
      result = true;
    } else {
      result = super.isMojoMustNotBeExecuted();
    }
    return result;
  }

  @Nonnull
  @MustNotContainNull
  private List<String> extractProblemPackagesFromErrorLog(@Nonnull final String errorLog) {
    final List<String> result = new ArrayList<String>();
    final Matcher extractor = PATTERN_EXTRACT_PACKAGE_AND_STATUS.matcher(errorLog);
    while (extractor.find()) {
      final String packageName = extractor.group(1);
      final String status = extractor.group(2);
      if (!"0".equals(status)) {
        result.add(packageName);
      }
    }
    return result;
  }

  private boolean tryToFixGitCacheErrorsForPackages(@Nonnull @MustNotContainNull final List<String> packages) throws IOException {
    final File goPath = findGoPath(true);

    int fixed = 0;

    for (final String s : packages) {
      final File packageFolder = new File(goPath, "src" + File.separatorChar + s);
      if (packageFolder.isDirectory()) {
        getLog().warn(String.format("Executing 'git rm -r --cached .' in %s", packageFolder.getAbsolutePath()));

        final ProcessExecutor executor = new ProcessExecutor(adaptExecNameForOS("git"), "rm", "-r", "--cached", ".");
        executor.directory(packageFolder);

        try {
          final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
          executor.redirectError(errorStream);

          final ProcessResult result = executor.executeNoTimeout();
          if (result.getExitValue() != 0) {
            getLog().error(new String(errorStream.toByteArray(), Charset.defaultCharset()));
            return false;
          }
        } catch (InterruptedException ex) {
          break;
        }

      } else {
        getLog().error("Can't find folder " + packageFolder);
        return false;
      }

      fixed++;
    }
    return fixed != 0;
  }

  @Override
  protected boolean doesNeedOneMoreAttempt(@Nonnull final ProcessResult processResult, @Nonnull final String consoleOut, @Nonnull final String consoleErr) throws IOException, MojoExecutionException {
    boolean result = false;
    if (processResult.getExitValue() != 0) {
      final Matcher matcher = PATTERN_NO_SUBMODULE_MAPPING_FOUND_IN_GIT.matcher(consoleErr);

      if (matcher.find()) {
        final List<String> packagesWithDetectedGitCacheErrors = extractProblemPackagesFromErrorLog(consoleErr);
        if (!packagesWithDetectedGitCacheErrors.isEmpty()) {
          if (this.autofixGitCache) {

            getLog().warn("Trying to fix the detected git cache errors automatically..");

            result = tryToFixGitCacheErrorsForPackages(packagesWithDetectedGitCacheErrors);
          } else {

            for (final String s : packagesWithDetectedGitCacheErrors) {
              getLog().error(String.format("Detected Git cache error for package '%s', can be fixed with 'git rm -r --cached .'", s));
            }

          }
        }
      }
    }
    return result;
  }

}
