package eu.kalafatic.evolution.controller.mediation.analysis;

import java.util.Objects;

/**
 * Metadata record for a file included in the mediated context package.
 * Explains the file's role, category, and selection rationale.
 */
public class ManifestFileEntry {

    public enum FileCategory {
        SOURCE,
        INTERFACE,
        CONFIG,
        BUILD,
        TEST,
        DOCUMENTATION,
        MODEL,
        DATASET,
        OTHER
    }

    private String relativePath;
    private FileCategory category;
    private String selectionRationale;
    private String architecturalRole;
    private long sizeBytes;

    public ManifestFileEntry() {
    }

    public ManifestFileEntry(String relativePath, FileCategory category, String selectionRationale, String architecturalRole, long sizeBytes) {
        this.relativePath = relativePath;
        this.category = category;
        this.selectionRationale = selectionRationale;
        this.architecturalRole = architecturalRole;
        this.sizeBytes = sizeBytes;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public FileCategory getCategory() {
        return category;
    }

    public void setCategory(FileCategory category) {
        this.category = category;
    }

    public String getSelectionRationale() {
        return selectionRationale;
    }

    public void setSelectionRationale(String selectionRationale) {
        this.selectionRationale = selectionRationale;
    }

    public String getArchitecturalRole() {
        return architecturalRole;
    }

    public void setArchitecturalRole(String architecturalRole) {
        this.architecturalRole = architecturalRole;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManifestFileEntry)) return false;
        ManifestFileEntry that = (ManifestFileEntry) o;
        return Objects.equals(relativePath, that.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath);
    }
}
