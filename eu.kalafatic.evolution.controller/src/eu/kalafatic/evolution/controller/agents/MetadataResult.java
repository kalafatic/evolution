package eu.kalafatic.evolution.controller.agents;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of the AI Metadata generation process.
 */
public class MetadataResult {
    private final File root;
    private final List<File> generatedFiles = new ArrayList<>();
    private final Map<String, Integer> roleStats = new HashMap<>();
    private String summary;

    public MetadataResult(File root) {
        this.root = root;
    }

    public File getRoot() {
        return root;
    }

    public List<File> getGeneratedFiles() {
        return generatedFiles;
    }

    public Map<String, Integer> getRoleStats() {
        return roleStats;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void addGeneratedFile(File file) {
        generatedFiles.add(file);
    }

    public void incrementRoleStat(String role) {
        roleStats.put(role, roleStats.getOrDefault(role, 0) + 1);
    }
}
