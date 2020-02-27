[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 8.0+](https://img.shields.io/badge/java-8.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/mvn-golang-wrapper/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|mvn-golang-wrapper|2.3.4|jar)
[![Maven 3.0.3+](https://img.shields.io/badge/maven-3.0.3%2b-green.svg)](https://maven.apache.org/)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](http://yasobe.ru/na/iamoss)

![mvn-golang](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/mvngolang.png)

# Changelog
__2.3.5 (SNAPSHOT)__
 - default version of GoSDK updated to 1.14

__2.3.4 (05-nov-2019)__
 - improved host arch detection [#70](https://github.com/raydac/mvn-golang/issues/70)
 - default version of GoSDK updated to 1.13.4
 - minor refactoring

__2.3.3 (30-jul-2019)__
 - improved work in parallel mode
 - __minimal supported JDK version increased to 1.8__
 - added `mod` mojo
 - added unified boolean properties `mvn.golang.<MOJO_NAME>.skip` to skip work of selected mojo [#65](https://github.com/raydac/mvn-golang/issues/65)
 - text `none` in `resultName` field of `build` mojo disables auto-adding of `-o` command line option  
 - minor fixes and refactoring
 - added parameter `workingDir` to replace automatically choosen working directory during tool execution.
 - added support of [Golang modules]() with maven dependencies, added [example](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-maven-module-mix) [#60](https://github.com/raydac/mvn-golang/issues/60)
 - default version of GoSDK updated to 1.12.7

__2.3.2 (24-jun-2019)__
 - updated maven archetypes
 - fixed `mvn-golang:vet does not have maven dependency resolution` [#59](https://github.com/raydac/mvn-golang/issues/59)
 - default version of GoSDK updated to 1.12.6

__2.3.1 (14-apr-2019)__
 - default version of GoSDK updated to 1.12.4
 - added parameter `goCache` to provide `GOCACHE` environment variable, the default value is `${project.build.directory}/.goBuildCache`

[full changelog](https://github.com/raydac/mvn-golang/blob/master/CHANGELOG.md)

# GO start!
__Taste Go in just two commands!__
```
mvn archetype:generate -B -DarchetypeGroupId=com.igormaznitsa -DarchetypeArtifactId=mvn-golang-hello -DarchetypeVersion=2.3.4 -DgroupId=com.go.test -DartifactId=gohello -Dversion=1.0-SNAPSHOT
mvn -f ./gohello/pom.xml package
```
The First command in th snippet above generates a maven project with some test files and the second command builds the project.
[Also you can take a look at the example `Hello world` project using the plugin](https://github.com/raydac/mvn-golang-example)

If you want to generate a multi-module project, then you can use such snippet
```
mvn archetype:generate -B -DarchetypeGroupId=com.igormaznitsa -DarchetypeArtifactId=mvn-golang-hello-multi -DarchetypeVersion=2.3.4 -DgroupId=com.go.test -DartifactId=gohello-multi -Dversion=1.0-SNAPSHOT
```

# Introduction
The Plug-in just wraps Golang tool-chain and allows to use strong maven based infrastructure to build Golang projects. It also can automatically download needed Golang SDK from the main server and tune needed version of packets for their branch, tag or revisions.
Because a Golang project in the case is formed as just maven project, it is possible to work with it in any Java IDE which supports Maven.
![mvn-golang-wrapper](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/doc_common.png)

# How it works
On start the plug-in makes below steps:
- analyzing the current platform to generate needed distributive name (it can be defined directly through properties)
- check that needed Golang SDK is already cached, if it is not cached then needed SDK will be loaded and unpacked from the main Golang SDK site
- execute needed go lang tool `bin/go` with defined command, the source folder will be set as current folder
- all folders of the project which are visible for maven (source folder, test folder, resource folders and test resource folders) are archived in ZIP file and the file is saved as a maven artifact into local maven repository (or remote maven repository). The generated ZIP artifact also contains information about dependencies which can be recognized by the plugin. In opposite to Java, Golang produces platform-dependent artifacts so that it doesn't make sense to share them through maven central but only resources and sources.  

# How to build
Because it is maven plugin, to build the plugin just use
```
mvn clean install -Pplugin
```
To save time, examples excluded from the main build process and activated through special profile
```
mvn clean install -Pexamples
```
# Important note about automatic Golang SDK load
If you have some problems with certificates during Golang SDK load, then just add `<disableSSLcheck>true</disableSSLcheck>` into plugin configuration to ignore check of certificates (also you can use property 'mvn.golang.disable.ssl.check'). Also it allows property `mvn.golang.disable.ssl.check` to disable SSL certificate check during network actions.

# How to add the plugin into maven project?
Below described build section for simple golang project which keeps source in `src` forlder and result should be placed into `bin` folder. Because it is golang project and mvn-golang plugin provides its own lifecycle, packaging for the project should be `<packaging>mvn-golang</packaging>`

```xml
<build>
    <!--Changing standard Maven project source structure to make it Go compatible-->
    <sourceDirectory>${basedir}${file.separator}src</sourceDirectory>
    <directory>${basedir}${file.separator}bin</directory>

    <plugins>
      <plugin>
        <groupId>com.igormaznitsa</groupId>
        <artifactId>mvn-golang-wrapper</artifactId>
        <version>2.3.4</version>
        <extensions>true</extensions>
        <executions>
          <execution>
            <goals>
              <goal>run</goal>
            </goals>
            <configuration>
              <packages>
                <package>main.go</package>
              </packages>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
</build>
```
# Work with dependencies

## Dependencies from Maven repository (since 2.3.0)

Plugin supports work with maven repository and can install and load dependencies from there. Artifacts generated by the plugin saved in maven respository as zip archives and `mvn-golang` must be used as `type`.
```xml
<dependency>
  <groupId>com.igormaznitsa</groupId>
  <artifactId>mvn-go-test-lib</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <type>mvn-golang</type>
</dependency>

```
Plugin makes some trick in work with Maven Golang dependencies, it downloads Golang artifacts from repository and unpack them into temporary folder, then all unpacked dependencies are added to `$GOPATH`. You can take a look at [example project which has two level dependency hierarchy](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-maven-repository).
Dependency mechanism is enabled by default for all goals which need it, but it can be off with `<scanDependencies>false</scanDependencies>`. If there is any conflicts or errors in Golang for Go modules then you can turn off Go modules with
```xml
<env>
  <GO111MODULE>off</GO111MODULE>
</env>
```
__Keep in mind that it is impossible use links to Github and Bitbucket libraries through `<dependencies>` maven mechanism, links to such dependencies should be processed by standard `GET` command and information about it you can find below.__

## Support of Module mode

Since 2.3.3 version, plug-in supports Golang module mode and allows to mix modules and work with golang maven dependencies, [I have made some sample to show the possibility](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-maven-module-mix). By default support of module mode is turned off, but it can be turned on through `<moduleMode>true</moduleMode>` or property `mvn.golang.module.mode`.

## Wrapped GET command

Plugin provides wrapper for Golang GET command and you can just define some external file contains package info through system property `mvn.golang.get.packages.file`, the file will be loaded and parsed and its definitions will be added into package depedencies.
Format of the file is very easy. Each package described on a line in format `package: <PACKAGE_NAME>[,branch: <BRANCH>][,tag: <TAG>][,revision: <REVISION>]` also it supports single line comments through `//` and directive `#include <FILE_NAME>` to load packages from some external file. Also it supports interpolation of properties defined in format `${property.name}` and provide access to maven, system and environment variables.   
Example:   
```
// example package file
#include "${basedir}/external/file.txt"
package:github.com/maruel/panicparse,tag:v1.0.2 // added because latest changes in panicparse is incompatible with termui
package:github.com/gizak/termui,branch:v2
```
This mechanism just makes work with dependencies easier and if you want to provide some specific flags and scripts to process CVS folders you have to define configuration parameters in pom.xml, pacages defined in the external file and in the pom.xml will be mixed.

The Plug-in doesn't work with standard maven dependencies and they must be defined through task of the plugin, the example of easiest case of dependencies is
```xml
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
```xml
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
```xml
<execution>
  <id>dependency1</id>
  <goals>
    <goal>get</goal>
  </goals>
  <configuration>
    <packages>
      <package>github.com/some/framework</package>
    </packages>
    <tag>1.0.1</tag>
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
By default, artifact will be installed into maven repository automatically during `install` phase if you want to turn off artifact installation then you can use standard maven properties
```xml
<properties>
    <maven.install.skip>true</maven.install.skip>
    <maven.deploy.skip>true</maven.deploy.skip>
</properties>
```
or disable plugin mojo execution
```xml
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
```xml
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

# Some Examples
- __[Protobuf use example](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-protobuf)__
- __[Maven repository dependencies example](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-maven-repository)__
 - __[Maven repository dependencies together with modules example](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-maven-module-mix)__
 - __[Versioning of dependencies](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/test-git-cvs)__
 - __["Hello world!" console application.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-helloworld)__
 - __[Console application with embedded text resource prepared with the `go-bindata` utility.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-genbindata)__
 - __[Console application with `termui` library (it needs installation of some native libraries!).](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-termui)__
 - __[NES emulator.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-nes)__
 - __[ANTLR usage.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-antlr)__
 - __[Multimodule project.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-multimodule)__
 - __[Preprocessing.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-preprocessing)__
