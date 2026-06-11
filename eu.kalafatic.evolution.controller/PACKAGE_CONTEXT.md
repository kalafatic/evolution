# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.controller/

## Domain: general

## Components
* `pom.xml`: <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <parent> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.aggregator</artifactId> <version>2.6.5-SNAPSHOT</version> <relativePath>../pom.xml</relativePath> </parent> <artifactId>eu.kalafatic.evolution.controller</artifactId> <packaging>eclipse-plugin</packaging> <build> <sourceDirectory>src</sourceDirectory> <resources> <resource> <directory>icons</directory> <excludes> <exclude>**/*.java</exclude> </excludes>
* `plugin.xml`: <?xml version="1.0" encoding="UTF-8"?> <?eclipse version="3.4"?> <plugin> <extension point="org.eclipse.ui.commands"> <command name="Save" id="eu.kalafatic.evolution.controller.saveCommand"> </command> <command name="Maven Build" id="eu.kalafatic.evolution.controller.mavenBuildCommand"> </command> <command name="Git Commit" id="eu.kalafatic.evolution.controller.gitCommitCommand"> </command> <command name="Orchestrate" id="eu.kalafatic.evolution.controller.orchestrationCommand">
