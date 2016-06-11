[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 6.0+](https://img.shields.io/badge/java-6.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/mvn-golang-wrapper/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|mvn-golang-wrapper|2.1.0|jar)
[![Maven 3.0.3+](https://img.shields.io/badge/maven-3.0.3%2b-green.svg)](https://maven.apache.org/)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](https://money.yandex.ru/embed/small.xml?account=41001158080699&quickpay=small&yamoney-payment-type=on&button-text=01&button-size=l&button-color=orange&targets=%D0%9F%D0%BE%D0%B6%D0%B5%D1%80%D1%82%D0%B2%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BD%D0%B0+%D0%BF%D1%80%D0%BE%D0%B5%D0%BA%D1%82%D1%8B+%D1%81+%D0%BE%D1%82%D0%BA%D1%80%D1%8B%D1%82%D1%8B%D0%BC+%D0%B8%D1%81%D1%85%D0%BE%D0%B4%D0%BD%D1%8B%D0%BC+%D0%BA%D0%BE%D0%B4%D0%BE%D0%BC&default-sum=100&successURL=)

![mvn-golang](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/mvngolang.png)

# Changelog
__2.1.1 (under development)__
- Improved `clean` mojo, added flags to clean Go path folder defined through `goPath` parameter, and added flag to delete whole `storeFolder`
- Added flag `ignoreErrorExitCode` to prevent failure for error exit code, it is useful in some test cases.
- Added parameter `reportsFolder` to define folder where some reports will be placed.
- Added parameters `outLogFile` and `errLogFile` to save execution console log as files in the report folder. By default such reports are not saved.

__2.1.0 (28-may-2016)__
- Output of environment variables has been moved under the `verbose` flag
- __Added `mvninstall` goal which saves artifact into local maven repository during `install` phase,[#2 request](https://github.com/raydac/mvn-golang/issues/2)__
- __Added support of branches and tags into `get`, it works for Git, Hg and SVN repositories__
- Improved archetype template, added example of test
- __Fixed issue [#3 "cannot run tests"](https://github.com/raydac/mvn-golang/issues/3)__

__2.0.0 (17-apr-2016)__
- __Added maven archetype `mvn-golang-hello` to generate minimal GoLang "Hello World!" project__
- Added mojo for `run` command.
- __Removed `<findExecInGoPath>` property because the logic of search executable file has been reworked__
- Added `goBin` parameter to provide $GOBIN value
- Improved CLEAN to delete also the project target folder
- The Banner is hidden by default
- __Changed project folder structure to be closer to GoLang projects__
- __Added life-cycle for packaging `mvn-golang` with support of the standard GoLang project hierarchy, as example see adapted [the Hello world example for the case](https://github.com/raydac/mvn-golang/blob/master/mvn-golang-examples/mvn-golang-example-helloworld/pom.xml)__
- Improved logging of command console output, now it is split to lines and every line logged separately
- Added support for loading of archives with Content-type `application/x-gzip`
- Increased number of test examples
- Build of example applications moved to the special profile `examples`

__1.1.0 (05-apr-2016)__
- Added [test example for `gomobile` for Android ARM 7](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-gomobile) 
- Added `<findExecInGoPath>`, it allows to find golang tool in $GOPATH instead of $GOROOT (by default `false`)
- Added `<echo>` and `<echoWarn>` to print echo messages into maven log
- Added `<exec>` parameter to define gotool name (by default `go`)
- Added `<execSubpath>` parameter to provide sub-path in SDK root to find golang tool (by default `bin`)
- Renamed parameter `<name>` to `<resultName>` and `<target>` to `<resultFolder>` for `build`
- Fixed racing issue for the maven `-T4` flag
- Fixed "Truncated TAR archive exception" for Mac OS tar.gz archive
- Removed predefined values for `<goVersion>` and `<osx>`
- Minor refactoring

__1.0.0 (26-mar-2016)__
- initial version

# Go start!
__Taste Go in just two commands!__
```
mvn archetype:generate -B -DarchetypeGroupId=com.igormaznitsa -DarchetypeArtifactId=mvn-golang-hello -DarchetypeVersion=2.1.0 -DgroupId=com.go.test -DartifactId=gohello -Dversion=1.0-SNAPSHOT
mvn -f ./gohello package
```

# Introduction
I very much like Maven build tool and use it very actively in my daily work so that I decided to develop a plug-in to provide way to automate build of GoLang applications with maven.   
The Plug-in wraps standard GoLang commands and even can download and unpack GoLang SDK from the main site.   
![mvn-golang-wrapper](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/doc_common.png)

# How it works
On start the plug-in makes below steps:
- analyzing the current platform to generate needed distributive name (it can be defined directly through properties)
- check that such distributive already cached
  - if the distributive is not cached, then it will load the distributive list from the GoLang server and will find *.zip or *.tar.gz file and download and unpack that into cache folder of the plug-in
- execute needed go lang tool `bin/go` with defined command, the source folder will be set as current folder
- since 2.1.0 version, all folders of the project which are visible for maven (source folder, test folder, resource folders and test resource folders) will be zipped and saved as artifact into local maven repository as a file with mvn-golang extension

# How to work with dependencies?

Unfortunately the plugin doesn't work with standard maven dependencies and to add dependencies into your project, just add special section among executions of the plugin
```
<execution>
  <id>default-get</id>
  <configuration>
    <buildFlags>
    <flag>-u</flag>
    </buildFlags>
    <packages>
      <package>github.com/gizak/termui</package>
    </packages>
    <branch>v2</branch>
  </configuration>
</execution>
```
if you want to have several dependencies then take a look at the example below
```
<execution>
  <id>dependency1</id>
  <goals>
    <goal>get</goal>
  </goals>
  <configuration>
    <packages>
      <package>github.com/some/framework</package>
    </packages>
    <tag>1.0.1<tag>
  </configuration>
</execution>
<execution>
  <id>dependency2</id>
  <goals>
    <goal>get</goal>
  </goals>
  <configuration>
    <packages>
      <package>github.com/some/another</package>
    </packages>
    <branch>v2</branch>
  </configuration>
</execution>
```
sometime GIT can produce cache errors and in the case you can try to turn on auto-fix of such errors with `<autofixGitCache>true</autofixGitCache>` flag.   
Since 2.1.0 version it is possible to select `branch` and `tag` for dependencies, it works with Git and potentially work with Hg and Svn.

# How to save generated artifact in repository?
The Wrapper during `install` phase collects all sources ande resources from folders defined in maven configuration and pack them as zip file, then the archive is saved in the local maven repository as new artifact with extension `mvn-golang`.   
If you want to save generated artifact then you can use snippet below
```
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-install-plugin</artifactId>
    <version>2.5.2</version>
    <executions>
      <execution>
        <id>save-result-as-artifact</id>
        <phase>install</phase>
        <goals>
          <goal>install-file</goal>
        </goals>
        <configuration>
          <file>${basedir}${file.separator}bin${file.separator}${project.build.finalName}</file>
          <groupId>${project.groupId}</groupId>
          <artifactId>${project.artifactId}-result</artifactId>
          <version>${project.version}</version>
          <!-- NB! packaging allows to select extension  -->
          <packaging>bin</packaging>
        </configuration>
      </execution>
    </executions>
</plugin>
```
if you want disable creation of artifact then you can use snippet
```
 <execution>
  <id>default-mvninstall</id>
  <phase>none</phase>
</execution>
```
# Configuration 

About configuration parameters, you can read at [the wiki page](https://github.com/raydac/mvn-golang/wiki/PluginConfigParameters).

# Testing
The Wrapper just wraps calls to Go tool and recognize the exit code, if call of `go test` is non-zero then build will be failed, it doesn't make any analysing of test reports!   
Sometime it is useful to use [GoConvey](https://github.com/smartystreets/goconvey) tool for testing, in the case use snippet below to add dependency and make testing verbose
```
<execution>
  <id>default-get</id>
  <configuration>
    <buildFlags>
      <flag>-u</flag>
    </buildFlags>
    <packages>
      <package>github.com/smartystreets/goconvey</package>
    </packages>
  </configuration>
</execution>
<execution>
  <id>default-test</id>
  <configuration>
    <buildFlags>
      <flag>-v</flag>
    </buildFlags>
  </configuration>
</execution>                    
```

# Sample use cases
 - __[Simple "Hello world!" console application.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-helloworld)__
 - __[Simple console application with embedded text resource prepared with the `go-bindata` utility.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-genbindata)__
 - __[Simple console application with `termui` library (it needs installation of some native libraries!).](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-termui)__
 - __[NES emulator.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-nes)__
 - __[Android application with `gomobile`.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-gomobile)__
