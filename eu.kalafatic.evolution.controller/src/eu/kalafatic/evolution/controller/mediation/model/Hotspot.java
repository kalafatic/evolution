package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a high-influence element within the target reality.
 */
public class Hotspot {
    private String id;
    private String name;
    private String type; // e.g., "entry_point", "core_module", "bottleneck", "critical_query"
    private String description;
    private double significance;
    private List<String> evidence = new ArrayList<>();
    private List<String> relatedArtifacts = new ArrayList<>();

    public Hotspot() {}

    public Hotspot(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getSignificance() { return significance; }
    public void setSignificance(double significance) { this.significance = significance; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }

    public List<String> getRelatedArtifacts() { return relatedArtifacts; }
    public void setRelatedArtifacts(List<String> relatedArtifacts) { this.relatedArtifacts = relatedArtifacts; }
}
