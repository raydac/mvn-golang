[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 6.0+](https://img.shields.io/badge/java-6.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/mvn-golang-wrapper/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|mvn-golang-wrapper|2.0.0|jar)
[![Maven 3.0.3+](https://img.shields.io/badge/maven-3.0.3%2b-green.svg)](https://maven.apache.org/)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)

![mvn-golang](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/mvngolang.png)

# Changelog
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
Taste Go on your machine in two commands!
```
mvn archetype:generate -B -DarchetypeGroupId=com.igormaznitsa -DarchetypeArtifactId=mvn-golang-hello -DarchetypeVersion=2.0.0 -DgroupId=com.go.test -DartifactId=gohello -Dversion=1.0-SNAPSHOT
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

# Configuration 

About configuration parameters, you can read at [the wiki page](https://github.com/raydac/mvn-golang/wiki/PluginConfigParameters).

# Sample use cases
 - __[Simple "Hello world!" console application.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-helloworld)__
 - __[Simple console application with embedded text resource prepared with the `go-bindata` utility.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-genbindata)__
 - __[Simple console application with `termui` library (it needs installation of some native libraries!).](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-termui)__
 - __[NES emulator.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-nes)__
 - __[Android application with `gomobile`.](https://github.com/raydac/mvn-golang/tree/master/mvn-golang-examples/mvn-golang-example-gomobile)__