package eu.kalafatic.evolution.controller.mediation.model;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reusable architectural fact discovered during evolution.
 */
public class ArchitecturalFact {
    private String id;
    private String subject; // e.g., "Component A"
    private String predicate; // e.g., "coordinates execution"
    private String description;
    private double confidence; // 0.0 - 1.0
    private List<String> evidence = new ArrayList<>(); // paths to files providing evidence
    private List<String> uncertainty = new ArrayList<>();
    private String rationale;
    private int discoveryIteration;

    public ArchitecturalFact() {}

    public ArchitecturalFact(String id, String subject, String predicate) {
        this.id = id;
        this.subject = subject;
        this.predicate = predicate;
    }

    public ArchitecturalFact(String id, String subject, String predicate, double confidence) {
        this.id = id;
        this.subject = subject;
        this.predicate = predicate;
        this.confidence = confidence;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getPredicate() { return predicate; }
    public void setPredicate(String predicate) { this.predicate = predicate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }

    public List<String> getUncertainty() { return uncertainty; }
    public void setUncertainty(List<String> uncertainty) { this.uncertainty = uncertainty; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public int getDiscoveryIteration() { return discoveryIteration; }
    public void setDiscoveryIteration(int discoveryIteration) { this.discoveryIteration = discoveryIteration; }

    @Override
    public String toString() {
        return subject + " " + predicate + (description != null ? " (" + description + ")" : "");
    }
}
