package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * A semantic representation of a file or logical unit in the target.
 */
public class SemanticNode {
    private  String id;
    private  String path;
    private String type;
    private String summary;
    private List<String> tags = new ArrayList<>();
    private double architecturalAuthority;
    private double evolutionaryInfluenceScore;
    private List<String> breakImpacts = new ArrayList<>();
    private List<String> evolutionPotentials = new ArrayList<>();
    private int discoveryDepth;

    // Extracted structure: functions, classes, sections, etc.
    private final List<String> structures = new ArrayList<>();

    // Key-value metadata (e.g., config values, specific attributes)
    private final Map<String, String> attributes = new HashMap<>();

    // Dependency references (imports, includes)
    private final List<String> dependencies = new ArrayList<>();
    
    public SemanticNode() {
    }

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

    public double getEvolutionaryInfluenceScore() { return evolutionaryInfluenceScore; }
    public void setEvolutionaryInfluenceScore(double evolutionaryInfluenceScore) { this.evolutionaryInfluenceScore = evolutionaryInfluenceScore; }

    public List<String> getBreakImpacts() { return breakImpacts; }
    public void setBreakImpacts(List<String> breakImpacts) { this.breakImpacts = breakImpacts; }

    public List<String> getEvolutionPotentials() { return evolutionPotentials; }
    public void setEvolutionPotentials(List<String> evolutionPotentials) { this.evolutionPotentials = evolutionPotentials; }

    public void addBreakImpact(String impact) { this.breakImpacts.add(impact); }
    public void addEvolutionPotential(String potential) { this.evolutionPotentials.add(potential); }

    public int getDiscoveryDepth() { return discoveryDepth; }
    public void setDiscoveryDepth(int discoveryDepth) { this.discoveryDepth = discoveryDepth; }
    public List<String> getStructures() { return structures; }
    public Map<String, String> getAttributes() { return attributes; }
    public List<String> getDependencies() { return dependencies; }
}
