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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.zeroturnaround.exec.ProcessResult;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.mvngolang.cvs.CVSType;
import com.igormaznitsa.mvngolang.utils.ProxySettings;

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

  /**
   * Branch to be activated.
   *
   * @since 2.1.0
   */
  @Parameter(name = "branch")
  private String branch;

  /**
   * Tag to be activated.
   *
   * @since 2.1.0
   */
  @Parameter(name = "tag")
  private String tag;

  /**
   * Revision to be activated.
   *
   * @since 2.1.1
   */
  @Parameter(name = "revision")
  private String revision;

  /**
   * Custom executable file to be executed for branch, tag and revision operations.
   *
   * @since 2.1.1
   */
  @Parameter(name = "cvsExe")
  private String cvsExe;

  /**
   * Search sources for package and its compiled version, enforce delete of found source folder and compiled '.a' file.
   * 
   * @since 2.1.4
   */
  @Parameter(name = "enforceDeletePackageFiles")
  private boolean enforceDeletePackageFiles;
  
  public boolean isAutoFixGitCache() {
    return this.autofixGitCache;
  }

  public boolean isEnforceDeletePackageFiles() {
    return this.enforceDeletePackageFiles;
  }
  
  @Nullable
  public String getRevision() {
    return this.revision;
  }

  @Nullable
  public String getBranch() {
    return this.branch;
  }

  @Nullable
  public String getTag() {
    return this.tag;
  }

  @Nullable
  public String getCvsExe() {
    return this.cvsExe;
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
    final String[] packages = getTailArguments();
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

  @Nonnull
  private static String processSlashes(@Nonnull final String str) {
    final StringBuilder result = new StringBuilder();
    boolean check = true;
    for (final char c : str.toCharArray()) {
      if (check) {
        if (c == '\\' || c == '/') {
          continue;
        } else {
          check = false;
        }
      }
      if (c == '\\' || c == '/') {
        result.append(File.separatorChar);
      } else {
        result.append(c);
      }
    }

    while (result.length() > 0) {
      final char last = result.charAt(result.length() - 1);
      if (last == '/' || last == '\\') {
        result.deleteCharAt(result.length() - 1);
      } else {
        break;
      }
    }

    return result.toString();
  }

  @Nonnull
  private File makePathToPackageSources(@Nonnull final File goPath, @Nonnull final String pack) {
    String path = pack.trim();

    try {
      final URI uri = URI.create(path);
      path = processSlashes(uri.getPath());
    } catch (IllegalArgumentException ex) {
      // it is not url
      path = processSlashes(path);
    }

    return new File(goPath, "src" + File.separatorChar + path);
  }

  @Nonnull
  private File makePathToPackageCompiled(@Nonnull final File goPath, @Nonnull final String pack) {
    String path = pack.trim();

    try {
      final URI uri = URI.create(path);
      path = processSlashes(uri.getPath());
    } catch (IllegalArgumentException ex) {
      // it is not url
      path = processSlashes(path);
    }

    return new File(goPath, "pkg" + File.separatorChar + this.getOs()+'_'+this.getArch()+ File.separatorChar + path);
  }

  private boolean tryToFixGitCacheErrorsForPackages(@Nonnull @MustNotContainNull final List<String> packages) throws IOException {
    final File goPath = findGoPath(true);

    int fixed = 0;

    for (final String s : packages) {
      final File packageFolder = makePathToPackageSources(goPath, s);
      final CVSType repo = CVSType.investigateFolder(packageFolder);
      if (repo == CVSType.GIT) {
        getLog().warn(String.format("Executing 'git rm -r --cached .' in %s", packageFolder.getAbsolutePath()));
        final int result = repo.getProcessor().execute(getCvsExe(), getLog(), packageFolder, "rm", "-r", "--cached", ".");
        if (result != 0) {
          return false;
        }
        fixed++;
      } else {
        getLog().warn(String.format("Folder %s is not GIT repository", packageFolder.getAbsolutePath()));
        return false;
      }

      fixed++;
    }
    return fixed != 0;
  }

  private boolean processCVS(@Nullable final ProxySettings proxySettings, @Nonnull final File goPath) {
    final String[] packages = this.getPackages();
    if (packages != null && packages.length > 0) {
      for (final String p : packages) {
        final File packageFolder = makePathToPackageSources(goPath, p);
        if (!packageFolder.isDirectory()) {
          getLog().error(String.format("Can't find package '%s' at '%s'", p, packageFolder.getAbsolutePath()));
          return false;
        } else {
          final CVSType repo = CVSType.investigateFolder(packageFolder);

          if (this.branch != null || this.tag != null || this.revision != null) {

            if (!repo.getProcessor().prepareFolder(getLog(), proxySettings, getCvsExe(), packageFolder)){
              getLog().debug("Can't prepare folder : "+packageFolder);
              return false;
            }
            
            if (this.branch != null || this.tag != null || this.revision != null) {
              getLog().info(String.format("Switch '%s' to branch = '%s', tag = '%s', revision = '%s'", p, GetUtils.ensureNonNull(this.branch,"_"),GetUtils.ensureNonNull(this.tag,"_"),GetUtils.ensureNonNull(this.revision,"_")));
              if (!repo.getProcessor().processCVSRequisites(getLog(), proxySettings, getCvsExe(), packageFolder, this.branch, this.tag, this.revision)) {
                return false;
              }
            }
          }
        }
      }
    }
    return true;
  }

  @Override
  public void beforeExecution(@Nonnull final ProxySettings proxySettings) throws MojoFailureException, MojoExecutionException {
    if (isEnforceDeletePackageFiles()) {
      getLog().debug("Detected request to delete both package source and binary folders if they are presented");
      
      final String[] packages = this.getPackages();
        final File goPath;
        try {
          goPath = findGoPath(true);
        } catch (IOException ex) {
          throw new MojoFailureException("Can't find $GOPATH", ex);
        }

      for (final String p : packages) {
        getLog().info("Removing binary and source folders for package '"+p+'\'');

        final File pkgSources = this.makePathToPackageSources(goPath, p);
        final File pkgBinary = this.makePathToPackageCompiled(goPath, p);

        getLog().debug("Src folder : "+pkgSources);
        getLog().debug("Pkg folder : "+pkgBinary);
        
        if (pkgSources.isDirectory()){
          try{
            FileUtils.deleteDirectory(pkgSources);
          }catch(IOException ex){
            throw new MojoExecutionException("Can't delete source folder : "+pkgSources, ex);
          }
          getLog().info("\tDeleted source folder : "+pkgSources);
        }

        if (pkgBinary.isDirectory()){
          try{
            FileUtils.deleteDirectory(pkgBinary);
          }catch(IOException ex){
            throw new MojoExecutionException("Can't delete binary folder : "+pkgBinary, ex);
          }
          getLog().info("\tDeleted binary folder : "+pkgBinary);
        } else {
          final File compiled = new File(pkgBinary.getAbsolutePath() + ".a");
          if (compiled.isFile()) {
            if (!compiled.delete()) {
              throw new MojoExecutionException("Can't delete compiled file : " + compiled);
            }
            getLog().info("\tDeleted compiled file : " + compiled);
          }
        }
      }
    }
    
    if (this.branch != null || this.tag != null || this.revision != null) {
      final File goPath;
      try {
        goPath = findGoPath(true);
      }
      catch (IOException ex) {
        throw new MojoFailureException("Can't find $GOPATH", ex);
      }

      getLog().info("(!) Get initial version of package repository before CVS operations");
      this.buildFlagsToIgnore.add("-u");
      try {
        final boolean error = this.doMainBusiness(proxySettings, 10);
        if (error) {
          throw new Exception("error as result of 'get' operation during initial loading of packages " + Arrays.toString(this.getPackages()));
        }
      }
      catch (Exception ex) {
        throw new MojoExecutionException("Can't get packages", ex);
      }
      finally {
        this.buildFlagsToIgnore.clear();
      }

      getLog().debug(String.format("Switching branch and tag for packages : branch = %s , tag = %s", GetUtils.ensureNonNull(this.branch, "..."), GetUtils.ensureNonNull(this.tag, "...")));

      if (!processCVS(proxySettings, goPath)) {
        throw new MojoFailureException("Can't change branch or tag, see the log for errors!");
      }
    }
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
