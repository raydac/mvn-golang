[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 6.0+](https://img.shields.io/badge/java-6.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/mvn-golang-wrapper/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|mvn-golang-wrapper|2.1.5|jar)
[![Maven 3.0.3+](https://img.shields.io/badge/maven-3.0.3%2b-green.svg)](https://maven.apache.org/)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](https://money.yandex.ru/embed/small.xml?account=41001158080699&quickpay=small&yamoney-payment-type=on&button-text=01&button-size=l&button-color=orange&targets=%D0%9F%D0%BE%D0%B6%D0%B5%D1%80%D1%82%D0%B2%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BD%D0%B0+%D0%BF%D1%80%D0%BE%D0%B5%D0%BA%D1%82%D1%8B+%D1%81+%D0%BE%D1%82%D0%BA%D1%80%D1%8B%D1%82%D1%8B%D0%BC+%D0%B8%D1%81%D1%85%D0%BE%D0%B4%D0%BD%D1%8B%D0%BC+%D0%BA%D0%BE%D0%B4%D0%BE%D0%BC&default-sum=100&successURL=)

![mvn-golang](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/mvngolang.png)

# Changelog
__2.1.5 (03-jul-2017)__
 - added archetype for multimodule project `mvn-golang-hello-multi` 
 - added `customCvsOptions` into `get` mojo to provide custom options for CVS operation.
 - improved `get` mojo, added auto-search of CVS folder in package folder hierarchy, it can be disabled with `<disableCvsAutosearch>true</disableCvsAutosearch>`, [#23](https://github.com/raydac/mvn-golang/issues/23)
 - improved `get` mojo, added way to define relative path to CVS folder in `src` folder through `<relativePathToCvsFolder>`, by default the path extracted from package name

__2.1.4 (24-jun-2017)__
 - added support of BAZAAR CVS (experimental)
 - fixed order of processing of CVS branch, tag and revision in `get` mojo
 - added `enforceDeletePackageFiles` flag into `get` mojo to enforce deletion of package sources and compiled version in local repository
 - fixed processing of `revision` for CVS

__2.1.3 (14-apr-2017)__
 - Improved `go-hello-test` archetype to generate [Intellij Idea Go plugin project structure](https://plugins.jetbrains.com/plugin/5047-go-language-golang-org-support-plugin)
 - Added flag `enforceGoPathToEnd` to enforce changing of folder list order in new generated GOPATH
 - Added list parameter `ldFlags` for `buildMojo` to define linker flags.
 - Added boolean flag `strip` for `buildMojo` to remove symbol table and DWARF from the result file.
 - Added parameter `buildMode` for `buildMojo` to define [Go build mode](https://golang.org/cmd/go/#hdr-Description_of_build_modes)

# GO start!
__Taste Go in just two commands!__
```
mvn archetype:generate -B -DarchetypeGroupId=com.igormaznitsa -DarchetypeArtifactId=mvn-golang-hello -DarchetypeVersion=2.1.4 -DgroupId=com.go.test -DartifactId=gohello -Dversion=1.0-SNAPSHOT
mvn -f ./gohello/pom.xml package
```
it will generate a maven project with extra configuration files to make the project compatible with NetBeans IDE and Intellij IDEA Golang plugin, they can be removed with `mvn clean -Pclean-ide-config`    
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

# How to build
Because it is maven plugin, to build the plugin just use
```
mvn clean install -Pplugin
```
To save time, examples excluded from the main build process and activated through special profile
```
mvn clean install -Pexamples
```

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
        <version>2.1.4</version>
        <extensions>true</extensions>
        <configuration>
          <goVersion>1.8.3</goVersion>
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

# Some Examples
 - __["Hello world!" console application.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-helloworld)__
 - __[Console application with embedded text resource prepared with the `go-bindata` utility.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-genbindata)__
 - __[Console application with `termui` library (it needs installation of some native libraries!).](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-termui)__
 - __[NES emulator.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-nes)__
 - __[ANTLR usage.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-antlr)__
 - __[Multimodule project.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-multimodule)__
 - __[Preprocessing.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-preprocessing)__
 - __[Versioning of dependencies](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/test-git-cvs)__
 
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
