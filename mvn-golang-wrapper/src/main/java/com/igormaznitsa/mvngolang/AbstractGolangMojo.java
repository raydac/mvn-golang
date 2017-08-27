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
import com.igormaznitsa.meta.common.utils.GetUtils;
import com.igormaznitsa.meta.common.utils.StrUtils;
import com.igormaznitsa.mvngolang.utils.ProxySettings;
import com.igormaznitsa.mvngolang.utils.UnpackUtils;
import com.igormaznitsa.mvngolang.utils.WildCardMatcher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static com.igormaznitsa.meta.common.utils.Assertions.assertNotNull;

public abstract class AbstractGolangMojo extends AbstractMojo {

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
     * set of flags to be ignored among build and extra build flags, for inside use
     */
    protected final Set<String> buildFlagsToIgnore = new HashSet<>();
    protected final List<String> tempBuildFlags = new ArrayList<>();
    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    /**
     * Use proxy server defined for maven.
     *
     * @since 2.1.1
     */
    @Parameter(name = "useMavenProxy", defaultValue = "false")
    private boolean useMavenProxy;

    /**
     * Suppose SDK archive file name if it is not presented in the list loaded from server.
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
     * Skip execution of the mojo.
     *
     * @since 2.1.2
     */
    @Parameter(name = "skip", defaultValue = "false")
    private boolean skip;

    /**
     * Ignore error exit code returned by GoLang tool and don't generate any failure.
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
     * Base site for SDK download. By default it uses <a href="https://storage.googleapis.com/golang/">https://storage.googleapis.com/golang/</a>
     */
    @Parameter(name = "sdkSite", defaultValue = "https://storage.googleapis.com/golang/")
    private String sdkSite;

    /**
     * Hide ASC banner.
     */
    @Parameter(defaultValue = "true", name = "hideBanner")
    private boolean hideBanner;

    /**
     * Folder to be used to save and unpack loaded SDKs and also keep different info. By default it has value "${user.home}${file.separator}.mvnGoLang"
     */
    @Parameter(defaultValue = "${user.home}${file.separator}.mvnGoLang", name = "storeFolder")
    private String storeFolder;

    /**
     * Folder to be used as $GOPATH. NB! By default it has value "${user.home}${file.separator}.mvnGoLang${file.separator}.go_path"
     */
    @Parameter(defaultValue = "${user.home}${file.separator}.mvnGoLang${file.separator}.go_path", name = "goPath")
    private String goPath;

    /**
     * Folder to be used as $GOARM. NB! By default it has value "${user.home}${file.separator}.mvnGoLang${file.separator}.go_arm"
     *
     * @since 2.1.1
     */
    @Parameter(name = "targetArm")
    private String targetArm;

    /**
     * Folder to be used as $GOBIN. NB! By default it has value "${project.build.directory}". It is possible to disable usage of GOBIN in process through value <b>NONE</b>
     */
    @Parameter(defaultValue = "${project.build.directory}", name = "goBin")
    private String goBin;

    /**
     * The Go SDK version. It plays role if goRoot is undefined.
     */
    @Parameter(name = "goVersion", required = true)
    private String goVersion;

    /**
     * The Go home folder. It can be undefined and in the case the plug-in will make automatic business to find SDK in its cache or download it.
     */
    @Parameter(name = "goRoot")
    private String goRoot;

    /**
     * The Go bootstrap home.
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
     * Go tool to be executed. NB! An Extension for OS will be automatically added.
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
     * The OS. If it is not defined then plug-in will try figure out the current one.
     */
    @Parameter(name = "os")
    private String os;

    /**
     * The Target architecture.
     */
    @Parameter(name = "targetArch")
    private String targetArch;

    /**
     * The Architecture. If it is not defined then plug-in will try figure out the current one.
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
    @Parameter(name = "verbose", defaultValue = "false")
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
     * Allows to find environment variable values for $GOROOT, $GOROOT_BOOTSTRAP, $GOOS, $GOARCH, $GOPATH and use them for process..
     */
    @Parameter(name = "useEnvVars", defaultValue = "false")
    private boolean useEnvVars;

    /**
     * It allows to define key value pairs which will be used as environment variables for started GoLang process.
     */
    @Parameter(name = "env")
    private Map<?, ?> env;

    /**
     * Allows directly define name of SDK archive. If it is not defined then plug-in will try to generate name and find such one in downloaded SDK list..
     */
    @Parameter(name = "sdkArchiveName")
    private String sdkArchiveName;

    /**
     * Directly defined URL to download SDK. In the case SDK list will not be downloaded and plug-in will try download archive through the link.
     */
    @Parameter(name = "sdkDownloadUrl")
    private String sdkDownloadUrl;

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

    @Nonnull
    private static String extractExtensionOfArchive(@Nonnull final String archiveName) {
        final String lcName = archiveName;
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
    private static String preparePath(@Nonnull @MayContainNull final String... paths) {
        final StringBuilder result = new StringBuilder();
        final Set<String> alreadyAdded = new HashSet<>();

        for (final String s : paths) {
            if (s != null && !s.isEmpty()) {
                if (!alreadyAdded.contains(s)) {
                    if (result.length() > 0) {
                        result.append(SystemUtils.IS_OS_WINDOWS ? ';' : ':');
                    }
                    result.append(s);
                    alreadyAdded.add(s);
                }
            }
        }
        return result.toString();
    }

    @Nonnull
    private synchronized static File loadSDKAndUnpackIntoCache(@Nonnull final AbstractGolangMojo instance, @Nullable final ProxySettings proxySettings, @Nonnull final File cacheFolder, @Nonnull final String baseSdkName, final boolean dontLoadIfNotInCache) throws IOException {
        final File sdkFolder = new File(cacheFolder, baseSdkName);

        final File lockFile = new File(cacheFolder, ".lck_load_" + baseSdkName);
        lockFile.deleteOnExit();
        try {
            if (!lockFile.createNewFile()) {
                instance.getLog().info("Detected SDK loading, waiting for the process end");
                while (lockFile.exists()) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ex) {
                        throw new IOException("Wait of SDK loading is interrupted", ex);
                    }
                }
                instance.getLog().info("Loading process has been completed");
                return sdkFolder;
            }

            if (sdkFolder.isDirectory()) {
                if (instance.verbose || instance.getLog().isDebugEnabled()) {
                    instance.getLog().info("SDK cache folder : " + sdkFolder);
                }
                return sdkFolder;
            } else if (dontLoadIfNotInCache) {
                throw new IOException("Can't find " + baseSdkName + " in the cache but loading is directly disabled");
            }

            final String predefinedLink = instance.getSdkDownloadUrl();

            final File archiveFile;
            final String linkForDownloading;

            if (isSafeEmpty(predefinedLink)) {
                instance.logOptionally("There is not any predefined SDK URL");
                final String sdkFileName = instance.findSdkArchiveFileName(proxySettings, baseSdkName);
                archiveFile = new File(cacheFolder, sdkFileName);
                linkForDownloading = instance.getSdkSite() + sdkFileName;
            } else {
                final String extension = extractExtensionOfArchive(assertNotNull(predefinedLink));
                archiveFile = new File(cacheFolder, baseSdkName + '.' + extension);
                linkForDownloading = predefinedLink;
                instance.logOptionally("Using predefined URL to download SDK : " + linkForDownloading);
                instance.logOptionally("Detected extension of archive : " + extension);
            }

            final HttpGet methodGet = new HttpGet(linkForDownloading);
            final RequestConfig config = instance.processRequestConfig(proxySettings, RequestConfig.custom()).build();
            methodGet.setConfig(config);

            boolean errorsDuringLoading = true;

            try {
                if (!archiveFile.isFile()) {
                    instance.getLog().warn("Loading SDK archive with URL : " + linkForDownloading);

                    final HttpResponse response = instance.getHttpClient(proxySettings).execute(methodGet);
                    final StatusLine statusLine = response.getStatusLine();

                    if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                        throw new IOException(String.format("Can't load SDK archive from %s : %d %s", linkForDownloading, statusLine.getStatusCode(), statusLine.getReasonPhrase()));
                    }

                    final HttpEntity entity = response.getEntity();
                    final Header contentType = entity.getContentType();

                    if (!ALLOWED_SDKARCHIVE_CONTENT_TYPE.contains(contentType.getValue())) {
                        throw new IOException("Unsupported content type : " + contentType.getValue());
                    }

                    final InputStream inStream = entity.getContent();
                    instance.getLog().info("Downloading SDK archive into file : " + archiveFile);
                    FileUtils.copyInputStreamToFile(inStream, archiveFile);

                    instance.getLog().info("Archived SDK has been succesfully downloaded, its size is " + (archiveFile.length() / 1024L) + " Kb");

                    inStream.close();
                } else {
                    instance.getLog().info("Archive file of SDK has been found in the cache : " + archiveFile);
                }

                errorsDuringLoading = false;

                return instance.unpackArchToFolder(archiveFile, "go", sdkFolder);
            } finally {
                methodGet.releaseConnection();
                if (errorsDuringLoading || !instance.isKeepSdkArchive()) {
                    instance.logOptionally("Deleting archive : " + archiveFile + (errorsDuringLoading ? " (because error during loading)" : ""));
                    deleteFileIfExists(archiveFile);
                } else {
                    instance.logOptionally("Archive file is kept for special flag : " + archiveFile);
                }
            }
        } finally {
            FileUtils.deleteQuietly(lockFile);
        }
    }

    @Nullable
    public String getGoBin() {
        final String foundInEnvironment = System.getenv("GOBIN");
        String result = assertNotNull(this.goBin);

        if ("NONE".equals(result.trim())) {
            result = null;
        } else {
            if (foundInEnvironment != null && isUseEnvVars()) {
                result = foundInEnvironment;
            }
        }
        return result;
    }

    public boolean isSkip() {
        return this.skip;
    }

    public boolean isEnforceGoPathToEnd() {
        return this.enforceGoPathToEnd;
    }

    @Nonnull
    public MavenProject getProject() {
        return this.project;
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

        for (final String b : this.tempBuildFlags) {
            result.add(b);
        }

        return result.toArray(new String[result.size()]);
    }

    @Nonnull
    @MustNotContainNull
    protected String[] getExtraBuildFlags() {
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    @Nonnull
    public File findGoPath(final boolean ensureExist) throws IOException {
        LOCKER.lock();
        try {
            final String theGoPath = getGoPath();

            if (theGoPath.contains(File.pathSeparator)) {
                getLog().error("Detected multiple folder items in the 'goPath' parameter but it must contain only folder!");
                throw new IOException("Detected multiple folder items in the 'goPath'");
            }

            final File result = new File(theGoPath);
            if (ensureExist && !result.isDirectory() && !result.mkdirs()) {
                throw new IOException("Can't create folder : " + theGoPath);
            }
            return result;
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

    @Nonnull
    public String getGoPath() {
        final String foundInEnvironment = System.getenv("GOPATH");
        String result = assertNotNull(this.goPath);

        if (foundInEnvironment != null && isUseEnvVars()) {
            result = foundInEnvironment;
        }
        return result;
    }

    @Nullable
    public String getTargetArm() {
        String result = this.targetArm;
        if (isSafeEmpty(result) && isUseEnvVars()) {
            result = System.getenv("GOARM");
        }
        return result;
    }

    @Nullable
    public String getTargetOS() {
        String result = this.targetOs;
        if (isSafeEmpty(result) && isUseEnvVars()) {
            result = System.getenv("GOOS");
        }
        return result;
    }

    @Nullable
    public String getTargetArch() {
        String result = this.targetArch;
        if (isSafeEmpty(result) && isUseEnvVars()) {
            result = System.getenv("GOARCH");
        }
        return result;
    }

    public boolean isUseMavenProxy() {
        return this.useMavenProxy;
    }

    public boolean getSupposeSdkArchiveFileName() {
        return this.supposeSdkArchiveFileName;
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

    @Nullable
    public String getGoRoot() {
        String result = this.goRoot;

        if (isSafeEmpty(result) && isUseEnvVars()) {
            result = System.getenv("GOROOT");
        }

        return result;
    }

    @Nullable
    public String getGoRootBootstrap() {
        String result = this.goRootBootstrap;

        if (isSafeEmpty(result) && isUseEnvVars()) {
            result = System.getenv("GOROOT_BOOTSTRAP");
        }

        return result;
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
            if (this.tempBuildFlags.contains(s)) continue;
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
    private synchronized HttpClient getHttpClient(@Nullable final ProxySettings proxy) {
        if (this.httpClient == null) {
            final HttpClientBuilder builder = HttpClients.custom();

            if (proxy != null) {
                if (proxy.hasCredentials()) {
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(new AuthScope(proxy.host, proxy.port), new UsernamePasswordCredentials(proxy.username, proxy.password));
                    builder.setDefaultCredentialsProvider(credentialsProvider);
                    getLog().debug(String.format("Credentials provider has been created for proxy (username : %s): %s", proxy.username, proxy.toString()));
                }

                final String[] ignoreForAddresses = proxy.nonProxyHosts == null ? new String[0] : proxy.nonProxyHosts.split("\\|");
                if (ignoreForAddresses.length > 0) {
                    final WildCardMatcher[] matchers = new WildCardMatcher[ignoreForAddresses.length];
                    for (int i = 0; i < ignoreForAddresses.length; i++) {
                        matchers[i] = new WildCardMatcher(ignoreForAddresses[i]);
                    }

                    final HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(new HttpHost(proxy.host, proxy.port, proxy.protocol)) {
                        @Override
                        @Nonnull
                        public HttpRoute determineRoute(@Nonnull final HttpHost host, @Nonnull final HttpRequest request, @Nonnull final HttpContext context) throws HttpException {
                            final String hostName = host.getHostName();
                            for (final WildCardMatcher m : matchers) {
                                if (m.match(hostName)) {
                                    getLog().debug("Ignoring proxy for host : " + hostName);
                                    return new HttpRoute(host);
                                }
                            }
                            return super.determineRoute(host, request, context);
                        }
                    };
                    builder.setRoutePlanner(routePlanner);
                    getLog().debug("Route planner tuned to ignore proxy for addresses : " + Arrays.toString(matchers));
                }
            }

            builder.setUserAgent("mvn-golang-wrapper-agent/1.0");
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
    private RequestConfig.Builder processRequestConfig(@Nullable final ProxySettings proxySettings, @Nonnull final RequestConfig.Builder config) {
        if (proxySettings != null) {
            final HttpHost proxyHost = new HttpHost(proxySettings.host, proxySettings.port, proxySettings.protocol);
            config.setProxy(proxyHost);
        }
        return config;
    }

    @Nonnull
    private String loadGoLangSdkList(@Nullable final ProxySettings proxySettings) throws IOException {
        final String sdksite = getSdkSite();

        getLog().warn("Loading list of available GoLang SDKs from " + sdksite);
        final HttpGet get = new HttpGet(sdksite);

        final RequestConfig config = processRequestConfig(proxySettings, RequestConfig.custom()).build();
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

    private void initConsoleBuffers() {
        getLog().debug("Initing console out and console err buffers");
        this.consoleErrBuffer = new ByteArrayOutputStream();
        this.consoleOutBuffer = new ByteArrayOutputStream();
    }

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
    private String findSdkArchiveFileName(@Nullable final ProxySettings proxySettings, @Nonnull final String sdkBaseName) throws IOException {
        String result = getSdkArchiveName();
        if (isSafeEmpty(result)) {
            final Document parsed = convertSdkListToDocument(loadGoLangSdkList(proxySettings));
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
    private File findGoRoot(@Nullable final ProxySettings proxySettings) throws IOException, MojoFailureException {
        final File result;
        LOCKER.lock();
        try {
            final String predefinedGoRoot = this.getGoRoot();

            if (isSafeEmpty(predefinedGoRoot)) {
                final File cacheFolder = new File(this.storeFolder);

                if (!cacheFolder.isDirectory()) {
                    logOptionally("Making SDK cache folder : " + cacheFolder);
                    if (!cacheFolder.mkdirs()) {
                        throw new IOException("Can't create folder " + cacheFolder);
                    }
                }

                final String definedOsxVersion = this.getOSXVersion();
                final String sdkVersion = this.getGoVersion();

                if (isSafeEmpty(sdkVersion)) {
                    throw new MojoFailureException("GoLang SDK version is not defined!");
                }

                final String sdkBaseName = String.format(NAME_PATTERN, sdkVersion, this.getOs(), this.getArch(), isSafeEmpty(definedOsxVersion) ? "" : "-" + definedOsxVersion);
                warnIfContainsUC("Prefer usage of lower case chars only for SDK base name", sdkBaseName);
                result = loadSDKAndUnpackIntoCache(this, proxySettings, cacheFolder, sdkBaseName, this.disableSdkLoad);
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

        while (true) {
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

            if (error || this.processConsoleOut(resultCode, outLog, errLog)) {
                printLogs(error || enforcePrintOutput(), outLog, errLog);
            }

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
            getLog().info("Execution is skipped by flag");
        } else {
            if (!isHideBanner()) {
                printBanner();
            }

            printEcho();

            final ProxySettings proxySettings = extractProxySettings();
            beforeExecution(proxySettings);

            boolean error = false;
            try {
                error = doMainBusiness(proxySettings, 10);
            } catch (IOException ex) {
                error = true;
                throw new MojoExecutionException(ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                error = true;
            } finally {
                afterExecution(null, error);
            }
        }
    }

    public void beforeExecution(@Nullable final ProxySettings proxySettings) throws MojoFailureException, MojoExecutionException {

    }

    public void afterExecution(@Nullable final ProxySettings proxySettings, final boolean error) throws MojoFailureException, MojoExecutionException {

    }

    public boolean enforcePrintOutput() {
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

    protected void printLogs(final boolean forcePrint, @Nonnull final String outLog, @Nonnull final String errLog) {
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
            getLog().error("");
            getLog().error("---------Exec.Err---------");
            for (final String str : errLog.split("\n")) {
                getLog().error(StrUtils.trimRight(str));
            }
            getLog().error("");
        } else {
            getLog().debug("There is not any log error from the process");
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

    private void addEnvVar(@Nonnull final ProcessExecutor executor, @Nonnull final String name, @Nonnull final String value) {
        logOptionally(" $" + name + " = " + value);
        executor.environment(name, value);
    }

    @Nullable
    private ProcessExecutor prepareExecutor(@Nullable final ProxySettings proxySettings) throws IOException, MojoFailureException {
        initConsoleBuffers();

        final String execNameAdaptedForOs = adaptExecNameForOS(makeExecutableFileSubpath());
        final File detectedRoot = findGoRoot(proxySettings);
        final String gobin = getGoBin();
        final File gopath = findGoPath(true);

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

        for (final String s : getCommandFlags()) {
            commandLine.add(s);
        }

        for (final String s : getBuildFlags()) {
            commandLine.add(s);
        }

        for (final String s : getTailArguments()) {
            commandLine.add(s);
        }

        for (final String s : getOptionalExtraTailArguments()) {
            commandLine.add(s);
        }

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

        final File sourcesFile = getSources(isSourceFolderRequired());
        logOptionally("GoLang project sources folder : " + sourcesFile);
        if (sourcesFile.isDirectory()) {
            result.directory(sourcesFile);
        }

        logOptionally("");
        logOptionally("....Environment vars....");

        addEnvVar(result, "GOROOT", detectedRoot.getAbsolutePath());

        if (isEnforceGoPathToEnd()) {
            addEnvVar(result, "GOPATH", preparePath(getExtraPathToAddToGoPathBeforeSources(), removeSrcFolderAtEndIfPresented(sourcesFile.getAbsolutePath()), getExtraPathToAddToGoPathToEnd(), gopath.getAbsolutePath()));
        } else {
            addEnvVar(result, "GOPATH", preparePath(gopath.getAbsolutePath(), getExtraPathToAddToGoPathBeforeSources(), removeSrcFolderAtEndIfPresented(sourcesFile.getAbsolutePath()), getExtraPathToAddToGoPathToEnd()));
        }


        if (gobin == null)
            getLog().warn("GOBIN is disabled by direct order");
        else
            addEnvVar(result, "GOBIN", gobin);


        final String trgtOs = this.getTargetOS();
        final String trgtArch = this.getTargetArch();
        final String trgtArm = this.getTargetArm();

        if (trgtOs != null) {
            addEnvVar(result, "GOOS", trgtOs);
        }

        if (trgtArm != null) {
            addEnvVar(result, "GOARM", trgtArm);
        }

        if (trgtArch != null) {
            addEnvVar(result, "GOARCH", trgtArch);
        }

        final File gorootbootstrap = findGoRootBootstrap(true);
        if (gorootbootstrap != null) {
            addEnvVar(result, "GOROOT_BOOTSTRAP", gorootbootstrap.getAbsolutePath());
        }

        String thePath = GetUtils.ensureNonNullStr(System.getenv("PATH"));
        thePath = preparePath(thePath, (detectedRoot + File.separator + getExecSubpath()), gobin);
        addEnvVar(result, "PATH", thePath);

        for (final Map.Entry<?, ?> record : getEnv().entrySet()) {
            addEnvVar(result, record.getKey().toString(), record.getValue().toString());
        }

        logOptionally("........................");

        result.redirectOutput(this.consoleOutBuffer);
        result.redirectError(this.consoleErrBuffer);

        return result;
    }

    @Nonnull
    protected String getExtraPathToAddToGoPathToEnd() {
        return "";
    }

    @Nonnull
    protected String getExtraPathToAddToGoPathBeforeSources() {
        String result = "";
        if (this.addToGoPath != null) {
            result = preparePath(this.addToGoPath);
        }
        return result;
    }

    protected boolean processConsoleOut(final int exitCode, @Nonnull final String out, @Nonnull final String err) throws MojoFailureException, MojoExecutionException {
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

        return true;
    }

}
