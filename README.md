[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 7.0+](https://img.shields.io/badge/java-7.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/mvn-golang-wrapper/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|mvn-golang-wrapper|2.2.0|jar)
[![Maven 3.0.3+](https://img.shields.io/badge/maven-3.0.3%2b-green.svg)](https://maven.apache.org/)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](http://yasobe.ru/na/iamoss)

![mvn-golang](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/mvngolang.png)

# Changelog
__2.2.1 (SNAPSHOT)__
 - added example [how to get and unpack mvn-golang artifact from maven repository](./mvn-golang-examples/mvn-golang-example-maven-repository)
 - changed result artifact extension to `zip` during artifact installation into maven repository, it allows use [maven-dependency-plugin](https://maven.apache.org/plugins/maven-dependency-plugin/)
 - added `jfrog-cli` mojo to provide way make call to external [JFrog CLI](https://www.jfrog.com/confluence/display/CLI/JFrog+CLI) in tuned Go SDK environment, [example](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-jfrog-cli).
 - added `connectionTimeout` property to provide timeout (milliseconds) for HTTP connections, default 60000 ms
 - [#55](https://github.com/raydac/mvn-golang/issues/55) print log error stream into debug if command status is not error
 - added check of hash for downloaded SDK archive, can be disabled by `false` in parameter `checkSdkHash`, it checks hash provided in response header `x-goog-hash`
 - improved GoSDK loading

__2.2.0 (13-may-2018)__
 - added property `mvn.golang.go.version` to define value for `goVersion` configuration parameter, it allows decrease configuration section dramatically, [example](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-termui/pom.xml)
 - added `externalPackageFile` (property `mvn.golang.get.packages.file`) option to the `get` mojo, it allows to keep package list in external file, [example](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-termui)
 - default value of the `useMavenProxy` flag is changed to __true__ to make the plugin more compatible with default maven process

__2.1.8 (29-apr-2018)__
 - added support of `maven.deploy.skip` and `maven.install.skip` prperties in `install` and `deploy` mojos
 - [#48](https://github.com/raydac/mvn-golang/issues/48) improved processing of `install` and `deploy` to be more compatible with standard maven process
 - fixed dependency for [termui test project in examples](./mvn-golang-examples/mvn-golang-example-termui)
 - added `customScript` section into `get` to execute some custom script over package CVS folder 

__2.1.7 (18-feb-2018)__
 - fixed target file extension in maven archetypes [#44](https://github.com/raydac/mvn-golang/issues/44)
 - added `target386` to provide value for `$GO386` environment variable
 - improved GOPATH value processing, multi-folder value allowed
 - added flag to disable SSL certificate check for HTTPS connections, `disableSSLcheck`, by default it is `false`
 - improved Golang SDK list load [#24](https://github.com/raydac/mvn-golang/issues/24)
 - added `args` attribute to the `run` mojo to provide tail command line arguments.
 - added processing of maven session offline mode
 - improved proxy server settings processing to process NTLM authorisation
 - removed maven-enforcer-plugin because it throws NPE for old maven versions

[full changelog](https://github.com/raydac/mvn-golang/blob/master/CHANGELOG.md)

# GO start!
__Taste Go in just two commands!__
```
mvn archetype:generate -B -DarchetypeGroupId=com.igormaznitsa -DarchetypeArtifactId=mvn-golang-hello -DarchetypeVersion=2.2.0 -DgroupId=com.go.test -DartifactId=gohello -Dversion=1.0-SNAPSHOT
mvn -f ./gohello/pom.xml package
```
The First command in th snippet above generates a maven project with some test files and the second command builds the project.
[Also you can take a look at the example `Hello world` project using the plugin](https://github.com/raydac/mvn-golang-example)

If you want to generate a multi-module project, then you can use such snippet
```
mvn archetype:generate -B -DarchetypeGroupId=com.igormaznitsa -DarchetypeArtifactId=mvn-golang-hello-multi -DarchetypeVersion=2.2.0 -DgroupId=com.go.test -DartifactId=gohello-multi -Dversion=1.0-SNAPSHOT
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
- since 2.1.0 version, all folders of the project which are visible for maven (source folder, test folder, resource folders and test resource folders) will be zipped and saved as artifact into local maven repository as a file with mvn-golang extension

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
If you have some problems with certificates during Golang SDK load, then just add `<disableSSLcheck>true</disableSSLcheck>` into plugin configuration to ignore check of certificates. 

# How to add the plugin into maven project?
Below described build section for simple golang project which keeps source in `src` forlder and result should be placed into `bin` folder. Because it is golang project and mvn-golang plugin provides its own lifecycle, packaging for the project should be `<packaging>mvn-golang</packaging>`

```
<build>
    <sourceDirectory>${basedir}${file.separator}src</sourceDirectory>
    <directory>${basedir}${file.separator}bin</directory>
    <plugins>
      <plugin>
        <groupId>com.igormaznitsa</groupId>
        <artifactId>mvn-golang-wrapper</artifactId>
        <version>2.2.0</version>
        <extensions>true</extensions>
        <configuration>
          <goVersion>1.10.1</goVersion>
        </configuration>
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

# How to work with dependencies?

Since 2.2.0 version you can dramatically decrease configuration section to work with dependencies, now you can just define some external file contains package info through system property `mvn.golang.get.packages.file`, the file will be loaded and parsed and its definitions will be added into package depedencies.
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
if you want to disable creation of artifact then you can use standard maven properties
```
<properties>
    <maven.install.skip>true</maven.install.skip>
    <maven.deploy.skip>true</maven.deploy.skip>
</properties>
```
or disable mojo execution
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

# Some Examples
 - __["Hello world!" console application.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-helloworld)__
 - __[Console application with embedded text resource prepared with the `go-bindata` utility.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-genbindata)__
 - __[Console application with `termui` library (it needs installation of some native libraries!).](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-termui)__
 - __[NES emulator.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-nes)__
 - __[ANTLR usage.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-antlr)__
 - __[Multimodule project.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-multimodule)__
 - __[Preprocessing.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-preprocessing)__
 - __[Versioning of dependencies](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/test-git-cvs)__

Because NetBeans IDE is very well in its processing of Maven projects and strongly supports them just out of the box, I have made [small plugin for NetBeans IDE](https://github.com/raydac/nb-mvn-golang-plugin) which makes some automation of processing maven projects with the mvn golang plugin.
