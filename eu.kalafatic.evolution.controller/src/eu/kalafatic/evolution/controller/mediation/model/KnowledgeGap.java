package eu.kalafatic.evolution.controller.mediation.model;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an identified knowledge gap in the architectural understanding.
 * Used to drive recursive discovery and uncertainty reduction.
 */
public class KnowledgeGap {
    private String id;
    private String description;
    private GapType type;
    private double significance; // 0.0 - 1.0
    private List<String> relatedArtifacts = new ArrayList<>();
    private List<String> evidenceRequired = new ArrayList<>();

    public enum GapType {
        UNKNOWN_FACT,
        WEAK_FACT,
        CONTRADICTION,
        MISSING_EVIDENCE,
        INCOMPLETE_SUBSYSTEM,
        UNEXPLAINED_AUTHORITY
    }

    public KnowledgeGap() {}

    public KnowledgeGap(String id, String description, GapType type) {
        this.id = id;
        this.description = description;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public GapType getType() { return type; }
    public void setType(GapType type) { this.type = type; }

    public double getSignificance() { return significance; }
    public void setSignificance(double significance) { this.significance = significance; }

    public List<String> getRelatedArtifacts() { return relatedArtifacts; }
    public void setRelatedArtifacts(List<String> relatedArtifacts) { this.relatedArtifacts = relatedArtifacts; }

    public List<String> getEvidenceRequired() { return evidenceRequired; }
    public void setEvidenceRequired(List<String> evidenceRequired) { this.evidenceRequired = evidenceRequired; }

    @Override
    public String toString() {
        return "[" + type + "] " + description;
    }
}
