# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.model.edit/

## Domain: general

## Components
* `pom.xml`: <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <parent> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.aggregator</artifactId> <version>2.6.5-SNAPSHOT</version> <relativePath>../pom.xml</relativePath> </parent> <artifactId>eu.kalafatic.evolution.model.edit</artifactId> <packaging>eclipse-plugin</packaging> <build> <sourceDirectory>src</sourceDirectory> </build> </project>
* `plugin.xml`: <?xml version="1.0" encoding="UTF-8"?> <?eclipse version="3.0"?> <!-- --> <plugin> <extension point="org.eclipse.emf.edit.itemProviderAdapterFactories"> <!-- @generated evolution --> <factory uri="http://eu.kalafatic.evolution/orchestration" class="eu.kalafatic.evolution.model.orchestration.provider.OrchestrationItemProviderAdapterFactory" supportedTypes= "org.eclipse.emf.edit.provider.IEditingDomainItemProvider org.eclipse.emf.edit.provider.IStructuredItemContentProvider org.eclipse.emf.edit.provider.ITreeItemContentProvider org.eclipse.emf.edit.provider.IItemLabelProvider org.eclipse.emf.edit.provider.IItemPropertySource"/> </extension> </plugin>
