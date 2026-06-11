# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.selfdev.genome/src/eu/kalafatic/evolution/selfdev/genome/core/

## Domain: general

## Components
* `ProjectSnapshot.java`: package eu.kalafatic.evolution.selfdev.genome.core; import java.util.Map; public class ProjectSnapshot { private String projectName; private String rootPath; private Map<String, String> fileContents; public String getProjectName() { return projectName; } public void setProjectName(String projectName) { this.projectName = projectName; } public String getRootPath() { return rootPath; } public void setRootPath(String rootPath) { this.rootPath = rootPath; } public Map<String, String> getFileContents() { return fileContents;
* `MediatedPackageArtifact.java`: package eu.kalafatic.evolution.selfdev.genome.core; import java.util.Map; public class MediatedPackageArtifact extends GenomeArtifact { private Map<String, String> files; @Override public ArtifactType getType() { return ArtifactType.MEDIATED_PACKAGE; } public Map<String, String> getFiles() { return files; } public void setFiles(Map<String, String> files) { this.files = files; } }
* `Mode.java`: package eu.kalafatic.evolution.selfdev.genome.core; public enum Mode { SELF_DEV, MEDIATED, SECONDHAND }
* `GenomeArtifact.java`: package eu.kalafatic.evolution.selfdev.genome.core; import java.time.Instant; import java.util.List; public abstract class GenomeArtifact { protected String id; protected String sourceProject; protected Instant timestamp; protected String topic; protected double fitness; protected List<String> tags; public abstract ArtifactType getType(); public String getId() { return id; } public void setId(String id) { this.id = id; } public String getSourceProject() { return sourceProject; }
* `ArtifactType.java`: package eu.kalafatic.evolution.selfdev.genome.core; public enum ArtifactType { DISCOVERY, MEDIATED_PACKAGE }
