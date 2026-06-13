# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.repository/

## Domain: general

## Components
* `category.xml`: <?xml version="1.0" encoding="UTF-8"?> <site> <feature id="eu.kalafatic.evolution.feature" version="0.0.0"> <category name="eu.kalafatic.evolution.category"/> </feature> <category-def name="eu.kalafatic.evolution.category" label="AI Evolution"/> </site>
* `pom.xml`: <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <parent> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.aggregator</artifactId> <version>2.6.5-SNAPSHOT</version> </parent> <artifactId>eu.kalafatic.evolution.repository</artifactId> <packaging>eclipse-repository</packaging> <dependencies> <dependency> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.feature</artifactId> <version>2.6.5-SNAPSHOT</version> </dependency> </dependencies> <build> <plugins>
