package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A semantic representation of a file or logical unit in the target.
 */
public class SemanticNode {
    private final String id;
    private final String path;
    private final String type;
    private String summary;
    private final List<String> tags = new ArrayList<>();
    private double architecturalAuthority;
    private int discoveryDepth;

    // Extracted structure: functions, classes, sections, etc.
    private final List<String> structures = new ArrayList<>();

    // Key-value metadata (e.g., config values, specific attributes)
    private final Map<String, String> attributes = new HashMap<>();

    // Dependency references (imports, includes)
    private final List<String> dependencies = new ArrayList<>();

    public SemanticNode(String id, String path, String type) {
        this.id = id;
        this.path = path;
        this.type = type;
    }

    public String getId() { return id; }
    public String getPath() { return path; }
    public String getType() { return type; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getTags() { return tags; }

    public double getArchitecturalAuthority() { return architecturalAuthority; }
    public void setArchitecturalAuthority(double architecturalAuthority) { this.architecturalAuthority = architecturalAuthority; }

    public int getDiscoveryDepth() { return discoveryDepth; }
    public void setDiscoveryDepth(int discoveryDepth) { this.discoveryDepth = discoveryDepth; }
    public List<String> getStructures() { return structures; }
    public Map<String, String> getAttributes() { return attributes; }
    public List<String> getDependencies() { return dependencies; }
}
