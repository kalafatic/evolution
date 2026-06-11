# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.selfdev.genome/src/eu/kalafatic/evolution/selfdev/genome/mediation/

## Domain: general

## Components
* `MediatedPackageProcessor.java`: package eu.kalafatic.evolution.selfdev.genome.mediation; import java.io.File; import java.io.FileInputStream; import java.io.IOException; import java.nio.charset.StandardCharsets; import java.util.HashMap; import java.util.Map; import java.util.UUID; import java.util.zip.ZipEntry; import java.util.zip.ZipInputStream; import eu.kalafatic.evolution.selfdev.genome.core.MediatedPackageArtifact; public class MediatedPackageProcessor { public MediatedPackageArtifact process(File zipFile) { Map<String, String> files = new HashMap<>(); try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) { ZipEntry entry; while ((entry = zis.getNextEntry()) != null) { if (!entry.isDirectory()) { String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8); files.put(entry.getName(), content);
