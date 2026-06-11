# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.quality.tests/src/eu/kalafatic/evolution/quality/tests/

## Domain: general

## Components
* `PlatformCodeQualityTest.java`: package eu.kalafatic.evolution.quality.tests; import static org.junit.Assert.*; import org.junit.Test; import java.io.File; public class PlatformCodeQualityTest { @Test public void testNamingConventions() { String packageName = this.getClass().getPackage().getName(); assertTrue("Package name should start with eu.kalafatic", packageName.startsWith("eu.kalafatic")); } @Test public void testPluginIdMatchesDirectory() { String pluginId = "eu.kalafatic.evolution.quality.tests"; assertTrue(pluginId.contains("evolution")); } }
