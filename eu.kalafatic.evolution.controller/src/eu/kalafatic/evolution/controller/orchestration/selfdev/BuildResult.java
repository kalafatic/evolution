package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;

/**
 * Result of a build operation.
 */
public class BuildResult {
    public boolean success;
    public int buildTime; // in milliseconds
    public List<String> warnings;
    public List<String> errors;
    public String output;
    public long timestamp;
    public int exitCode;
    
    public BuildResult() {
        this.warnings = new java.util.ArrayList<>();
        this.errors = new java.util.ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    public String getErrorSummary() {
        if (errors == null || errors.isEmpty()) return "No errors";
        return String.join("; ", errors);
    }
    
    @Override
    public String toString() {
        return "BuildResult{" +
                "success=" + success +
                ", buildTime=" + buildTime +
                ", warnings=" + (warnings != null ? warnings.size() : 0) +
                ", errors=" + (errors != null ? errors.size() : 0) +
                ", exitCode=" + exitCode +
                '}';
    }
}
