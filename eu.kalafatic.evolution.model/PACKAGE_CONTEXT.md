# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.model/

## Domain: general

## Components
* `pom.xml`: <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <parent> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.aggregator</artifactId> <version>2.6.5-SNAPSHOT</version> <relativePath>../pom.xml</relativePath> </parent> <artifactId>eu.kalafatic.evolution.model</artifactId> <packaging>eclipse-plugin</packaging> <build> <sourceDirectory>src</sourceDirectory> <plugins> <plugin> <artifactId>maven-compiler-plugin</artifactId> <version>3.13.0</version> <configuration> <release>21</release>
* `plugin.xml`: <?xml version="1.0" encoding="UTF-8"?> <?eclipse version="3.0"?> <!-- --> <plugin> <extension point="org.eclipse.emf.ecore.generated_package"> <!-- @generated evolution --> <package uri="http://eu.kalafatic.evolution/orchestration" class="eu.kalafatic.evolution.model.orchestration.OrchestrationPackage" genModel="model/evolution.genmodel"/> </extension> </plugin>
