# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.selfdev.genome.tests/src/eu/kalafatic/evolution/selfdev/genome/test/

## Domain: general

## Components
* `GenomeHubTest.java`: package eu.kalafatic.evolution.selfdev.genome.test; import static org.junit.Assert.assertEquals; import static org.junit.Assert.assertNotNull; import static org.junit.Assert.assertTrue; import java.io.File; import java.io.FileOutputStream; import java.io.IOException; import java.nio.charset.StandardCharsets; import java.util.List; import java.util.zip.ZipEntry; import java.util.zip.ZipOutputStream; import org.junit.Test; import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact; import eu.kalafatic.evolution.selfdev.genome.core.MediatedPackageArtifact; import eu.kalafatic.evolution.selfdev.genome.core.Mode; import eu.kalafatic.evolution.selfdev.genome.core.ProjectSnapshot; import eu.kalafatic.evolution.selfdev.genome.event.GenomeEvent; import eu.kalafatic.evolution.selfdev.genome.event.GenomeEventBus; import eu.kalafatic.evolution.selfdev.genome.event.GenomeEventListener; import eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub;
