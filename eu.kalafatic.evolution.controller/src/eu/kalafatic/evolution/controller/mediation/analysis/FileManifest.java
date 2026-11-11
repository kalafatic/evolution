package eu.kalafatic.evolution.controller.mediation.analysis;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * File manifest containing metadata and selection rationales for all files
 * bundled into the mediated package.
 */
public class FileManifest {

    private String targetRoot;
    private String generatedAt;
    private int totalFiles;
    private long totalBytes;
    private List<ManifestFileEntry> files = new ArrayList<>();

    public FileManifest() {
        this.generatedAt = java.time.Instant.now().toString();
    }

    public FileManifest(String targetRoot) {
        this.targetRoot = targetRoot;
        this.generatedAt = java.time.Instant.now().toString();
    }

    public String getTargetRoot() {
        return targetRoot;
    }

    public void setTargetRoot(String targetRoot) {
        this.targetRoot = targetRoot;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalFiles() {
        return files.size();
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalBytes() {
        return files.stream().mapToLong(ManifestFileEntry::getSizeBytes).sum();
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public List<ManifestFileEntry> getFiles() {
        return files;
    }

    public void setFiles(List<ManifestFileEntry> files) {
        this.files = files;
    }

    public void addEntry(ManifestFileEntry entry) {
        if (entry != null && !files.contains(entry)) {
            files.add(entry);
        }
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Included Project Context File Manifest\n\n");
        sb.append("Total Files: ").append(getTotalFiles()).append(" | Total Size: ").append(getTotalBytes()).append(" bytes\n\n");
        sb.append("| Relative Path | Category | Role | Selection Rationale |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        for (ManifestFileEntry entry : files) {
            sb.append("| `").append(entry.getRelativePath()).append("` ")
              .append("| ").append(entry.getCategory()).append(" ")
              .append("| ").append(entry.getArchitecturalRole() != null ? entry.getArchitecturalRole() : "N/A").append(" ")
              .append("| ").append(entry.getSelectionRationale() != null ? entry.getSelectionRationale() : "Identified relevant context").append(" |\n");
        }
        return sb.toString();
    }
}
