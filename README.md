[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 6.0+](https://img.shields.io/badge/java-6.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/mvn-golang-wrapper/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|mvn-golang-wrapper|1.1.0|jar)
[![Maven 3.0.3+](https://img.shields.io/badge/maven-3.0.3%2b-green.svg)](https://maven.apache.org/)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)

![mvn-golang](https://raw.githubusercontent.com/raydac/mvn-golang/master/assets/mvngolang.png)

# Changelog
__2.0.0-SNAPSHOT__
- Added maven archetype `mvn-golang-hello` to generate minimal GoLang "Hello World!" project
- Added mojo for `run` command.
- __Removed `<findExecInGoPath>` property because the logic of search executable file has been reworked__
- Added `goBin` parameter to provide $GOBIN value
- Improved CLEAN to delete also the project target folder
- The Banner is hidden by default
- __Changed project folder structure to be closer to GoLang projects__
- __Added life-cycle for packaging `mvn-golang` with support of the standard GoLang project hierarchy, as example see adapted [the Hello world example for the case](https://github.com/raydac/mvn-golang/blob/master/mvn-golang-examples/mvn-golang-example-helloworld/pom.xml)__
- Improved logging of command console output, now it is split to lines and every line logged separately
- Added support for loading of archives with Content-type `application/x-gzip`
- Added pair of test examples
- Build of example application moved to the special profile `examples`

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

# Introduction
I very much like Maven build tool and use it very actively in my work so that I decided to develop a small maven plug-in to provide way to build GoLang executable modules with the build tool.   
The Plug-in is a full cycle one and allows to build a GoLang application with Maven even if there is not any installed GoLang SDK in OS, in the case the plug-in will download compatible one and unpack an SDK from GoLang page and will be using the SDK in its work.   
Mainly all main commands of GoLang are accessible through the plug-in and their list you can see below:
* __clean__ (by default works in maven _CLEAN_ phase)
* __fix__ (by default works in maven _VALIDATE_ phase)
* __generate__ (by default works in maven _GENERATE_SOURCES_ phase)
* __fmt__ (by default works in maven _PROCESS_SOURCES_ phase)
* __get__ (by default works in maven _GENERATE_RESOURCES_ phase)
* __test__ (by default works in maven _TEST_ phase)
* __build__ (by default works in maven _PACKAGE_ phase)
* __tool__ (by default works in maven _VERIFY_ phase)
* __vet__ (by default works in maven _VERIFY_ phase)
* __install__ (by default works in maven _INSTALL_ phase)

# How it works
On start the plug-in makes such steps:
- analyzing the current platform to generate needed distributive name
- check that such distributive already cached
  - if the distributive is not cached, then it will load the distributive list from the GoLang server and will find *.zip or *.tar.gz file and download and unpack that into cache folder of the plug-in
- execute the go lang tool `bin/go` with needed command over the sources folder

# What to do if I want to use already installed SDK?
In the case just define `<goRoot>` in plug-in configuration
```
<goRoot>some/folder/where/go</goRoot>
````
and plug-in will be using already installed distributive

# I want to use values of already defined environment variables!
By defaut the plug-in using only parameters defined in its configuration, if you want to enable import of environment parameters $GOROOT, $GOOS, $GOARCH and $GOROOT_BOOTSTRAP (if they defined), then just add flag `<useEnvVars>`into plug-in configuration
```
<useEnvVars>true</useEnvVars>
```

# Example
To Check the plugin fastly you can clone [the example "Hello World" project](https://github.com/raydac/mvn-golang-example). The Example will be working for Linux and Windows just out of the box but if you use Mac OS then you should have installed GoLang SDK and define `<goRoot>`.      
The Part pom.xml below shows how to build 'Hello world' application with the plug-in. GoLang sources should be placed in `${basedir}/src/golang` folder.
```
  <properties>
    <target.name>helloworld</target.name>
  </properties>

  <profiles>
    <profile> 
      <id>windows</id>
      <activation>
        <os>
          <family>windows</family>
        </os>
      </activation>
      <properties>
        <target.name>helloworld.exe</target.name>
      </properties>
    </profile>
  </profiles>

  <build>
    <plugins>
      <plugin>
        <groupId>com.igormaznitsa</groupId>
        <artifactId>mvn-golang-wrapper</artifactId>
        <version>1.1.0</version>
        <executions>
          <execution>
            <id>golang-build</id>
            <goals>
              <goal>build</goal>
            </goals>
            <configuration>
              <resultName>${target.name}</resultName>
            </configuration>
          </execution>
        </executions>
        <configuration>
          <goVersion>1.6</goVersion>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

# Configuration 

About configuration parameters, you can read at [the wiki page](https://github.com/raydac/mvn-golang/wiki/PluginConfigParameters).


# How to execute command non-covered by the plug-in?
The Plug-in covers only the main set of commands but there are another commands for Go tool and also in future their number will be increased. To cover such cases I have added the `custom` mojo into the plug-in.
```
<build>
    <plugins>
      <plugin>
        <groupId>com.igormaznitsa</groupId>
        <artifactId>mvn-golang-wrapper</artifactId>
        <version>1.1.0</version>
        <executions>
          <execution>
            <id>golang-build</id>
            <goals>
              <goal>custom</goal>
            </goals>
            <configuration>
              <customCommand>run</customCommand>
            </configuration>
          </execution>
        </executions>
        <configuration>
          <goVersion>1.6</goVersion>
        </configuration>
      </plugin>
    </plugins>
  </build>
```
