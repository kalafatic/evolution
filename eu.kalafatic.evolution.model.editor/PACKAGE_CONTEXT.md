# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.model.editor/

## Domain: general

## Components
* `pom.xml`: <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"> <modelVersion>4.0.0</modelVersion> <parent> <groupId>eu.kalafatic.evolution</groupId> <artifactId>eu.kalafatic.evolution.aggregator</artifactId> <version>2.6.5-SNAPSHOT</version> <relativePath>../pom.xml</relativePath> </parent> <artifactId>eu.kalafatic.evolution.model.editor</artifactId> <packaging>eclipse-plugin</packaging> <build> <sourceDirectory>src</sourceDirectory> </build> </project>
* `plugin.xml`: <?xml version="1.0" encoding="UTF-8"?> <?eclipse version="3.0"?> <!-- --> <plugin> <extension point="org.eclipse.ui.newWizards"> <!-- @generated evolution --> <category id="org.eclipse.emf.ecore.Wizard.category.ID" name="%_UI_Wizard_category"/> <wizard id="eu.kalafatic.evolution.model.orchestration.presentation.OrchestrationModelWizardID" name="%_UI_OrchestrationModelWizard_label" class="eu.kalafatic.evolution.model.orchestration.presentation.OrchestrationModelWizard" category="org.eclipse.emf.ecore.Wizard.category.ID" icon="icons/evo_model.svg"> <description>%_UI_OrchestrationModelWizard_description</description> <selection class="org.eclipse.core.resources.IResource"/> </wizard> </extension>
