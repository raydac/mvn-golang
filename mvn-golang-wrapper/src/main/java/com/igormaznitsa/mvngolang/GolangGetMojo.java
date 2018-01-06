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
import com.igormaznitsa.mvngolang.cvs.CVSType;
import com.igormaznitsa.mvngolang.utils.ProxySettings;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.zeroturnaround.exec.ProcessResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Mojo wraps the 'get' command.
 */
@Mojo(name = "get", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangGetMojo extends AbstractPackageGolangMojo {

  private static final Pattern PATTERN_NO_SUBMODULE_MAPPING_FOUND_IN_GIT = Pattern.compile("no\\s+submodule\\s+mapping\\s+found\\s+in\\s+.gitmodules for path\\s+\\'([\\S]+?)\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  private static final Pattern PATTERN_EXTRACT_PACKAGE_AND_STATUS = Pattern.compile("^package ([\\S]+?)\\s*:\\s*exit\\s+status\\s+([\\d]+?)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  /**
   * Flag to make attempt to fix detected Git cache error and re-execute the
   * command. It will try to execute
   * <pre>'git rm -r --cached .'</pre> just in the package directory to clear
   * cache.
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
   * Allows to define custom options for CVS operation.
   *
   * @since 2.1.5
   */
  @Parameter(name = "customCvsOptions")
  private String[] customCvsOptions;

  /**
   * Custom executable file to be executed for branch, tag and revision
   * operations.
   *
   * @since 2.1.1
   */
  @Parameter(name = "cvsExe")
  private String cvsExe;

  /**
   * Search sources for package and its compiled version, enforce delete of
   * found source folder and compiled '.a' file.
   *
   * @since 2.1.4
   */
  @Parameter(name = "enforceDeletePackageFiles", defaultValue = "false")
  private boolean enforceDeletePackageFiles;

  /**
   * Delete whole common pkg folder at $GOPATH/pkg.
   *
   * @since 2.1.6
   */
  @Parameter(name = "deleteCommonPkg", defaultValue = "false")
  private boolean deleteCommonPkg;

  /**
   * Allows directly define relative path to the package containing CVS data
   * inside 'src' folder for package, by default the folder is the same as
   * package.
   *
   * @since 2.1.5
   */
  @Parameter(name = "relativePathToCvsFolder")
  private String relativePathToCvsFolder;

  /**
   * Disable auto-search for CVS folder in package folder hierarchy, it works
   * only if CVS folder is not defined directly.
   *
   * @since 2.1.5
   */
  @Parameter(name = "disableCvsAutosearch", defaultValue = "false")
  private boolean disableCvsAutosearch;

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

  private static synchronized boolean processCVS(@Nonnull final GolangGetMojo instance, @Nullable final ProxySettings proxySettings, @Nonnull @MustNotContainNull final File[] goPath) {
    final String[] packages = instance.getPackages();

    if (packages != null && packages.length > 0) {
      for (final File f : goPath) {
        for (final String p : packages) {
          File rootCvsFolder = instance.makePathToPackageSources(f, p);

          if (instance.getRelativePathToCvsFolder() == null) {
            rootCvsFolder = instance.isDisableCvsAutosearch() ? rootCvsFolder : instance.findRootCvsFolderForPackageSources(f, rootCvsFolder);
          }

          if (rootCvsFolder == null) {
            instance.getLog().error("Can't find CVS folder in hierarchy for package '" + p + "' [" + rootCvsFolder + ']');
            return false;
          }

          instance.getLog().info("CVS folder for processing is : " + rootCvsFolder);

          if (!rootCvsFolder.isDirectory()) {
            instance.getLog().error(String.format("Can't find CVS folder for package '%s' at '%s'", p, rootCvsFolder.getAbsolutePath()));
            return false;
          } else {
            final CVSType repo = CVSType.investigateFolder(rootCvsFolder);

            if (repo == CVSType.UNKNOWN) {
              instance.getLog().error("Can't recognize CVS in the folder : " + rootCvsFolder + " (for package '" + p + "')");
              instance.getLog().error("May be to define folder directly through <relativePathToCvsFolder>...</relativePathToCvsFolder>!");
              return false;
            }

            final boolean hasCvsRequisite = instance.branch != null || instance.tag != null || instance.revision != null;
            final String[] customcvs = instance.customCvsOptions;

            if (customcvs != null || hasCvsRequisite) {

              if (!repo.getProcessor().prepareFolder(instance.getLog(), proxySettings, instance.getCvsExe(), rootCvsFolder)) {
                instance.getLog().debug("Can't prepare folder : " + rootCvsFolder);
                return false;
              }

              if (customcvs != null && hasCvsRequisite) {
                instance.getLog().warn("CVS branch, tag or revision are ignored for provided custom CVS options!");
              }

              if (customcvs != null) {
                instance.getLog().info("Custom CVS options : " + Arrays.toString(customcvs));
                if (!repo.getProcessor().processCVSForCustomOptions(instance.getLog(), proxySettings, rootCvsFolder, instance.getCvsExe(), customcvs)) {
                  return false;
                }
              } else if (instance.branch != null || instance.tag != null || instance.revision != null) {
                instance.getLog().info(String.format("Switch '%s' to branch = '%s', tag = '%s', revision = '%s'", p, GetUtils.ensureNonNull(instance.branch, "_"), GetUtils.ensureNonNull(instance.tag, "_"), GetUtils.ensureNonNull(instance.revision, "_")));
                if (!repo.getProcessor().processCVSRequisites(instance.getLog(), proxySettings, instance.getCvsExe(), rootCvsFolder, instance.branch, instance.tag, instance.revision)) {
                  return false;
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  public boolean isDisableCvsAutosearch() {
    return this.disableCvsAutosearch;
  }

  public boolean getDeleteCommonPkg() {
    return this.deleteCommonPkg;
  }

  @Nullable
  @MustNotContainNull
  public String[] getCustomCvsOptions() {
    return this.customCvsOptions;
  }

  @Nullable
  public String getRelativePathToCvsFolder() {
    return this.relativePathToCvsFolder;
  }

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
      getLog().info("There are no packages to get");
      result = true;
    } else {
      result = super.isMojoMustNotBeExecuted();
    }
    return result;
  }

  @Nonnull
  @MustNotContainNull
  private List<String> extractProblemPackagesFromErrorLog(@Nonnull final String errorLog) {
    final List<String> result = new ArrayList<>();
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

  @Nullable
  private File findRootCvsFolderForPackageSources(@Nonnull final File rootFolder, @Nullable final File packageSourceFolder) {
    File foundFile = null;
    if (packageSourceFolder != null) {
      final File srcFolder = getSrcFolder(rootFolder);

      File current = packageSourceFolder;

      while (!srcFolder.equals(current)) {
        if (CVSType.investigateFolder(current) != CVSType.UNKNOWN) {
          foundFile = current;
          break;
        }
        current = current.getParentFile();
      }
    }
    return foundFile;
  }

  @Nonnull
  private File getSrcFolder(@Nonnull final File goPath) {
    return new File(goPath, "src");
  }

  @Nonnull
  private File makePathToPackageSources(@Nonnull final File goPath, @Nonnull final String pack) {
    String path = pack.trim();
    final String predefinedCvsPath = getRelativePathToCvsFolder();

    if (predefinedCvsPath != null) {
      path = processSlashes(predefinedCvsPath);
    } else {
      try {
        final URI uri = URI.create(path);
        path = processSlashes(uri.getPath());
      } catch (IllegalArgumentException ex) {
        // it is not url
        path = processSlashes(path);
      }
    }

    return new File(getSrcFolder(goPath), path);
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

    return new File(goPath, "pkg" + File.separatorChar + this.getOs() + '_' + this.getArch() + File.separatorChar + path);
  }

  private boolean tryToFixGitCacheErrorsForPackages(@Nonnull @MustNotContainNull final List<String> packages) throws IOException {
    final File[] goPath = findGoPath(true);

    int fixed = 0;

    for (final File f : goPath) {
      for (final String s : packages) {
        final File packageFolder = makePathToPackageSources(f, s);
        if (packageFolder.isDirectory()) {
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
        } else {
          getLog().debug("Folder " + packageFolder + " is not found");
        }
      }
    }
    return fixed != 0;
  }

  @Override
  public void beforeExecution(@Nullable final ProxySettings proxySettings) throws MojoFailureException, MojoExecutionException {
    if (getDeleteCommonPkg()) {
      getLog().warn("Request to delete whole common pkg folder");

      final File[] goPath;
      try {
        goPath = findGoPath(true);
      } catch (IOException ex) {
        throw new MojoExecutionException("Can't get $GOPATH", ex);
      }

      for (final File f : goPath) {
        final File pkgBinary = new File(f, "pkg");

        if (pkgBinary.isDirectory()) {
          try {
            FileUtils.deleteDirectory(pkgBinary);
            getLog().warn("Folder " + pkgBinary + " has been deleted");
          } catch (IOException ex) {
            throw new MojoExecutionException("Can't delete PKG folder : " + pkgBinary, ex);
          }
        } else {
          getLog().info("PKG folder is not found : " + pkgBinary);
        }
      }
    }

    if (isEnforceDeletePackageFiles()) {
      int deletedInstances = 0;
      getLog().debug("Detected request to delete both package source and binary folders if they are presented");

      final String[] packages = this.getPackages();
      final File[] goPath;
      try {
        goPath = findGoPath(true);
      } catch (IOException ex) {
        throw new MojoExecutionException("Can't find $GOPATH", ex);
      }

      for (final File f : goPath) {
        for (final String p : packages) {
          getLog().info("Removing binary and source folders for package '" + p + "\' in " + f);

          final File pkgSources = this.makePathToPackageSources(f, p);
          final File pkgBinary = this.makePathToPackageCompiled(f, p);

          getLog().debug("Src folder : " + pkgSources);
          getLog().debug("Pkg folder : " + pkgBinary);

          if (pkgSources.isDirectory()) {
            try {
              FileUtils.deleteDirectory(pkgSources);
              deletedInstances++;
            } catch (IOException ex) {
              throw new MojoExecutionException("Can't delete source folder : " + pkgSources, ex);
            }
            getLog().info("\tDeleted source folder : " + pkgSources);
          } else {
            getLog().debug("Folder " + pkgSources + " is not found");
          }

          if (pkgBinary.isDirectory()) {
            try {
              FileUtils.deleteDirectory(pkgBinary);
              deletedInstances++;
            } catch (IOException ex) {
              throw new MojoExecutionException("Can't delete binary folder : " + pkgBinary, ex);
            }
            getLog().info("\tDeleted binary folder : " + pkgBinary);
          } else {
            final File compiled = new File(pkgBinary.getAbsolutePath() + ".a");
            if (compiled.isFile()) {
              if (!compiled.delete()) {
                throw new MojoExecutionException("Can't delete compiled file : " + compiled);
              }
              deletedInstances++;
              getLog().info("\tDeleted compiled file : " + compiled);
            } else {
              getLog().debug("File " + compiled + " is not found");
            }
          }
        }
      }

      if (deletedInstances > 0) {
        try {
          getLog().info("1.5 second delay to be visible by systems analyzing file time stamp");
          Thread.sleep(1500L);
        } catch (InterruptedException ex) {
          throw new MojoExecutionException("Interrupted");
        }
      }
    }

    final String[] customcvs = this.getCustomCvsOptions();
    if (customcvs != null || this.branch != null || this.tag != null || this.revision != null) {
      final File[] goPath;
      try {
        goPath = findGoPath(true);
      } catch (IOException ex) {
        throw new MojoFailureException("Can't find $GOPATH", ex);
      }

      getLog().info("(!) Get initial version of package repository before CVS operations");
      this.buildFlagsToIgnore.add("-u");
      this.addTmpBuildFlagIfNotPresented("-d");

      try {
        final boolean error = this.doMainBusiness(proxySettings, 10);
        if (error) {
          throw new Exception("error as result of 'get' operation during initial loading of packages " + Arrays.toString(this.getPackages()));
        }
      } catch (Exception ex) {
        throw new MojoExecutionException("Can't get packages", ex);
      } finally {
        this.buildFlagsToIgnore.clear();
        this.tempBuildFlags.clear();
      }

      getLog().debug(String.format("Switching branch and tag for packages : branch = %s , tag = %s", GetUtils.ensureNonNull(this.branch, "..."), GetUtils.ensureNonNull(this.tag, "...")));
      getLog().debug("Custom CVS options : " + Arrays.toString(customCvsOptions));

      if (!processCVS(this, proxySettings, goPath)) {
        throw new MojoFailureException("Can't change branch or tag or execute custom CVS options, see the log for errors!");
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
