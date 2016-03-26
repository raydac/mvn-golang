[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Java 6.0+](https://img.shields.io/badge/java-6.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/mvn-golang-builder/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|mvn-golang-builder|1.0.0|jar)
[![Maven 3.0.3+](https://img.shields.io/badge/maven-3.0.3%2b-green.svg)](https://maven.apache.org/)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)

![MvnGoLang](https://raw.githubusercontent.com/raydac/mvnGoLang/master/assets/mvngolang.png)

__The Plugin is under development and there is not any ready version yet!__

# Introduction
I very like Maven build tool and use it very actively in my work so that I decided to develop a small maven plugin to bring possibility to build and GoLang executable applications with the build tool.   
The Plugin is a full cycle one and allows to build a GoLang application with Maven even if there is not any installed GoLang SDK in OS, in the case the plugin will download compatible one and unpack an SDK from GoLang page and will be using the SDK in its work.   
Mainly all main commands of GoLang are accessible through the plugin and their list you can see below:
* build
* clean
* fix
* fmt
* get
* generate
* install
* test
* tool
* vet

# Example
The Example pom.xml below shows how to build a GoLang executable file from an example of [termui framework](https://github.com/gizak/termui). It will download the package automatically and as the result in the target folder will be placed the completed executable file named as 'sparklines'.
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.igormaznitsa</groupId>
    <artifactId>TestGoLangWrapper</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <target.name>sparklines</target.name>
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
          <target.name>sparklines.exe</target.name>
        </properties>
      </profile>
    </profiles>
    
    <build>
      <plugins>
        <plugin>
          <groupId>com.igormaznitsa</groupId>
          <artifactId>mvn-golang-wrapper</artifactId>
          <version>1.0.0-SNAPSHOT</version>
          <executions>
            <execution>
              <id>golang-get</id>
              <goals>
                <goal>get</goal>
              </goals>
              <configuration combine.self="override">
                <autofixGitCache>true</autofixGitCache>
                <packages>
                  <package>github.com/gizak/termui</package>
                </packages>
                <buildFlags>
                  <flag>-u</flag>
                </buildFlags>
              </configuration>
            </execution>
            <execution>
              <id>golang-build</id>
              <goals>
                <goal>build</goal>
              </goals>
              <configuration>
                <name>${target.name}</name>
              </configuration>
            </execution>
          </executions>
          <configuration>
            <goVersion>1.6</goVersion>
            <hideBanner>true</hideBanner>
            <packages>
              <file>sparklines.go</file>
            </packages>
          </configuration>
        </plugin>
      </plugins>
    </build>
    
</project>
```

# Configuration 

About configuration parameters, you can read at [the wiki page](https://github.com/raydac/mvnGoLang/wiki/PluginConfigParameters).