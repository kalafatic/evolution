# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.module/

## Domain: general

## Components
* `site.xml`: <?xml version="1.0" encoding="UTF-8"?> <site> </site>
* `pom2.xml`: <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <artifactId>eu.kalafatic.evolution.module</artifactId> <packaging>eclipse-repository</packaging> <build> <sourceDirectory>src</sourceDirectory> <extensions> <extension> <groupId>org.eclipse.tycho</groupId> <artifactId>tycho-maven-plugin</artifactId> <version>4.0.5</version> </extension> </extensions> <plugins> <plugin> <artifactId>maven-compiler-plugin</artifactId> <version>3.13.0</version> <configuration>
* `pom.xml`: <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <parent> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.aggregator</artifactId> <version>2.6.5-SNAPSHOT</version> <relativePath>../pom.xml</relativePath> </parent> <artifactId>eu.kalafatic.evolution.module</artifactId> <packaging>pom</packaging> <dependencies> <dependency> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.feature</artifactId> <version>2.6.5-SNAPSHOT</version> <type>pom</type> </dependency> </dependencies>
