# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.feature/

## Domain: general

## Components
* `pom.xml`: <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <parent> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.aggregator</artifactId> <version>2.6.5-SNAPSHOT</version> <relativePath>../pom.xml</relativePath> </parent> <artifactId>eu.kalafatic.evolution.feature</artifactId> <packaging>eclipse-feature</packaging> <build> <sourceDirectory>src</sourceDirectory> <resources> <resource> <directory>icons</directory> <excludes> <exclude>**/*.java</exclude> </excludes>
* `feature.xml`: <?xml version="1.0" encoding="UTF-8"?> <feature id="eu.kalafatic.evolution.feature" label="Feature" version="2.6.5.qualifier"> <description url="http://www.example.com/description"> [Enter Feature Description here.] </description> <copyright url="http://www.example.com/copyright"> [Enter Copyright Description here.] </copyright> <license url="http://www.example.com/license"> [Enter License Description here.] </license> <plugin id="eu.kalafatic.evolution.view" version="0.0.0"/> <plugin id="eu.kalafatic.evolution.controller" version="0.0.0"/>
