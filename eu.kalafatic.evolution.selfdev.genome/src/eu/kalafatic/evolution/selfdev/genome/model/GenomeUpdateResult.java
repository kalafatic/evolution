package eu.kalafatic.evolution.selfdev.genome.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the execution results, statistics, and trace metrics for a Genome Update operation.
 */
public class GenomeUpdateResult {

    public enum GenomeUpdateStatus {
        SUCCESS,
        UNCHANGED,
        FAILED
    }

    private String projectName;
    private String branch = "main";
    private String commitHash = "UNKNOWN";
    private String commitTimestamp = "";
    private int scannedFiles;
    private int unchangedFiles;
    private int changedFiles;
    private int newFiles;
    private int removedFiles;
    private int metadataCount;
    private int discoveredModulesCount;
    private int discoveredBundlesCount;
    private int discoveredClassesCount;
    private int discoveredRelationshipsCount;
    private List<String> updatedDocuments = new ArrayList<>();
    private String historicalSnapshotPath = "";
    private String analyticalSnapshotPath = "";
    private long elapsedTimeMs;
    private boolean hasChanges;
    private boolean success = true;
    private GenomeUpdateStatus status = GenomeUpdateStatus.SUCCESS;
    private String errorMessage;
    private List<String> warnings = new ArrayList<>();

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getCommitTimestamp() { return commitTimestamp; }
    public void setCommitTimestamp(String commitTimestamp) { this.commitTimestamp = commitTimestamp; }

    public int getScannedFiles() { return scannedFiles; }
    public void setScannedFiles(int scannedFiles) { this.scannedFiles = scannedFiles; }

    public int getUnchangedFiles() { return unchangedFiles; }
    public void setUnchangedFiles(int unchangedFiles) { this.unchangedFiles = unchangedFiles; }

    public int getChangedFiles() { return changedFiles; }
    public void setChangedFiles(int changedFiles) { this.changedFiles = changedFiles; }

    public int getNewFiles() { return newFiles; }
    public void setNewFiles(int newFiles) { this.newFiles = newFiles; }

    public int getRemovedFiles() { return removedFiles; }
    public void setRemovedFiles(int removedFiles) { this.removedFiles = removedFiles; }

    public int getMetadataCount() { return metadataCount; }
    public void setMetadataCount(int metadataCount) { this.metadataCount = metadataCount; }

    public int getDiscoveredModulesCount() { return discoveredModulesCount; }
    public void setDiscoveredModulesCount(int discoveredModulesCount) { this.discoveredModulesCount = discoveredModulesCount; }

    public int getDiscoveredBundlesCount() { return discoveredBundlesCount; }
    public void setDiscoveredBundlesCount(int discoveredBundlesCount) { this.discoveredBundlesCount = discoveredBundlesCount; }

    public int getDiscoveredClassesCount() { return discoveredClassesCount; }
    public void setDiscoveredClassesCount(int discoveredClassesCount) { this.discoveredClassesCount = discoveredClassesCount; }

    public int getDiscoveredRelationshipsCount() { return discoveredRelationshipsCount; }
    public void setDiscoveredRelationshipsCount(int discoveredRelationshipsCount) { this.discoveredRelationshipsCount = discoveredRelationshipsCount; }

    public List<String> getUpdatedDocuments() { return updatedDocuments; }
    public void setUpdatedDocuments(List<String> updatedDocuments) { this.updatedDocuments = updatedDocuments; }

    public String getHistoricalSnapshotPath() { return historicalSnapshotPath; }
    public void setHistoricalSnapshotPath(String historicalSnapshotPath) { this.historicalSnapshotPath = historicalSnapshotPath; }

    public String getAnalyticalSnapshotPath() { return analyticalSnapshotPath; }
    public void setAnalyticalSnapshotPath(String analyticalSnapshotPath) { this.analyticalSnapshotPath = analyticalSnapshotPath; }

    public long getElapsedTimeMs() { return elapsedTimeMs; }
    public void setElapsedTimeMs(long elapsedTimeMs) { this.elapsedTimeMs = elapsedTimeMs; }

    public boolean isHasChanges() { return hasChanges; }
    public void setHasChanges(boolean hasChanges) { this.hasChanges = hasChanges; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) {
        this.success = success;
        if (!success) {
            this.status = GenomeUpdateStatus.FAILED;
        }
    }

    public GenomeUpdateStatus getStatus() { return status; }
    public void setStatus(GenomeUpdateStatus status) {
        this.status = status;
        if (status == GenomeUpdateStatus.FAILED) {
            this.success = false;
        }
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String toSummaryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Genome Update (Status: ").append(status).append(")\n");
        sb.append("-----------------------------------\n");
        if (commitHash != null && !commitHash.isEmpty() && !"UNKNOWN".equals(commitHash)) {
            sb.append("Branch: ").append(branch).append(" | Commit: ").append(commitHash.length() > 7 ? commitHash.substring(0, 7) : commitHash).append("\n");
        }
        sb.append("Scanned: ").append(scannedFiles).append(" files\n\n");
        sb.append("  Added: ").append(newFiles).append("\n");
        sb.append("  Modified: ").append(changedFiles).append("\n");
        sb.append("  Removed: ").append(removedFiles).append("\n");
        sb.append("  Unchanged: ").append(unchangedFiles).append("\n\n");

        if (status == GenomeUpdateStatus.UNCHANGED) {
            sb.append("Genome is already up to date. No source changes detected.\n\n");
        } else if (!updatedDocuments.isEmpty()) {
            sb.append("Genome documents updated:\n");
            for (String doc : updatedDocuments) {
                sb.append("  ✓ ").append(doc).append("\n");
            }
            sb.append("\n");
        } else if (status == GenomeUpdateStatus.FAILED) {
            sb.append("Genome Update Failed: ").append(errorMessage != null ? errorMessage : "Unknown error").append("\n\n");
        } else {
            sb.append("Genome documents updated:\n  none (no relevant changes)\n\n");
        }

        if (historicalSnapshotPath != null && !historicalSnapshotPath.isEmpty()) {
            sb.append("Historical snapshot:\n  ").append(historicalSnapshotPath).append("\n");
        }
        if (analyticalSnapshotPath != null && !analyticalSnapshotPath.isEmpty()) {
            sb.append("Analytical snapshot:\n  ").append(analyticalSnapshotPath).append("\n");
        }
        if (!warnings.isEmpty()) {
            sb.append("\nWarnings:\n");
            for (String w : warnings) {
                sb.append("  - ").append(w).append("\n");
            }
        }
        return sb.toString();
    }
}
