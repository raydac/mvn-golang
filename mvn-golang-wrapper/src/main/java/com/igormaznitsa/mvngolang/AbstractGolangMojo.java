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

import com.igormaznitsa.meta.annotation.LazyInited;
import com.igormaznitsa.meta.annotation.MayContainNull;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.annotation.ReturnsOriginal;
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import static com.igormaznitsa.meta.common.utils.Assertions.assertNotNull;
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.meta.common.utils.StrUtils;
import com.igormaznitsa.mvngolang.utils.IOUtils;
import com.igormaznitsa.mvngolang.utils.MavenUtils;
import com.igormaznitsa.mvngolang.utils.ProxySettings;
import com.igormaznitsa.mvngolang.utils.UnpackUtils;
import com.igormaznitsa.mvngolang.utils.WildCardMatcher;
import com.igormaznitsa.mvngolang.utils.XGoogHashHeader;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public abstract class AbstractGolangMojo extends AbstractMojo {

  public static final String GOARTIFACT_PACKAGING = "mvn-golang";
  public static final String GO_MOD_FILE_NAME = "go.mod";
  public static final String ENV_GO111MODULE = "GO111MODULE";

  /**
   * VERSION, OS, PLATFORM,-OSXVERSION
   */
  public static final String NAME_PATTERN = "go%s.%s-%s%s";
  private static final List<String> ALLOWED_SDKARCHIVE_CONTENT_TYPE = Collections.unmodifiableList(Arrays.asList("application/octet-stream", "application/zip", "application/x-tar", "application/x-gzip"));
  private static final ReentrantLock LOCKER = new ReentrantLock();
  private static final String[] BANNER = new String[]{"______  ___             _________     ______",
    "___   |/  /__   __________  ____/________  / ______ ______________ _",
    "__  /|_/ /__ | / /_  __ \\  / __ _  __ \\_  /  _  __ `/_  __ \\_  __ `/",
    "_  /  / / __ |/ /_  / / / /_/ / / /_/ /  /___/ /_/ /_  / / /  /_/ / ",
    "/_/  /_/  _____/ /_/ /_/\\____/  \\____//_____/\\__,_/ /_/ /_/_\\__, /",
    "                                                           /____/",
    "                  https://github.com/raydac/mvn-golang",
    ""
  };
  /**
   * set of flags to be ignored among build and extra build flags, for inside
   * use
   */
  protected final Set<String> buildFlagsToIgnore = new HashSet<>();
  protected final List<String> tempBuildFlags = new ArrayList<>();

  @Parameter(defaultValue = "${settings}", readonly = true)
  protected Settings settings;

  @Component
  private ArtifactResolver artifactResolver;

  @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
  private List<ArtifactRepository> remoteRepositories;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
  private MojoExecution execution;

  /**
   * Flag to turn on support for module mode. Dependencies will not be added
   * into GOPATH, go.mod files will be preprocessed to have replace links to
   * each other locally. After processing all go.mod files are restored from
   * backup.
   *
   * @since 2.3.3
   */
  @Parameter(name = "moduleMode", defaultValue = "false", property = "mvn.golang.module.mode")
  private boolean moduleMode;

  /**
   * Path to be used as working directory for executing process, by default it
   * is unset and working directory depends on mode and command.
   *
   * @since 2.3.3
   */
  @Parameter(name = "workingDir")
  private String workingDir;

  @Nullable
  public final String getWorkingDir() {
    return this.workingDir;
  }

  public final void setWorkingDir(@Nullable final String path) {
    this.workingDir = path;
  }

  public boolean isModuleMode() {
    return this.moduleMode;
  }

  public void setModuleMode(final boolean value) {
    this.moduleMode = value;
  }

  /**
   * Flag shows that environment PATH variable should be filtered for footsteps
   * of other go/bin folders to prevent conflicts.
   *
   * @since 2.3.0
   */
  @Parameter(defaultValue = "true", name = "filterEnvPath")
  private boolean filterEnvPath = true;

  /**
   * Check hash for downloaded SDK archive.
   *
   * @since 2.3.0
   */
  @Parameter(name = "checkSdkHash", defaultValue = "true")
  private boolean checkSdkHash = true;

  /**
   * Use proxy server defined for maven.
   *
   * @since 2.3.0
   */
  @Parameter(name = "useMavenProxy", defaultValue = "true")
  private boolean useMavenProxy;

  /**
   * Disable check of SSL certificate during HTTPS request. Also can be changed
   * by system property 'mvn.golang.disable.ssl.check'
   *
   * @since 2.1.7
   */
  @Parameter(name = "disableSSLcheck", defaultValue = "false")
  private boolean disableSSLcheck;

  /**
   * Suppose SDK archive file name if it is not presented in the list loaded
   * from server.
   *
   * @since 2.1.6
   */
  @Parameter(name = "supposeSdkArchiveFileName", defaultValue = "true")
  private boolean supposeSdkArchiveFileName;

  /**
   * Parameters of proxy server to be used to make connection to SDK server.
   *
   * @since 2.1.1
   */
  @Parameter(name = "proxy")
  private ProxySettings proxy;

  /**
   * Skip execution of the mojo. Also can be disabled through system property
   * `mvn.golang.skip'
   *
   * @since 2.1.2
   */
  @Parameter(name = "skip", defaultValue = "false")
  private boolean skip;

  /**
   * Ignore error exit code returned by GoLang tool and don't generate any
   * failure.
   *
   * @since 2.1.1
   */
  @Parameter(name = "ignoreErrorExitCode", defaultValue = "false")
  private boolean ignoreErrorExitCode;

  /**
   * Folder to place console logs.
   *
   * @since 2.1.1
   */
  @Parameter(name = "reportsFolder", defaultValue = "${project.build.directory}${file.separator}reports")
  private String reportsFolder;

  /**
   * File to save console out log. If empty then will not be saved.
   *
   * @since 2.1.1
   */
  @Parameter(name = "outLogFile")
  private String outLogFile;

  /**
   * File to save console error log. If empty then will not be saved
   *
   * @since 2.1.1
   */
  @Parameter(name = "errLogFile")
  private String errLogFile;

  /**
   * Base site for SDK download. By default it uses
   * <a href="https://storage.googleapis.com/golang/">https://storage.googleapis.com/golang/</a>
   */
  @Parameter(name = "sdkSite", defaultValue = "https://storage.googleapis.com/golang/")
  private String sdkSite;

  /**
   * Hide ASC banner.
   */
  @Parameter(defaultValue = "true", name = "hideBanner")
  private boolean hideBanner;

  /**
   * Folder to be used to save and unpack loaded SDKs and also keep different
   * info. By default it has value "${user.home}${file.separator}.mvnGoLang"
   */
  @Parameter(defaultValue = "${user.home}${file.separator}.mvnGoLang", name = "storeFolder")
  private String storeFolder;

  /**
   * Folder to be used as $GOPATH. NB! By default it has value
   * "${user.home}${file.separator}.mvnGoLang${file.separator}.go_path"
   */
  @Parameter(defaultValue = "${user.home}${file.separator}.mvnGoLang${file.separator}.go_path", name = "goPath")
  private String goPath;

  /**
   * Value to be provided as $GO386. This controls the code generated by gc to
   * use either the 387 floating-point unit (set to 387) or SSE2 instructions
   * (set to sse2) for floating point computations.
   *
   * @since 2.1.7
   */
  private String target386;

  /**
   * Value to be provided as $GOARM. This sets the ARM floating point
   * co-processor architecture version the run-time should target. If you are
   * compiling on the target system, its value will be auto-detected.
   *
   * @since 2.1.1
   */
  @Parameter(name = "targetArm")
  private String targetArm;

  /**
   * Folder to be used as $GOBIN. NB! By default it has value
   * "${project.build.directory}". It is possible to disable usage of GOBIN in
   * process through value <b>NONE</b>
   */
  @Parameter(defaultValue = "${project.build.directory}", name = "goBin")
  private String goBin;

  /**
   * The Go SDK version. It plays role if goRoot is undefined. Can be defined
   * through system property 'mvn.golang.go.version'
   */
  @Parameter(name = "goVersion", defaultValue = "1.12.9", property = "mvn.golang.go.version")
  private String goVersion;

  /**
   * Cache directory to keep build data. It affects GOCACHE environment
   * variable. By default it is turned off by value `off`
   *
   * @since 2.3.1
   */
  @Parameter(name = "goCache", defaultValue = "${project.build.directory}${file.separator}.goBuildCache")
  private String goCache;

  /**
   * The Go home folder. It can be undefined and in the case the plug-in will
   * make automatic business to find SDK in its cache or download it.
   */
  @Parameter(name = "goRoot")
  private String goRoot;

  /**
   * The Go bootstrap home folder.
   */
  @Parameter(name = "goRootBootstrap")
  private String goRootBootstrap;

  /**
   * Make GOPATH value as the last one in new generated GOPATH chain.
   *
   * @since 2.1.3
   */
  @Parameter(name = "enforceGoPathToEnd", defaultValue = "false")
  private boolean enforceGoPathToEnd;

  /**
   * Sub-path to executing go tool in SDK folder.
   *
   * @since 1.1.0
   */
  @Parameter(name = "execSubpath", defaultValue = "bin")
  private String execSubpath;

  /**
   * Go tool to be executed. NB! An Extension for OS will be automatically
   * added.
   *
   * @since 1.1.0
   */
  @Parameter(name = "exec", defaultValue = "go")
  private String exec;

  /**
   * Allows defined text to be printed before execution as warning in to log.
   */
  @Parameter(name = "echoWarn")
  private String[] echoWarn;

  /**
   * Allows defined text to be printed before execution as info into log.
   */
  @Parameter(name = "echo")
  private String[] echo;

  /**
   * Disable loading GoLang SDK through network if it is not found at cache.
   */
  @Parameter(name = "disableSdkLoad", defaultValue = "false")
  private boolean disableSdkLoad;

  /**
   * GoLang source directory. By default <b>${project.build.sourceDirectory}</b>
   */
  @Parameter(defaultValue = "${project.build.sourceDirectory}", name = "sources")
  private String sources;

  /**
   * The Target OS.
   */
  @Parameter(name = "targetOs")
  private String targetOs;

  /**
   * The OS. If it is not defined then plug-in will try figure out the current
   * one.
   */
  @Parameter(name = "os")
  private String os;

  /**
   * The Target architecture.
   */
  @Parameter(name = "targetArch")
  private String targetArch;

  /**
   * The Architecture. If it is not defined then plug-in will try figure out the
   * current one.
   */
  @Parameter(name = "arch")
  private String arch;

  /**
   * Version of OSX to be used during distributive name synthesis.
   */
  @Parameter(name = "osxVersion")
  private String osxVersion;

  /**
   * List of optional build flags.
   */
  @Parameter(name = "buildFlags")
  private String[] buildFlags;

  /**
   * Be verbose in logging.
   */
  @Parameter(name = "verbose", defaultValue = "false", property = "mvn.golang.verbose")
  private boolean verbose;

  /**
   * Do not delete SDK archive after unpacking.
   */
  @Parameter(name = "keepSdkArchive", defaultValue = "false")
  private boolean keepSdkArchive;

  /**
   * Name of tool to be called instead of standard 'go' tool.
   */
  @Parameter(name = "useGoTool")
  private String useGoTool;

  /**
   * Flag to override all provided configuration variables by their environment
   * values if such value is detected
   * <ul>
   * <li>goRoot by $GOROOT</li>
   * <li>goRootBootstrap by $GOROOT_BOOTSTRAP</li>
   * <li>targetOs by $GOOS</li>
   * <li>targetArch by $GOARCH</li>
   * <li>targetArm by $GOARM</li>
   * <li>goPath by $GOPATH</li>
   * </ul>
   * <b>NB! Your configuration values will be ignored if you define the flag
   * because it has higher priority!</b>
   */
  @Parameter(name = "useEnvVars", defaultValue = "false")
  private boolean useEnvVars;

  /**
   * It allows to define key value pairs which will be used as environment
   * variables for started GoLang process.
   */
  @Parameter(name = "env")
  private Map<?, ?> env;

  /**
   * Allows directly define name of SDK archive. If it is not defined then
   * plug-in will try to generate name and find such one in downloaded SDK
   * list..
   */
  @Parameter(name = "sdkArchiveName")
  private String sdkArchiveName;

  /**
   * Directly defined URL to download GoSDK. In the case SDK list will not be
   * downloaded and plug-in will try download archive through the link.
   */
  @Parameter(name = "sdkDownloadUrl")
  private String sdkDownloadUrl;

  /**
   * Timeout for HTTP connection in milliseconds.
   *
   * @since 2.3.0
   */
  @Parameter(name = "connectionTimeout", defaultValue = "60000")
  private int connectionTimeout = 60000;

  /**
   * Keep unpacked wrongly SDK folder.
   */
  @Parameter(name = "keepUnarchFolderIfError", defaultValue = "false")
  private boolean keepUnarchFolderIfError;

  /**
   * Allows to define folders which will be added into $GOPATH
   *
   * @since 2.0.0
   */
  @Parameter(name = "addToGoPath")
  private String[] addToGoPath;
  @LazyInited
  private CloseableHttpClient httpClient;
  @LazyInited
  private ByteArrayOutputStream consoleErrBuffer;
  @LazyInited
  private ByteArrayOutputStream consoleOutBuffer;

  private static final Pattern GOBINFOLDER_PATTERN = Pattern.compile("(?:\\\\|/)go[0-9\\-\\+.]*(?:\\\\|/)bin(?:\\\\|/)?$", Pattern.CASE_INSENSITIVE);

  @Nonnull
  public ArtifactResolver getArtifactResolver() {
    return assertNotNull("Artifact resolver component is not provided by Maven", this.artifactResolver);
  }

  @Nonnull
  @MustNotContainNull
  public List<ArtifactRepository> getRemoteRepositories() {
    return this.remoteRepositories;
  }

  @Nonnull
  private static String ensureNoSurroundingSlashes(@Nonnull final String str) {
    String result = str;
    if (!result.isEmpty() && (result.charAt(0) == '/' || result.charAt(0) == '\\')) {
      result = result.substring(1);
    }
    if (!result.isEmpty() && (result.charAt(result.length() - 1) == '/' || result.charAt(result.length() - 1) == '\\')) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private static void deleteFileIfExists(@Nonnull final File file) throws IOException {
    if (file.isFile() && !file.delete()) {
      throw new IOException("Can't delete file : " + file);
    }
  }

  private static boolean isSafeEmpty(@Nullable final String value) {
    return value == null || value.isEmpty();
  }

  protected boolean doesNeedSessionLock() {
    return false;
  }

  /**
   * Generate unique file name in bounds current maven session.
   *
   * @return file name, must not be null
   * @since 2.3.3
   */
  @Nonnull
  private String makeSessionLockFileName() {
    final String id = Long.toHexString(this.getSession().getStartTime().getTime()).toUpperCase(Locale.ENGLISH);
    return ".#mvn.go.session.lock." + id;
  }

  @Nonnull
  protected File getTempFileFolder() {
    return new File(System.getProperty("java.io.tmpdir"));
  }
  
  /**
   * Internal method to generate session locking file for mvn-golang mojo. If
   * file exists then it will be waiting for its removing to create new one.
   *
   * @throws MojoExecutionException it will be thrown if any error in process
   * @since 2.3.3
   */
  private void lockMvnGolangSession() throws MojoExecutionException {
    final File lockFile = new File(this.getTempFileFolder(), makeSessionLockFileName());

    this.getLog().debug("Locking project for mvn-golang sync processing, locker file: " + lockFile);
    while (!Thread.currentThread().isInterrupted()) {
      final boolean locked;

      try {
        locked = lockFile.createNewFile();
      } catch (IOException ex) {
        throw new MojoExecutionException("Detected error during attempt to make locker file: " + lockFile, ex);
      }

      if (locked) {
        lockFile.deleteOnExit();
        this.getLog().debug("Locking file created: " + lockFile);
        return;
      } else {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
  }

  /**
   * Internal method to unlock current mvn-golang session through removing of
   * the locking file. If file can't be found then it warns and continue work as
   * if it would be removed.
   *
   * @throws MojoExecutionException it is thrown if any error
   * @since 2.3.3
   */
  private void unlockMvnGolangSession() throws MojoExecutionException {
    final File locker = new File(this.getTempFileFolder(), makeSessionLockFileName());

    this.getLog().debug("Unlocking project for mvn-golang sync processing, locker file: " + locker);
    if (locker.isFile()) {
      if (!locker.delete()) {
        throw new MojoExecutionException("Can't delete locker file: " + locker);
      }
    } else {
      this.getLog().warn("Can't detect locker file, may be it was removed externally: " + locker);
    }
  }

  @Nonnull
  private static String extractExtensionOfArchive(@Nonnull final String archiveName) {
    final String lcName = archiveName.toLowerCase(Locale.ENGLISH);
    final String result;
    if (lcName.endsWith(".tar.gz")) {
      result = archiveName.substring(archiveName.length() - "tar.gz".length());
    } else {
      result = FilenameUtils.getExtension(archiveName);
    }
    return result;
  }

  @Nonnull
  private static String investigateArch() {
    final String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
    if (arch.contains("arm")) {
      return "arm";
    }
    if (arch.equals("386") || arch.equals("i386") || arch.equals("x86")) {
      return "386";
    }
    return "amd64";
  }

  @Nonnull
  protected static String adaptExecNameForOS(@Nonnull final String execName) {
    return execName + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");
  }

  @Nonnull
  private static String getPathToFolder(@Nonnull final String path) {
    String text = path;
    if (!text.endsWith("/") && !text.endsWith("\\")) {
      text = text + File.separatorChar;
    }
    return text;
  }

  @Nonnull
  private static String getPathToFolder(@Nonnull final File path) {
    return getPathToFolder(path.getAbsolutePath());
  }

  @Nullable
  protected static File findExisting(@Nonnull @MayContainNull final File... files) {
    File result = null;
    for (final File f : files) {
      if (f != null && f.isFile()) {
        result = f;
        break;
      }
    }
    return result;
  }

  @Nonnull
  private static String removeSrcFolderAtEndIfPresented(@Nonnull final String text) {
    String result = text;
    if (text.endsWith("/src") || text.endsWith("\\src")) {
      result = text.substring(0, text.length() - 4);
    }
    return result;
  }

  @Nonnull
  private File loadSDKAndUnpackIntoCache(
          @Nullable final ProxySettings proxySettings, 
          @Nonnull final File cacheFolder, 
          @Nonnull final String baseSdkName, 
          final boolean dontLoadIfNotInCache
  ) throws IOException, MojoExecutionException {
    synchronized (AbstractGolangMojo.class) {
      final File sdkFolder = new File(cacheFolder, baseSdkName);

      if (sdkFolder.isDirectory()) {
        return sdkFolder;
      }

      final File lockFile = new File(cacheFolder, ".lck" + baseSdkName);
      lockFile.deleteOnExit();
      try {
        if (!lockFile.createNewFile()) {
          this.getLog().info("Detected SDK loading, waiting for the process end");
          while (lockFile.exists()) {
            try {
              Thread.sleep(100L);
            } catch (InterruptedException ex) {
              throw new IOException("Wait of SDK loading is interrupted", ex);
            }
          }
          this.getLog().info("Loading process has been completed");
          return sdkFolder;
        }

        if (sdkFolder.isDirectory()) {
          if (this.isVerbose() || this.getLog().isDebugEnabled()) {
            this.getLog().info("SDK cache folder : " + sdkFolder);
          }
          return sdkFolder;
        } else if (dontLoadIfNotInCache || this.session.isOffline()) {
          this.getLog().error("Can't find cached Golang SDK and downloading is disabled or Maven in offline mode");
          throw new IOException("Can't find " + baseSdkName + " in the cache but loading is directly disabled");
        }

        final String predefinedLink = this.getSdkDownloadUrl();

        final File archiveFile;
        final String linkForDownloading;

        if (isSafeEmpty(predefinedLink)) {
          this.logOptionally("There is not any predefined SDK URL");
          final String sdkFileName = this.findSdkArchiveFileName(proxySettings, baseSdkName);
          archiveFile = new File(cacheFolder, sdkFileName);
          linkForDownloading = this.getSdkSite() + sdkFileName;
        } else {
          final String extension = extractExtensionOfArchive(assertNotNull(predefinedLink));
          archiveFile = new File(cacheFolder, baseSdkName + '.' + extension);
          linkForDownloading = predefinedLink;
          this.logOptionally("Using predefined URL to download SDK : " + linkForDownloading);
          this.logOptionally("Detected extension of archive : " + extension);
        }

        if (archiveFile.exists()) {
          this.logOptionally("Detected existing archive " + archiveFile + ", deleting it and reload");
          if (!archiveFile.delete()) {
            throw new IOException("Can't delete archive file: " + archiveFile);
          }
        }

        final HttpGet methodGet = new HttpGet(linkForDownloading);
        final RequestConfig config = this.processRequestConfig(proxySettings, this.getConnectionTimeout(), RequestConfig.custom()).build();
        methodGet.setConfig(config);

        boolean errorsDuringLoading = true;
        boolean showProgressBar;

        try {
          if (!archiveFile.isFile()) {
            this.getLog().warn("Loading SDK archive with URL : " + linkForDownloading);

            final HttpResponse response = this.getHttpClient(proxySettings).execute(methodGet);
            final StatusLine statusLine = response.getStatusLine();

            this.getLog().debug("HttpResponse: " + response);

            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
              throw new IOException(String.format("Can't load SDK archive from %s : %d %s", linkForDownloading, statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            }

            final XGoogHashHeader xGoogHash = new XGoogHashHeader(response.getHeaders("x-goog-hash"));
            this.getLog().debug("XGoogHashHeader: " + xGoogHash);

            final HttpEntity entity = response.getEntity();
            final Header contentType = entity.getContentType();

            if (!ALLOWED_SDKARCHIVE_CONTENT_TYPE.contains(contentType.getValue())) {
              throw new IOException("Unsupported content type : " + contentType.getValue());
            }

            final long size = entity.getContentLength();
            try (final InputStream inStream = entity.getContent()) {
              showProgressBar = size > 0L && !this.session.isParallel();
              this.getLog().info("Downloading SDK archive into file : " + archiveFile);
              long loadedCounter = 0L;
              final byte[] buffer = new byte[1024 * 1024];
              int lastRenderedValue = -1;
              final int PROGRESSBAR_WIDTH = 10;
              final String LOADING_TITLE = "Loading " + size / (1024L * 1024L) + " Mb ";
              if (showProgressBar) {
                lastRenderedValue = IOUtils.printTextProgressBar(LOADING_TITLE, 0, size, PROGRESSBAR_WIDTH, lastRenderedValue);
              }
              final OutputStream fileOutStream = new BufferedOutputStream(new FileOutputStream(archiveFile), 128 * 16384);
              try {
                while (!Thread.currentThread().isInterrupted()) {
                  final int readCounter = inStream.read(buffer);
                  if (readCounter < 0) {
                    break;
                  }
                  fileOutStream.write(buffer, 0, readCounter);
                  loadedCounter += readCounter;
                  if (showProgressBar) {
                    lastRenderedValue = IOUtils.printTextProgressBar(LOADING_TITLE, loadedCounter, size, PROGRESSBAR_WIDTH, lastRenderedValue);
                  }
                }
              } finally {
                if (showProgressBar) {
                  System.out.println();
                }
                IOUtils.closeSilently(fileOutStream);
              }
              if (Thread.currentThread().isInterrupted()) {
                throw new MojoExecutionException("Interrupted");
              }
              this.getLog().info("Archived SDK has been succesfully downloaded, its size is " + (archiveFile.length() / 1024L) + " Kb");
            }

            if (this.isCheckSdkHash()) {
              if (xGoogHash.isValid() && xGoogHash.hasData()) {
                this.getLog().debug("Checking hash of file");
                final boolean fileHashOk = xGoogHash.isFileOk(this.getLog(), archiveFile);
                if (fileHashOk) {
                  this.getLog().info("Downloaded archive file hash is OK");
                } else {
                  this.getLog().error("Downloaded archive file hash is BAD");
                  throw new MojoExecutionException("Downloaded SDK archive has wrong hash");
                }
              } else {
                if (!xGoogHash.isValid()) {
                  throw new MojoExecutionException("Couldn't parse x-goog-hash from response: " + response);
                } else {
                  throw new MojoExecutionException("Parsed x-goog-hash has not needed data: " + xGoogHash);
                }
              }
            }

          } else {
            this.getLog().info("Archive file of SDK has been found in the cache : " + archiveFile);
          }

          errorsDuringLoading = false;

          final File interFolder = this.unpackArchToFolder(archiveFile, "go", new File(cacheFolder, ".#" + baseSdkName));

          this.getLog().info("Renaming " + interFolder.getName() + " to " + sdkFolder.getName());
          if (interFolder.renameTo(sdkFolder)) {
            this.logOptionally("Renamed successfully: " + interFolder + " -> " + sdkFolder);
          } else {
            throw new IOException("Can't rename temp GoSDK folder: " + interFolder + " -> " + sdkFolder);
          }

          return sdkFolder;
        } finally {
          methodGet.releaseConnection();
          if (errorsDuringLoading || !this.isKeepSdkArchive()) {
            this.logOptionally("Deleting archive : " + archiveFile + (errorsDuringLoading ? " (because error during loading)" : ""));
            deleteFileIfExists(archiveFile);
          } else {
            this.logOptionally("Archive file is kept for special flag : " + archiveFile);
          }
        }
      } finally {
        final boolean deleted = FileUtils.deleteQuietly(lockFile);
        this.getLog().debug("Lock file " + lockFile + " deleted : " + deleted);
      }
    }
  }

  public boolean isFilterEnvPath() {
    return this.filterEnvPath;
  }

  public boolean isSkip() {
    final boolean result = this.skip 
            || Boolean.parseBoolean(MavenUtils.findProperty(this.getProject(), "mvn.golang.skip", "false"));
    
    final String skipMojoSuffix = this.getSkipMojoPropertySuffix();
    return skipMojoSuffix == null ? result : result 
            || Boolean.parseBoolean(MavenUtils.findProperty(this.getProject(), String.format("mvn.golang.%s.skip", skipMojoSuffix), "false"));
  }

  public boolean isEnforceGoPathToEnd() {
    return this.enforceGoPathToEnd;
  }

  @Nonnull
  public MavenProject getProject() {
    return this.project;
  }

  @Nonnull
  public MojoExecution getExecution() {
    return this.execution;
  }

  @Nonnull
  public MavenSession getSession() {
    return this.session;
  }

  public boolean isIgnoreErrorExitCode() {
    return this.ignoreErrorExitCode;
  }

  @Nonnull
  public Map<?, ?> getEnv() {
    return GetUtils.ensureNonNull(this.env, Collections.EMPTY_MAP);
  }

  @Nullable
  public String getSdkDownloadUrl() {
    return this.sdkDownloadUrl;
  }

  @Nonnull
  public String getExecSubpath() {
    return ensureNoSurroundingSlashes(assertNotNull(this.execSubpath));
  }

  public int getConnectionTimeout() {
    return this.connectionTimeout;
  }

  @Nonnull
  public String getExec() {
    return ensureNoSurroundingSlashes(assertNotNull(this.exec));
  }

  public boolean isUseEnvVars() {
    return this.useEnvVars;
  }

  public boolean isKeepSdkArchive() {
    return this.keepSdkArchive;
  }

  public boolean isKeepUnarchFolderIfError() {
    return this.keepUnarchFolderIfError;
  }

  @Nullable
  public String getSdkArchiveName() {
    return this.sdkArchiveName;
  }

  @Nonnull
  public String getReportsFolder() {
    return this.reportsFolder;
  }

  @Nullable
  public String getOutLogFile() {
    return this.outLogFile;
  }

  @Nullable
  public String getErrLogFile() {
    return this.errLogFile;
  }

  public boolean isCheckSdkHash() {
    return this.checkSdkHash;
  }

  @Nonnull
  public String getStoreFolder() {
    return this.storeFolder;
  }

  @Nullable
  public String getUseGoTool() {
    return this.useGoTool;
  }

  public boolean isVerbose() {
    return this.verbose;
  }

  public boolean isDisableSdkLoad() {
    return this.disableSdkLoad;
  }

  @Nonnull
  public String getSdkSite() {
    return assertNotNull(this.sdkSite);
  }

  @Nonnull
  @MustNotContainNull
  public String[] getBuildFlags() {
    final List<String> result = new ArrayList<>();
    for (final String s : ArrayUtils.joinArrays(GetUtils.ensureNonNull(this.buildFlags, ArrayUtils.EMPTY_STRING_ARRAY), getExtraBuildFlags())) {
      if (!this.buildFlagsToIgnore.contains(s)) {
        result.add(s);
      }
    }

    result.addAll(this.tempBuildFlags);

    return result.toArray(new String[0]);
  }

  @Nonnull
  @MustNotContainNull
  protected String[] getExtraBuildFlags() {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Nonnull
  @MustNotContainNull
  public File[] findGoPath(final boolean ensureExist) throws IOException {
    LOCKER.lock();
    try {
      final String foundGoPath = getGoPath();
      if (getLog().isDebugEnabled()) {
        getLog().debug("findGoPath(" + ensureExist + "), getGoPath() returns " + foundGoPath);
      }

      final List<File> result = new ArrayList<>();

      for (final String p : foundGoPath.split(String.format("\\%s", File.pathSeparator))) {
        final File folder = new File(p);
        result.add(folder);
        if (ensureExist && !folder.isDirectory() && !folder.mkdirs()) {
          throw new IOException("Can't create folder for GOPATH : " + folder.getAbsolutePath());
        }
      }

      return result.toArray(new File[0]);
    } finally {
      LOCKER.unlock();
    }
  }

  @Nullable
  public File findGoRootBootstrap(final boolean ensureExist) throws IOException {
    LOCKER.lock();
    try {
      final String value = getGoRootBootstrap();
      File result = null;
      if (value != null) {
        result = new File(value);
        if (ensureExist && !result.isDirectory()) {
          throw new IOException("Can't find folder for GOROOT_BOOTSTRAP: " + result);
        }
      }
      return result;
    } finally {
      LOCKER.unlock();
    }
  }

  @Nonnull
  public String getOs() {
    String result = this.os;
    if (isSafeEmpty(result)) {
      if (SystemUtils.IS_OS_WINDOWS) {
        result = "windows";
      } else if (SystemUtils.IS_OS_LINUX) {
        result = "linux";
      } else if (SystemUtils.IS_OS_FREE_BSD) {
        result = "freebsd";
      } else {
        result = "darwin";
      }
    }
    return result;
  }

  @Nonnull
  public String getArch() {
    String result = this.arch;
    if (isSafeEmpty(result)) {
      result = investigateArch();
    }
    return result;
  }

  @Nullable
  private String getValueOrEnv(@Nonnull final String varName, @Nullable final String configValue) {
    final String foundInEnvironment = System.getenv(varName);
    String result = configValue;

    if (foundInEnvironment != null && isUseEnvVars()) {
      if (!isSafeEmpty(configValue)) {
        getLog().warn(String.format("Value %s is replaced by environment value.", varName));
      }
      result = foundInEnvironment;
    }
    return result;
  }

  @Nullable
  public String getGoRoot() {
    return getValueOrEnv("GOROOT", this.goRoot);
  }

  @Nullable
  public String getGoCache() {
    return getValueOrEnv("GOCACHE", this.goCache);
  }

  @Nullable
  public String getGoRootBootstrap() {
    return getValueOrEnv("GOROOT_BOOTSTRAP", this.goRootBootstrap);
  }

  @Nullable
  public String getGoBin() {
    String result = getValueOrEnv("GOBIN", this.goBin);
    return "NONE".equals(result) ? null : result;
  }

  @Nonnull
  public String getGoPath() {
    return assertNotNull(getValueOrEnv("GOPATH", this.goPath));
  }

  @Nullable
  public String getTargetArm() {
    return getValueOrEnv("GOARM", this.targetArm);
  }

  @Nullable
  public String getTarget386() {
    return getValueOrEnv("GO386", this.target386);
  }

  @Nullable
  public String getTargetOS() {
    return getValueOrEnv("GOOS", this.targetOs);
  }

  @Nullable
  public String getTargetArch() {
    return getValueOrEnv("GOARCH", this.targetArch);
  }

  public boolean isUseMavenProxy() {
    return this.useMavenProxy;
  }

  public boolean getSupposeSdkArchiveFileName() {
    return this.supposeSdkArchiveFileName;
  }

  public boolean isDisableSslCheck() {
    return this.disableSSLcheck || Boolean.parseBoolean(MavenUtils.findProperty(this.getProject(), "mvn.golang.disable.ssl.check", "false"));
  }

  public void setDisableSslCheck(final boolean flag) {
    this.disableSSLcheck = flag;
  }

  @Nullable
  public ProxySettings getProxy() {
    return this.proxy;
  }

  @Nullable
  public String getOSXVersion() {
    return this.osxVersion;
  }

  @Nonnull
  public String getGoVersion() {
    return this.goVersion;
  }

  @Nonnull
  public File getSources(final boolean ensureExist) throws IOException {
    final File result = new File(this.sources);
    if (ensureExist && !result.isDirectory()) {
      throw new IOException("Can't find GoLang project sources : " + result);
    }
    return result;
  }

  protected void addTmpBuildFlagIfNotPresented(@Nonnull @MustNotContainNull final String... flags) {
    for (final String s : flags) {
      if (this.tempBuildFlags.contains(s)) {
        continue;
      }
      boolean found = false;
      if (this.buildFlags != null) {
        for (final String b : this.buildFlags) {
          if (s.equals(b)) {
            found = true;
            break;
          }
        }
      }
      if (!found) {
        this.tempBuildFlags.add(s);
      }
    }
  }

  @Nonnull
  private static String extractComputerName() {
    String result = System.getenv("COMPUTERNAME");
    if (result == null) {
      result = System.getenv("HOSTNAME");
    }
    if (result == null) {
      try {
        result = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException ex) {
        result = null;
      }
    }
    return GetUtils.ensureNonNull(result, "<Unknown computer>");
  }

  @Nonnull
  private static String extractDomainName() {
    final String result = System.getenv("USERDOMAIN");
    return GetUtils.ensureNonNull(result, "");
  }

  @Nonnull
  private synchronized HttpClient getHttpClient(@Nullable final ProxySettings proxy) throws MojoExecutionException {
    if (this.httpClient == null) {
      final HttpClientBuilder builder = HttpClients.custom();

      if (proxy != null) {
        if (proxy.hasCredentials()) {
          final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
          credentialsProvider.setCredentials(new AuthScope(proxy.host, proxy.port),
                  new NTCredentials(GetUtils.ensureNonNull(proxy.username, ""), proxy.password, extractComputerName(), extractDomainName()));
          builder.setDefaultCredentialsProvider(credentialsProvider);
          getLog().debug(String.format("Credentials provider has been created for proxy (username : %s): %s", proxy.username, proxy.toString()));
        }

        final String[] ignoreForAddresses = proxy.nonProxyHosts == null ? new String[0] : proxy.nonProxyHosts.split("\\|");

        final WildCardMatcher[] matchers;

        if (ignoreForAddresses.length > 0) {
          matchers = new WildCardMatcher[ignoreForAddresses.length];
          for (int i = 0; i < ignoreForAddresses.length; i++) {
            matchers[i] = new WildCardMatcher(ignoreForAddresses[i]);
          }
        } else {
          matchers = new WildCardMatcher[0];
        }

        getLog().debug("Regular routing mode");

        final HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(new HttpHost(proxy.host, proxy.port, proxy.protocol)) {
          @Override
          @Nonnull
          public HttpRoute determineRoute(@Nonnull final HttpHost host, @Nonnull final HttpRequest request, @Nonnull final HttpContext context) throws HttpException {
            HttpRoute result = null;
            final String hostName = host.getHostName();
            for (final WildCardMatcher m : matchers) {
              if (m.match(hostName)) {
                getLog().debug("Ignoring proxy for host : " + hostName);
                result = new HttpRoute(host);
                break;
              }
            }
            if (result == null) {
              result = super.determineRoute(host, request, context);
            }
            getLog().debug("Made connection route : " + result);
            return result;
          }
        };

        builder.setRoutePlanner(routePlanner);
        getLog().debug("Proxy will ignore: " + Arrays.toString(matchers));
      }

      builder.setUserAgent("mvn-golang-wrapper-agent/1.0");
      builder.disableCookieManagement();

      if (this.isDisableSslCheck()) {
        this.getLog().warn("SSL certificate check is disabled");
        try {
          final SSLContext sslcontext = SSLContext.getInstance("TLS");
          X509TrustManager tm = new X509TrustManager() {
            @Override
            @Nullable
            @MustNotContainNull
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            @Override
            public void checkClientTrusted(@Nonnull @MustNotContainNull final X509Certificate[] arg0, @Nonnull final String arg1) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(@Nonnull @MustNotContainNull final X509Certificate[] arg0, @Nonnull String arg1) throws CertificateException {
            }
          };
          sslcontext.init(null, new TrustManager[]{tm}, null);

          final SSLConnectionSocketFactory sslfactory = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
          final Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
                  .register("https", sslfactory)
                  .register("http", new PlainConnectionSocketFactory()).build();

          builder.setConnectionManager(new BasicHttpClientConnectionManager(r));
          builder.setSSLSocketFactory(sslfactory);
          builder.setSSLContext(sslcontext);
        } catch (final KeyManagementException | NoSuchAlgorithmException ex) {
          throw new MojoExecutionException("Can't disable SSL certificate check", ex);
        }
      } else {
        this.getLog().debug("SSL check is enabled");
      }
      this.httpClient = builder.build();
    }
    return this.httpClient;
  }

  @Nullable
  private ProxySettings extractProxySettings() {
    final ProxySettings result;
    if (this.isUseMavenProxy()) {
      final Proxy activeMavenProxy = this.settings == null ? null : this.settings.getActiveProxy();
      result = activeMavenProxy == null ? null : new ProxySettings(activeMavenProxy);
      getLog().debug("Detected maven proxy : " + result);
    } else {
      result = this.proxy;
      if (result != null) {
        getLog().debug("Defined proxy : " + result);
      }
    }
    return result;
  }

  @ReturnsOriginal
  @Nonnull
  private RequestConfig.Builder processRequestConfig(@Nullable final ProxySettings proxySettings, final int timeout, @Nonnull final RequestConfig.Builder config) {
    this.getLog().debug("Connection(timeout=" + timeout + "ms, proxySettings=" + proxySettings + ')');
    if (proxySettings != null) {
      final HttpHost proxyHost = new HttpHost(proxySettings.host, proxySettings.port, proxySettings.protocol);
      config.setProxy(proxyHost);
    }
    return config.setConnectTimeout(timeout).setSocketTimeout(timeout);
  }

  @Nonnull
  private String loadGoLangSdkList(@Nullable final ProxySettings proxySettings, @Nullable final String keyPrefix) throws IOException, MojoExecutionException {
    final String sdksite = getSdkSite() + (keyPrefix == null ? "" : "?prefix=" + keyPrefix);

    getLog().warn("Loading list of available GoLang SDKs from " + sdksite);
    final HttpGet get = new HttpGet(sdksite);

    final RequestConfig config = processRequestConfig(proxySettings, this.getConnectionTimeout(), RequestConfig.custom()).build();
    get.setConfig(config);

    get.addHeader("Accept", "application/xml");

    try {
      final HttpResponse response = getHttpClient(proxySettings).execute(get);
      final StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
        final String content = EntityUtils.toString(response.getEntity());
        getLog().info("GoLang SDK list has been loaded successfuly");
        getLog().debug(content);
        return content;
      } else {
        throw new IOException(String.format("Can't load list of SDKs from %s : %d %s", sdksite, statusLine.getStatusCode(), statusLine.getReasonPhrase()));
      }
    } finally {
      get.releaseConnection();
    }
  }

  @Nonnull
  private Document convertSdkListToDocument(@Nonnull final String sdkListAsString) throws IOException {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(new InputSource(new StringReader(sdkListAsString)));
    } catch (ParserConfigurationException ex) {
      getLog().error("Can't configure XML parser", ex);
      throw new IOException("Can't configure XML parser", ex);
    } catch (SAXException ex) {
      getLog().error("Can't parse document", ex);
      throw new IOException("Can't parse document", ex);
    } catch (IOException ex) {
      getLog().error("Unexpected IOException", ex);
      throw new IOException("Unexpected IOException", ex);
    }
  }

  private void printEcho() {
    if (this.echoWarn != null) {
      for (final String s : this.echoWarn) {
        getLog().warn(s);
      }
    }

    if (this.echo != null) {
      for (final String s : this.echo) {
        getLog().info(s);
      }
    }
  }

  protected void logOptionally(@Nonnull final String message) {
    if (getLog().isDebugEnabled() || this.verbose) {
      getLog().info(message);
    }
  }

  protected void initConsoleBuffers() {
    getLog().debug("Initing console out and console err buffers");
    this.consoleErrBuffer = new ByteArrayOutputStream();
    this.consoleOutBuffer = new ByteArrayOutputStream();
  }

  @ReturnsOriginal
  @Nonnull
  private File unpackArchToFolder(@Nonnull final File archiveFile, @Nonnull final String folderInArchive, @Nonnull final File destinationFolder) throws IOException {
    getLog().info(String.format("Unpacking archive %s to folder %s", archiveFile.getName(), destinationFolder.getName()));

    boolean detectedError = true;
    try {

      final int unpackedFileCounter = UnpackUtils.unpackFileToFolder(getLog(), folderInArchive, archiveFile, destinationFolder, true);
      if (unpackedFileCounter == 0) {
        throw new IOException("Couldn't find folder '" + folderInArchive + "' in archive or the archive is empty");
      } else {
        getLog().info("Unpacked " + unpackedFileCounter + " file(s)");
      }

      detectedError = false;

    } finally {
      if (detectedError && !isKeepUnarchFolderIfError()) {
        logOptionally("Deleting folder because error during unpack : " + destinationFolder);
        FileUtils.deleteQuietly(destinationFolder);
      }
    }
    return destinationFolder;
  }

  @Nonnull
  private String extractSDKFileName(@Nonnull final String listUrl, @Nonnull final Document doc, @Nonnull final String sdkBaseName, @Nonnull @MustNotContainNull final String[] allowedExtensions) throws IOException {
    getLog().debug("Looking for SDK started with base name : " + sdkBaseName);

    final Set<String> variants = new HashSet<>();
    for (final String ext : allowedExtensions) {
      variants.add(sdkBaseName + '.' + ext);
    }

    final List<String> listedSdk = new ArrayList<>();

    final Element root = doc.getDocumentElement();
    if ("ListBucketResult".equals(root.getTagName())) {
      final NodeList list = root.getElementsByTagName("Contents");
      for (int i = 0; i < list.getLength(); i++) {
        final Element element = (Element) list.item(i);
        final NodeList keys = element.getElementsByTagName("Key");
        if (keys.getLength() > 0) {
          final String text = keys.item(0).getTextContent();
          if (variants.contains(text)) {
            logOptionally("Detected compatible SDK in the SDK list : " + text);
            return text;
          } else {
            listedSdk.add(text);
          }
        }
      }

      if (this.supposeSdkArchiveFileName) {
        final String supposedSdkName = sdkBaseName + '.' + (SystemUtils.IS_OS_WINDOWS ? "zip" : "tar.gz");
        getLog().warn("Can't find SDK file in the loaded list");
        getLog().debug("..................................................");
        for (final String s : listedSdk) {
          getLog().debug(s);
        }
        getLog().debug("..................................................");

        getLog().warn("Supposed name of SDK archive is " + supposedSdkName + ", trying to load it directly! It can be disabled with <supposeSdkArchiveFileName>false</supposeSdkArchiveFileName>)");
        return supposedSdkName;
      }

      getLog().error("Can't find any SDK to be used as " + sdkBaseName);
      getLog().error("GoLang list contains listed SDKs (" + listUrl + ")");
      getLog().error("It is possible directly define link to SDK through configuration parameter <sdkDownloadUrl>..</sdkDownloadUrl>");
      getLog().error("..................................................");
      for (final String s : listedSdk) {
        getLog().error(s);
      }

      throw new IOException("Can't find SDK : " + sdkBaseName);
    } else {
      throw new IOException("It is not a ListBucket file [" + root.getTagName() + ']');
    }
  }

  @Nonnull
  private String findSdkArchiveFileName(@Nullable final ProxySettings proxySettings, @Nonnull final String sdkBaseName) throws IOException, MojoExecutionException {
    String result = getSdkArchiveName();
    if (isSafeEmpty(result)) {
      final Document parsed = convertSdkListToDocument(loadGoLangSdkList(proxySettings, URLEncoder.encode(sdkBaseName, "UTF-8")));
      result = extractSDKFileName(getSdkSite(), parsed, sdkBaseName, new String[]{"tar.gz", "zip"});
    } else {
      getLog().info("SDK archive name is predefined : " + result);
    }
    return GetUtils.ensureNonNullStr(result);
  }

  private void warnIfContainsUC(@Nonnull final String message, @Nonnull final String str) {
    boolean detected = false;
    for (final char c : str.toCharArray()) {
      if (Character.isUpperCase(c)) {
        detected = true;
        break;
      }
    }
    if (detected) {
      getLog().warn(message + " : " + str);
    }
  }

  @Nonnull
  protected File findGoRoot(@Nullable final ProxySettings proxySettings) throws IOException, MojoFailureException, MojoExecutionException {
    final File result;
    LOCKER.lock();
    try {
      final String predefinedGoRoot = this.getGoRoot();

      if (isSafeEmpty(predefinedGoRoot)) {
        final File cacheFolder = new File(this.storeFolder);

        if (!cacheFolder.isDirectory()) {
          if (cacheFolder.isFile()) {
            throw new IOException("Can't create folder '" + cacheFolder + "' because there is presented a file with such name!");
          }
          logOptionally("Making SDK cache folder : " + cacheFolder);
          FileUtils.forceMkdir(cacheFolder);
        }

        final String definedOsxVersion = this.getOSXVersion();
        final String sdkVersion = this.getGoVersion();

        if (isSafeEmpty(sdkVersion)) {
          throw new MojoFailureException("GoLang SDK version is not defined!");
        }

        final String sdkBaseName = String.format(NAME_PATTERN, sdkVersion, this.getOs(), this.getArch(), isSafeEmpty(definedOsxVersion) ? "" : "-" + definedOsxVersion);
        warnIfContainsUC("Prefer usage of lower case chars only for SDK base name", sdkBaseName);
        result = loadSDKAndUnpackIntoCache(proxySettings, cacheFolder, sdkBaseName, isDisableSdkLoad());
      } else {
        logOptionally("Detected predefined SDK root folder : " + predefinedGoRoot);
        result = new File(predefinedGoRoot);
        if (!result.isDirectory()) {
          throw new MojoFailureException("Predefined SDK root is not a directory : " + result);
        }
      }
    } finally {
      LOCKER.unlock();
    }
    return result;
  }

  private void printBanner() {
    for (final String s : BANNER) {
      getLog().info(s);
    }
  }

  public boolean isHideBanner() {
    return this.hideBanner;
  }

  protected boolean doesNeedOneMoreAttempt(@Nonnull final ProcessResult result, @Nonnull final String consoleOut, @Nonnull final String consoleErr) throws IOException, MojoExecutionException {
    return false;
  }

  protected boolean doMainBusiness(@Nullable final ProxySettings proxySettings, final int maxAttempts) throws InterruptedException, MojoFailureException, MojoExecutionException, IOException {
    int iterations = 0;

    boolean error = false;

    while (!Thread.currentThread().isInterrupted()) {
      final ProcessExecutor executor = prepareExecutor(proxySettings);
      if (executor == null) {
        logOptionally("The Mojo should not be executed");
        break;
      }
      final ProcessResult result = executor.executeNoTimeout();
      final int resultCode = result.getExitValue();
      error = resultCode != 0 && !isIgnoreErrorExitCode();
      iterations++;

      final String outLog = extractOutAsString();
      final String errLog = extractErrorOutAsString();

      if (getLog().isDebugEnabled()) {
        getLog().debug("OUT_LOG: " + outLog);
        getLog().debug("ERR_LOG: " + errLog);
      }

      this.processConsoleOut(resultCode, outLog, errLog);
      printLogs(error || isEnforcePrintOutput() || (this.isVerbose() && this.isCommandSupportVerbose()), error, outLog, errLog);

      if (doesNeedOneMoreAttempt(result, outLog, errLog)) {
        if (iterations > maxAttempts) {
          throw new MojoExecutionException("Too many iterations detected, may be some loop and bug at mojo " + this.getClass().getName());
        }
        getLog().warn("Make one more attempt...");
      } else {
        if (!isIgnoreErrorExitCode()) {
          assertProcessResult(result);
        }
        break;
      }
    }

    return error;
  }

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (this.isSkip()) {
      getLog().info("Skipping mvn-golang execution");
    } else {
      if (this.doesNeedSessionLock()) {
        lockMvnGolangSession();
        if (Thread.currentThread().isInterrupted()) {
          throw new MojoFailureException("Current thread is interrupted");
        }
      }
      try {
        if (!isHideBanner()) {
          printBanner();
        }
        doInit();

        printEcho();

        final ProxySettings proxySettings = extractProxySettings();
        beforeExecution(proxySettings);

        Exception exception = null;
        boolean errorDuringMainBusiness = false;
        try {
          errorDuringMainBusiness = doMainBusiness(proxySettings, 10);
        } catch (final IOException | InterruptedException | MojoExecutionException | MojoFailureException ex) {
          if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
          exception = ex;
        } finally {
          afterExecution(null, errorDuringMainBusiness || exception != null);
        }

        if (exception != null) {
          throw new MojoExecutionException(exception.getMessage(), exception);
        } else if (errorDuringMainBusiness) {
          throw new MojoFailureException("Mojo execution failed, see log");
        }
      } finally {
        if (this.doesNeedSessionLock()) {
          unlockMvnGolangSession();
        }
      }
    }
  }

  public void doInit() throws MojoFailureException, MojoExecutionException {

  }

  public void beforeExecution(@Nullable final ProxySettings proxySettings) throws MojoFailureException, MojoExecutionException {

  }

  public void afterExecution(@Nullable final ProxySettings proxySettings, final boolean error) throws MojoFailureException, MojoExecutionException {

  }

  public boolean isEnforcePrintOutput() {
    return false;
  }

  @Nonnull
  private String extractOutAsString() {
    return new String(this.consoleOutBuffer.toByteArray(), Charset.defaultCharset());
  }

  @Nonnull
  private String extractErrorOutAsString() {
    return new String(this.consoleErrBuffer.toByteArray(), Charset.defaultCharset());
  }

  protected void printLogs(final boolean forcePrint, final boolean errorDetected, @Nonnull final String outLog, @Nonnull final String errLog) {
    final boolean outLogNotEmpty = !outLog.isEmpty();
    final boolean errLogNotEmpty = !errLog.isEmpty();

    if (outLogNotEmpty) {
      if (forcePrint || getLog().isDebugEnabled()) {
        getLog().info("");
        getLog().info("---------Exec.Out---------");
        for (final String str : outLog.split("\n")) {
          getLog().info(StrUtils.trimRight(str));
        }
        getLog().info("");
      } else {
        getLog().debug("There is not any log out from the process");
      }
    }

    if (errLogNotEmpty) {
      if (forcePrint) {
        if (errorDetected) {
          getLog().error("");
          getLog().error("---------Exec.Err---------");
          for (final String str : errLog.split("\n")) {
            getLog().error(StrUtils.trimRight(str));
          }
          getLog().error("");
        } else {
          getLog().warn("");
          getLog().warn("---------Exec.Err---------");
          for (final String str : errLog.split("\n")) {
            getLog().warn(StrUtils.trimRight(str));
          }
          getLog().warn("");
        }
      } else {
        getLog().debug("---------Exec.Err---------");
        for (final String str : errLog.split("\n")) {
          getLog().debug(StrUtils.trimRight(str));
        }
      }
    } else {
      getLog().debug("Error log buffer is empty");
    }

  }

  private void assertProcessResult(@Nonnull final ProcessResult result) throws MojoFailureException {
    final int code = result.getExitValue();
    if (code != 0) {
      throw new MojoFailureException("Process exit code : " + code);
    }
  }

  public boolean isSourceFolderRequired() {
    return false;
  }

  public boolean isMojoMustNotBeExecuted() throws MojoFailureException {
    try {
      return isSourceFolderRequired() && !this.getSources(false).isDirectory();
    } catch (IOException ex) {
      throw new MojoFailureException("Can't check source folder", ex);
    }
  }

  @Nonnull
  @MustNotContainNull
  public abstract String[] getTailArguments();

  @Nonnull
  @MustNotContainNull
  public String[] getOptionalExtraTailArguments() {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Nonnull
  public String makeExecutableFileSubpath() {
    return getExecSubpath() + File.separatorChar + getExec();
  }

  @Nonnull
  public abstract String getGoCommand();

  @Nonnull
  @MustNotContainNull
  public abstract String[] getCommandFlags();

  @Nullable
  private String getEnvPath() {
    String path = System.getenv("PATH");
    if (path == null) {
      this.getLog().warn("Can't find any defined PATH in environment");
    } else {
      this.getLog().debug("Found env.PATH: " + path);
    }

    final boolean filter = this.isFilterEnvPath();

    if (path != null) {
      final StringBuilder buffer = new StringBuilder();
      for (final String s : path.split(Pattern.quote(File.pathSeparator))) {
        if (filter && GOBINFOLDER_PATTERN.matcher(s).find()) {
          getLog().debug("Removing item '" + s + "' from PATH because it looks like go/bin");
          continue;
        }
        if (buffer.length() > 0) {
          buffer.append(File.pathSeparator);
        }
        buffer.append(s);
        getLog().debug("Add item '" + s + "' to PATH");
      }
      path = buffer.toString();
    }

    this.getLog().debug("Prepared PATH var content:" + path);

    return path;
  }

  private void addEnvVar(@Nonnull final ProcessExecutor executor, @Nonnull final String name, @Nonnull final String value) {
    logOptionally(" $" + name + " = " + value);
    executor.environment(name, value);
  }

  @Nullable
  protected ProcessExecutor prepareExecutor(@Nullable final ProxySettings proxySettings) throws IOException, MojoFailureException, MojoExecutionException {
    initConsoleBuffers();

    final String execNameAdaptedForOs = adaptExecNameForOS(makeExecutableFileSubpath());
    final File detectedRoot = findGoRoot(proxySettings);
    final String gobin = getGoBin();
    final String gocache = getGoCache();
    final File[] gopathParts = findGoPath(true);

    if (isMojoMustNotBeExecuted()) {
      return null;
    }

    final String toolName = FilenameUtils.normalize(GetUtils.ensureNonNull(getUseGoTool(), execNameAdaptedForOs));
    final File executableFileInPathOrRoot = new File(getPathToFolder(detectedRoot) + toolName);

    final File executableFileInBin = gobin == null ? null : new File(getPathToFolder(gobin) + adaptExecNameForOS(getExec()));

    final File[] exeVariants = new File[]{executableFileInBin, executableFileInPathOrRoot};

    final File foundExecutableTool = findExisting(exeVariants);

    if (foundExecutableTool == null) {
      throw new MojoFailureException("Can't find executable file : " + Arrays.toString(exeVariants));
    } else {
      logOptionally("Executable file detected : " + foundExecutableTool);
    }

    final List<String> commandLine = new ArrayList<>();
    commandLine.add(foundExecutableTool.getAbsolutePath());

    final String gocommand = getGoCommand();
    if (!gocommand.isEmpty()) {
      commandLine.add(getGoCommand());
    }

    boolean verboseAdded = false;

    for (final String s : getCommandFlags()) {
      if (s.equals("-v")) {
        verboseAdded = true;
      }
      commandLine.add(s);
    }
    if (this.isVerbose() && !verboseAdded && isCommandSupportVerbose()) {
      commandLine.add("-v");
    }

    commandLine.addAll(Arrays.asList(getBuildFlags()));
    commandLine.addAll(Arrays.asList(getTailArguments()));
    commandLine.addAll(Arrays.asList(getOptionalExtraTailArguments()));

    final StringBuilder cli = new StringBuilder();
    int index = 0;
    for (final String s : commandLine) {
      if (cli.length() > 0) {
        cli.append(' ');
      }
      if (index == 0) {
        cli.append(execNameAdaptedForOs);
      } else {
        cli.append(s);
      }
      index++;
    }

    getLog().info(String.format("Prepared command line : %s", cli.toString()));

    final ProcessExecutor result = new ProcessExecutor(commandLine);

    final File workingDirectory = this.getWorkingDirectoryForExecutor();
    if (workingDirectory.isDirectory()) {
      logOptionally("Working directory: " + workingDirectory);
      result.directory(workingDirectory);
    } else {
      logOptionally("Working directory is not set because provided folder doesn't exist: " + workingDirectory);
    }

    logOptionally("");

    registerEnvVars(result, detectedRoot, gobin, gocache, this.getSources(this.isSourceFolderRequired()), gopathParts);

    logOptionally("........................");

    registerOutputBuffers(result);

    return result;
  }

  protected void registerOutputBuffers(@Nonnull final ProcessExecutor executor) {
    executor.redirectOutput(this.consoleOutBuffer);
    executor.redirectError(this.consoleErrBuffer);
  }

  protected void registerEnvVars(
          @Nonnull final ProcessExecutor result,
          @Nonnull final File theGoRoot,
          @Nullable final String theGoBin,
          @Nullable final String theGoCache,
          @Nonnull final File sourcesFile,
          @MustNotContainNull @Nonnull final File[] goPathParts
  ) throws IOException {
    logOptionally("....Environment vars....");

    addEnvVar(result, "GOROOT", theGoRoot.getAbsolutePath());
    this.project.getProperties().setProperty("mvn.golang.last.goroot", theGoRoot.getAbsolutePath());

    String preparedGoPath = IOUtils.makeOsFilePathWithoutDuplications(goPathParts);
    if (isEnforceGoPathToEnd()) {
      preparedGoPath = IOUtils.makeOsFilePathWithoutDuplications(makePathFromExtraGoPathElements(), removeSrcFolderAtEndIfPresented(sourcesFile.getAbsolutePath()), getSpecialPartOfGoPath(), preparedGoPath);
    } else {
      preparedGoPath = IOUtils.makeOsFilePathWithoutDuplications(preparedGoPath, makePathFromExtraGoPathElements(), removeSrcFolderAtEndIfPresented(sourcesFile.getAbsolutePath()), getSpecialPartOfGoPath());
    }
    addEnvVar(result, "GOPATH", preparedGoPath);
    this.project.getProperties().setProperty("mvn.golang.last.gopath", preparedGoPath);

    if (theGoBin == null) {
      getLog().warn("GOBIN is disabled by direct order");
    } else {
      addEnvVar(result, "GOBIN", theGoBin);
      this.project.getProperties().setProperty("mvn.golang.last.gobin", theGoBin);
    }

    if (theGoBin == null) {
      getLog().warn("GOCACHE is not provided by direct order");
    } else {
      addEnvVar(result, "GOCACHE", theGoCache);
      this.project.getProperties().setProperty("mvn.golang.last.gocache", theGoCache);
    }

    final String trgtOs = this.getTargetOS();
    final String trgtArch = this.getTargetArch();
    final String trgtArm = this.getTargetArm();
    final String trgt386 = this.getTarget386();

    if (trgt386 != null) {
      addEnvVar(result, "GO386", trgt386);
      this.project.getProperties().setProperty("mvn.golang.last.go386", trgt386);
    }

    if (trgtOs != null) {
      addEnvVar(result, "GOOS", trgtOs);
      this.project.getProperties().setProperty("mvn.golang.last.goos", trgtOs);
    }

    if (trgtArm != null) {
      addEnvVar(result, "GOARM", trgtArm);
      this.project.getProperties().setProperty("mvn.golang.last.goarm", trgtArm);
    }

    if (trgtArch != null) {
      addEnvVar(result, "GOARCH", trgtArch);
      this.project.getProperties().setProperty("mvn.golang.last.goarch", trgtArch);
    }

    final File gorootbootstrap = findGoRootBootstrap(true);
    if (gorootbootstrap != null) {
      addEnvVar(result, "GOROOT_BOOTSTRAP", gorootbootstrap.getAbsolutePath());
      this.project.getProperties().setProperty("mvn.golang.last.goroot_bootstrap", gorootbootstrap.getAbsolutePath());
    }

    String thePath = GetUtils.ensureNonNull(getEnvPath(), "");
    thePath = IOUtils.makeOsFilePathWithoutDuplications((theGoRoot + File.separator + getExecSubpath()), thePath, theGoBin);
    addEnvVar(result, "PATH", thePath);
    this.project.getProperties().setProperty("mvn.golang.last.path", thePath);

    boolean go111moduleDetected = false;

    for (final Map.Entry<?, ?> record : getEnv().entrySet()) {
      if (ENV_GO111MODULE.equals(record.getKey().toString())) {
        go111moduleDetected = true;
      }
      addEnvVar(result, record.getKey().toString(), record.getValue().toString());
    }

    if (this.isModuleMode()) {
      if (go111moduleDetected) {
        this.getLog().warn(String.format("Module mode is true but %s detected among custom environment parameters", ENV_GO111MODULE));
      } else {
        this.getLog().warn(String.format("Forcing '%s = on' because module mode is activated", ENV_GO111MODULE));
        addEnvVar(result, ENV_GO111MODULE, "on");
      }
    }
  }

  @Nonnull
  @MustNotContainNull
  protected List<File> findAllGoModsInFolder(@Nonnull @MustNotContainNull final File folder) throws IOException {
    return new ArrayList<>(FileUtils.listFiles(folder, FileFilterUtils.nameFileFilter(GO_MOD_FILE_NAME), TrueFileFilter.INSTANCE));
  }

  @Nonnull
  protected File getWorkingDirectoryForExecutor() throws IOException {
    final String forcedWorkingDirdir = this.getWorkingDir();
    if (forcedWorkingDirdir != null) {
      final File result = new File(forcedWorkingDirdir);
      if (!result.isDirectory()) {
        throw new IOException("Working directory doesn't exist: " + result);
      }
      return result;
    }

    if (this.isModuleMode()) {
      final File srcFolder = this.getSources(false);
      if (srcFolder.isDirectory()) {
        final List<File> foundGoMods = this.findAllGoModsInFolder(srcFolder);
        this.getLog().debug(String.format("Detected %d go.mod files in source folder %s", foundGoMods.size(), srcFolder));

        foundGoMods.sort(Comparator.comparing(File::toString));

        if (foundGoMods.isEmpty()) {
          this.getLog().error("Module mode is activated but there is no any go.mod file in the source folder: " + srcFolder);
          throw new IOException("Can't find any go.mod folder in the source folder: " + srcFolder);
        } else {
          final File gomodFolder = foundGoMods.get(0).getParentFile();
          this.getLog().info(String.format("Detected module folder '%s' to be used as working folder", gomodFolder));
          return gomodFolder;
        }
      } else {
        this.getLog().debug("Source folder is not found: " + srcFolder);
      }
    }

    return this.getSources(isSourceFolderRequired());
  }

  /**
   * Internal method which returns special part of GOPATH which can be formed by
   * mojos. Must be either empty or contain folders divided by file path
   * separator.
   *
   * @return special part of GOPATH, must not be null, by default must be empty
   */
  @Nonnull
  protected String getSpecialPartOfGoPath() {
    return "";
  }

  @Nonnull
  protected String makePathFromExtraGoPathElements() {
    String result = "";
    if (this.addToGoPath != null) {
      result = IOUtils.makeOsFilePathWithoutDuplications(this.addToGoPath);
    }
    return result;
  }

  public boolean isCommandSupportVerbose() {
    return false;
  }

  @Nullable
  protected String getSkipMojoPropertySuffix() {
    return null;
  }
  
  protected void processConsoleOut(final int exitCode, @Nonnull final String out, @Nonnull final String err) throws MojoFailureException, MojoExecutionException {
    final File reportsFolderFile = new File(this.getReportsFolder());

    final String fileOut = this.getOutLogFile();
    final String fileErr = this.getErrLogFile();

    final File fileToWriteOut = fileOut == null || fileOut.trim().isEmpty() ? null : new File(reportsFolderFile, fileOut);
    final File fileToWriteErr = fileErr == null || fileErr.trim().isEmpty() ? null : new File(reportsFolderFile, fileErr);

    if (fileToWriteOut != null) {
      getLog().debug("Reports folder : " + reportsFolderFile);
      if (!reportsFolderFile.isDirectory() && !reportsFolderFile.mkdirs()) {
        throw new MojoExecutionException("Can't create folder for console logs : " + reportsFolderFile);
      }
      try {
        getLog().debug("Writing out console log : " + fileToWriteErr);
        FileUtils.write(fileToWriteOut, out, "UTF-8");
      } catch (IOException ex) {
        throw new MojoExecutionException("Can't save console output log into file : " + fileToWriteOut, ex);
      }
    }

    if (fileToWriteErr != null) {
      getLog().debug("Reports folder : " + reportsFolderFile);
      if (!reportsFolderFile.isDirectory() && !reportsFolderFile.mkdirs()) {
        throw new MojoExecutionException("Can't create folder for console logs : " + reportsFolderFile);
      }
      try {
        getLog().debug("Writing error console log : " + fileToWriteErr);
        FileUtils.write(fileToWriteErr, err, "UTF-8");
      } catch (IOException ex) {
        throw new MojoExecutionException("Can't save console error log into file : " + fileToWriteErr, ex);
      }
    }
  }

}
