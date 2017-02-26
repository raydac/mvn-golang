[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 6.0+](https://img.shields.io/badge/java-6.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/mvn-golang-wrapper/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|mvn-golang-wrapper|2.1.2|jar)
[![Maven 3.0.3+](https://img.shields.io/badge/maven-3.0.3%2b-green.svg)](https://maven.apache.org/)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](https://money.yandex.ru/embed/small.xml?account=41001158080699&quickpay=small&yamoney-payment-type=on&button-text=01&button-size=l&button-color=orange&targets=%D0%9F%D0%BE%D0%B6%D0%B5%D1%80%D1%82%D0%B2%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BD%D0%B0+%D0%BF%D1%80%D0%BE%D0%B5%D0%BA%D1%82%D1%8B+%D1%81+%D0%BE%D1%82%D0%BA%D1%80%D1%8B%D1%82%D1%8B%D0%BC+%D0%B8%D1%81%D1%85%D0%BE%D0%B4%D0%BD%D1%8B%D0%BC+%D0%BA%D0%BE%D0%B4%D0%BE%D0%BC&default-sum=100&successURL=)

![mvn-golang](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/mvngolang.png)

# Changelog
__2.1.3-SNAPSHOT (under development)__
 - Added parameter `buildMode` for `buildMojo` to define [Go build mode](https://golang.org/cmd/go/#hdr-Description_of_build_modes)

__2.1.2 (07-nov-2016)__
- Added `skip` attribute to skip execution of mojo
- [#10](https://github.com/raydac/mvn-golang/issues/10), Added way to disable providing of $GOBIN through pseudo-path __NONE__
- Changed maven phase for build from `compile` to `package` (to prevent build start before tests)
- Enforced console output for `test` even in non-verbose mode
- Added default packages `./...` for `fmt`,`vet`,`fix` and `test` tasks
- Added `maven.test.failure.ignore` and `test` properties processing into `test` goal, also allowed method regex template after `#` like in surefire

__2.1.1 (21-aug-2016)__
- [#9](https://github.com/raydac/mvn-golang/issues/9), Added attribute `targetArm` to provide $GOARM value
- Added support of proxy server [#8](https://github.com/raydac/mvn-golang/issues/8), added flag `useMavenProxy` to use proxy server defined either through maven settings.xml file or the `proxy` configuration section of the plugin.
- Improved `clean` mojo, added flags to clean Go path folder defined through `goPath` parameter, and added flag to delete whole `storeFolder`
- Added flag `ignoreErrorExitCode` to prevent failure for error exit code, it is useful in some test cases.
- Added parameter `reportsFolder` to define folder where some reports will be placed.
- Added parameters `outLogFile` and `errLogFile` to save execution console log as files in the report folder. By default such reports are not saved.
- Console log for `test` will be shown in maven log only in verbose mode 

__2.1.0 (28-may-2016)__
- Output of environment variables has been moved under the `verbose` flag
- __Added `mvninstall` goal which saves artifact into local maven repository during `install` phase,[#2 request](https://github.com/raydac/mvn-golang/issues/2)__
- __Added support of branches and tags into `get`, it works for Git, Hg and SVN repositories__
- Improved archetype template, added example of test
- __Fixed issue [#3 "cannot run tests"](https://github.com/raydac/mvn-golang/issues/3)__


# GO start!
__Taste Go in just two commands!__
```
mvn archetype:generate -B -DarchetypeGroupId=com.igormaznitsa -DarchetypeArtifactId=mvn-golang-hello -DarchetypeVersion=2.1.2 -DgroupId=com.go.test -DartifactId=gohello -Dversion=1.0-SNAPSHOT
mvn -f ./gohello/pom.xml package
```
[Also you can take a look at the example `Hello world` project using the plugin](https://github.com/raydac/mvn-golang-example)

# Introduction
The Plug-in just wraps Golang tool-chain and allows to use strong maven based infrastructure to build Golang projects. It also can automatically download needed Golang SDK from the main server and tune needed version of packets for their branch, tag or revisions.
Because a Golang project in the case is formed as just maven project, it is possible to work with it in any Java IDE which supports Maven.
![mvn-golang-wrapper](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/doc_common.png)

# How it works
On start the plug-in makes below steps:
- analyzing the current platform to generate needed distributive name (it can be defined directly through properties)
- check that needed Golang SDK is already cached, if it is not cached then needed SDK will be loaded and unpacked from the main Golang SDK site
- execute needed go lang tool `bin/go` with defined command, the source folder will be set as current folder
- since 2.1.0 version, all folders of the project which are visible for maven (source folder, test folder, resource folders and test resource folders) will be zipped and saved as artifact into local maven repository as a file with mvn-golang extension

# How to work with dependencies?

The Plug-in doesn't work with standard maven dependencies andf they must be defined through task of the plugin, the example of easiest case of dependencies is
```
<execution>
   <id>default-get</id>
   <configuration>
     <packages>
       <package>github.com/gizak/termui</package>
       <package>github.com/kataras/iris</package>
     </packages>
   </configuration>
</execution> 
```
it will be executed as `bin/go get github.com/gizak/termui github.com/kataras/iris` 

If you want work with specific branch then use below snipet
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
if you want to have several dependencies with different tag and branch then take a look at the snipet below
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
 
# Public snapshot repository for the plugin
To make accessible the snapshot version of the plugin during development, I have tuned public maven snapshot repository which can be added into project with snippet
```xml
<repositories>
 <repository>
  <id>coldcore.ru-snapshots</id>
  <name>ColdCore.RU Mvn Snapshots</name>
  <url>http://coldcore.ru/m2</url>
  <snapshots>
   <enabled>true</enabled>
  </snapshots>
  <releases>
   <enabled>false</enabled>
  </releases>
 </repository>
</repositories>
```
