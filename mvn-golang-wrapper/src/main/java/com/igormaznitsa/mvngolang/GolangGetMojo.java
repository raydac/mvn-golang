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
import com.igormaznitsa.meta.common.utils.Assertions;
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.mvngolang.cvs.CVSType;
import com.igormaznitsa.mvngolang.utils.PackageList;
import com.igormaznitsa.mvngolang.utils.ProxySettings;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

/**
 * The Mojo wraps the 'get' command.
 */
@Mojo(name = "get", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true, requiresDependencyResolution = ResolutionScope.NONE)
public class GolangGetMojo extends AbstractGoPackageAndDependencyAwareMojo {

  private static final Pattern PATTERN_NO_SUBMODULE_MAPPING_FOUND_IN_GIT = Pattern.compile("no\\s+submodule\\s+mapping\\s+found\\s+in\\s+.gitmodules for path\\s+\\'([\\S]+?)\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  private static final Pattern PATTERN_EXTRACT_PACKAGE_AND_STATUS = Pattern.compile("^package ([\\S]+?)\\s*:\\s*exit\\s+status\\s+([\\d]+?)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  /**
   * Script to be called in the end of all operations over CVS, <b>it will be
   * executed separately for each package</b>. The Script will be started with
   * the package CVS root folder. During start there will be added number of
   * environment variables: MVNGO_CVS_BRANCH, MVNGO_CVS_TAG, MVNGO_CVS_REVISION,
   * MVNGO_CVS_PACKAGE
   *
   * @since 2.1.8
   */
  @Parameter(name = "customScript")
  private CustomScript customScript;

  /**
   * Path to a file contains list of packages. Each package takes a line and
   * described in format 'package: NAME [,branch: BRANCH][,tag: TAG][,revsion:
   * REVISION]'
   *
   * There is special field value 'none' which allows to efine value but to not
   * make any work, sometime it is useful for multi-profile projects.
   *
   * NB! Its value can be provided through property
   * 'mvn.golang.get.packages.file', but keep in mind that the value will be the
   * same for all get goals.
   *
   * @since 2.1.9
   */
  @Parameter(name = "externalPackageFile", property = "mvn.golang.get.packages.file")
  private String externalPackageFile;

  /**
   * Flag to try to fix GIT cache error if it is detected.
   *
   * @since 1.0
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
   * Custom executable CVS file to be executed for branch, tag and revision
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

  private List<PackageList.Package> integralPackageList;

  @Nullable
  public String getExternalPackageFile() {
    return this.externalPackageFile;
  }

  @Override
  @Nonnull
  @MustNotContainNull
  public String[] getPackages() {
    final String[] result = new String[this.integralPackageList.size()];
    for (int i = 0; i < this.integralPackageList.size(); i++) {
      result[i] = this.integralPackageList.get(i).getPackage();
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

  @Nullable
  public CustomScript getCustomScript() {
    return this.customScript;
  }

  private synchronized boolean processCVS(@Nonnull @MustNotContainNull final List<PackageList.Package> packages, @Nullable final ProxySettings proxySettings, @Nonnull @MustNotContainNull final File[] goPath) {
    if (!packages.isEmpty()) {
      for (final File f : goPath) {
        for (final PackageList.Package p : packages) {
          File rootCvsFolder = this.makePathToPackageSources(f, p.getPackage());

          if (this.getRelativePathToCvsFolder() == null) {
            rootCvsFolder = this.isDisableCvsAutosearch() ? rootCvsFolder : this.findRootCvsFolderForPackageSources(f, rootCvsFolder);
          }

          if (rootCvsFolder == null) {
            getLog().error("Can't find CVS folder, may be it was not initially loaded from repository: " + p);
            return false;
          }
          
          if (this.getLog().isDebugEnabled()) {
            this.getLog().debug(String.format("CVS folder path for %s is %s", p, rootCvsFolder));
          }

          if (!rootCvsFolder.isDirectory()) {
            this.getLog().error(String.format("Can't find CVS folder for package '%s' at '%s'", p, rootCvsFolder.getAbsolutePath()));
            return false;
          } else {
            final CVSType repo = CVSType.investigateFolder(rootCvsFolder);

            if (repo == CVSType.UNKNOWN) {
              this.getLog().error("Can't recognize CVS in the folder : " + rootCvsFolder + " (for package '" + p + "')");
              this.getLog().error("May be to define folder directly through <relativePathToCvsFolder>...</relativePathToCvsFolder>!");
              return false;
            }

            final String[] customcvs = this.getCustomCvsOptions();

            if (customcvs != null || p.doesNeedCvsProcessing()) {

              if (!repo.getProcessor().prepareFolder(this.getLog(), proxySettings, this.getCvsExe(), rootCvsFolder)) {
                this.getLog().debug("Can't prepare folder : " + rootCvsFolder);
                return false;
              }

              if (customcvs != null && p.doesNeedCvsProcessing()) {
                this.getLog().warn("CVS branch, tag or revision are ignored for provided custom CVS options!");
              }

              if (customcvs != null) {
                this.getLog().info("Custom CVS options : " + Arrays.toString(customcvs));
                if (!repo.getProcessor().processCVSForCustomOptions(this.getLog(), proxySettings, rootCvsFolder, this.getCvsExe(), customcvs)) {
                  return false;
                }
              } else if (p.doesNeedCvsProcessing()) {
                this.getLog().info(String.format("Switch '%s' to branch = '%s', tag = '%s', revision = '%s'", p, GetUtils.ensureNonNull(p.getBranch(), "_"), GetUtils.ensureNonNull(p.getTag(), "_"), GetUtils.ensureNonNull(p.getRevision(), "_")));
                if (!repo.getProcessor().processCVSRequisites(this.getLog(), proxySettings, this.getCvsExe(), rootCvsFolder, p.getBranch(), p.getTag(), p.getRevision())) {
                  return false;
                }
              }
            }
          }

          if (this.getCustomScript() != null) {
            if (!processCustomScriptCallForPackage(p.getPackage(), rootCvsFolder, Assertions.assertNotNull(this.getCustomScript()))) {
              return false;
            }
          }

        }
      }
    }
    return true;
  }

  private boolean processCustomScriptCallForPackage(@Nonnull final String packageName, @Nonnull final File rootCvsFolder, @Nonnull final CustomScript script) {
    final List<String> command = new ArrayList<>();

    command.add(script.path);
    if (script.options != null) {
      for (final String s : script.options) {
        command.add(s);
      }
    }

    if (getLog().isDebugEnabled()) {
      getLog().debug("CLI : " + command);
      getLog().debug("Package name : " + packageName);
      getLog().debug("Root CVS folder : " + rootCvsFolder);
    }

    getLog().warn(String.format("Starting script in VCS folder [%s] : %s", packageName, StringUtils.join(command.toArray(), ' ')));

    final ProcessExecutor processExecutor = new ProcessExecutor(command.toArray(new String[command.size()]));
    processExecutor
            .exitValueAny()
            .directory(rootCvsFolder)
            .environment("MVNGO_CVS_BRANCH", GetUtils.ensureNonNull(this.branch, ""))
            .environment("MVNGO_CVS_TAG", GetUtils.ensureNonNull(this.tag, ""))
            .environment("MVNGO_CVS_REVISION", GetUtils.ensureNonNull(this.revision, ""))
            .environment("MVNGO_CVS_PACKAGE", packageName)
            .redirectError(System.err)
            .redirectOutput(System.out);

    boolean result = false;

    try {
      final ProcessResult process = processExecutor.executeNoTimeout();
      final int exitValue = process.getExitValue();

      result = script.ignoreFail ? true : exitValue == 0;
    } catch (IOException | InterruptedException | InvalidExitValueException ex) {
      getLog().error("Error in proces custom script", ex);
    }

    return result;
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
  public boolean isEnforcePrintOutput() {
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

  private void preparePackageList() throws MojoExecutionException {
    final boolean debugEnabled = getLog().isDebugEnabled();

    if (debugEnabled) {
      getLog().debug("Preparing package list");
    }

    final String[] packagesInConfiguration = super.getPackages();
    final List<PackageList.Package> list = new ArrayList<>();

    final String extPackageFilePath = this.getExternalPackageFile();

    if (extPackageFilePath != null) {
      if ("none".equals(extPackageFilePath)) {
        getLog().warn("Provided value 'none' as package list file name, so that it is ignored");
      } else {
        final File extFile = new File(extPackageFilePath);

        getLog().info("Loading external package list file : " + extFile.getAbsolutePath());

        try {
          String text = FileUtils.readFileToString(extFile, "UTF-8");
          if (getLog().isDebugEnabled()) {
            getLog().debug(text);
          }
          text = interpolate(text);
          list.addAll(new PackageList(extFile, text, new PackageList.ContentProvider() {
            @Override
            @Nonnull
            public String readContent(@Nonnull final File contentFile) throws IOException {
              if (!contentFile.isFile()) {
                throw new IOException("Can't find file : " + contentFile.getAbsolutePath());
              }
              try {
                String text = FileUtils.readFileToString(contentFile, "UTF-8");
                if (getLog().isDebugEnabled()) {
                  getLog().debug(text);
                }
                return interpolate(text);
              } catch (InterpolationException ex) {
                throw new IOException("Can't interpolate text for error", ex);
              }
            }
          }).getPackages());
        } catch (InterpolationException ex) {
          throw new MojoExecutionException("Interpolation error with file : " + extFile, ex);
        } catch (IOException ex) {
          throw new MojoExecutionException("Can't load external package list file : " + extFile, ex);
        } catch (ParseException ex) {
          throw new MojoExecutionException("Can't parse external package list file", ex);
        }
      }
    } else {
      if (debugEnabled) {
        getLog().debug("There is no provided external package list file");
      }
    }

    if (packagesInConfiguration != null && packagesInConfiguration.length > 0) {
      for (final String p : packagesInConfiguration) {
        list.add(new PackageList.Package(p, this.getBranch(), this.getTag(), this.getRevision()));
      }
    } else {
      if (debugEnabled) {
        getLog().debug("There are no defined packages in mojo configuration");
      }
    }

    this.integralPackageList = Collections.unmodifiableList(list);

    if (debugEnabled) {
      for (final PackageList.Package p : this.integralPackageList) {
        getLog().debug("Added package in list: " + p.makeString());
      }
    }
  }

  @Override
  public void beforeExecution(@Nullable final ProxySettings proxySettings) throws MojoFailureException, MojoExecutionException {

    preparePackageList();

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

      if (packages != null) {
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

    boolean hasTagBranchOrRevision = false;

    final List<PackageList.Package> packages = Assertions.assertNotNull("Integral package list must be not inited", this.integralPackageList);

    for (final PackageList.Package p : packages) {
      hasTagBranchOrRevision |= p.doesNeedCvsProcessing();
      if (hasTagBranchOrRevision) {
        break;
      }
    }

    if (customcvs != null || hasTagBranchOrRevision) {
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

      if (!processCVS(packages, proxySettings, goPath)) {
        throw new MojoFailureException("Can't change branch or tag or execute custom CVS options, see the log for errors!");
      }
    }
  }

  @Nonnull
  private String interpolate(@Nonnull final String str) throws IOException, InterpolationException {
    Interpolator interpolator = new StringSearchInterpolator();
    interpolator.addValueSource(new MapBasedValueSource(this.getProject().getProperties()));
    interpolator.addValueSource(new MapBasedValueSource(System.getProperties()));
    interpolator.addValueSource(new EnvarBasedValueSource());
    return interpolator.interpolate(str);
  }

  @Override
  public boolean isCommandSupportVerbose() {
    return true;
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
