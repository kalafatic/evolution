package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formal model of the discovered target reality.
 * Single source of truth for all projections.
 */
public class TargetRealityModel {
    private String domain;
    private String purpose;
    private String architectureSummary;
    private List<Hotspot> hotspots = new ArrayList<>();
    private List<Subsystem> subsystems = new ArrayList<>();
    private List<ArchitecturalFact> architecturalFacts = new ArrayList<>();
    private List<ArchitecturalGene> genes = new ArrayList<>();
    private List<KnowledgeGap> knowledgeGaps = new ArrayList<>();

    // Canonical Reconstruction Fields
    private final Map<String, Double> influenceGraph = new HashMap<>(); // Node ID -> influence score
    private final List<String> executionFlows = new ArrayList<>();
    private final List<String> decisionFlows = new ArrayList<>();
    private final List<String> selectedFiles = new ArrayList<>();
    private final List<String> impactPaths = new ArrayList<>();
    private final List<String> lessons = new ArrayList<>();
    private final List<String> patterns = new ArrayList<>();
    private final List<String> referenceImplementations = new ArrayList<>();

    private final Map<String, Object> architectureView = new HashMap<>();
    private final Map<String, Object> implementationView = new HashMap<>();
    private final Map<String, Object> genomeView = new HashMap<>();

    private double realityCompleteness; // 0.0 - 1.0
    private final Map<String, String> dimensions = new HashMap<>();
    private final List<String> objectives = new ArrayList<>();
    private final List<String> risks = new ArrayList<>();

    public TargetRealityModel() {}

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getArchitectureSummary() { return architectureSummary; }
    public void setArchitectureSummary(String architectureSummary) { this.architectureSummary = architectureSummary; }

    public List<Hotspot> getHotspots() { return hotspots; }
    public void setHotspots(List<Hotspot> hotspots) { this.hotspots = hotspots; }

    public List<Subsystem> getSubsystems() { return subsystems; }
    public void setSubsystems(List<Subsystem> subsystems) { this.subsystems = subsystems; }

    public List<ArchitecturalFact> getArchitecturalFacts() { return architecturalFacts; }
    public void setArchitecturalFacts(List<ArchitecturalFact> architecturalFacts) { this.architecturalFacts = architecturalFacts; }

    public List<ArchitecturalGene> getGenes() { return genes; }
    public void setGenes(List<ArchitecturalGene> genes) { this.genes = genes; }

    public List<KnowledgeGap> getKnowledgeGaps() { return knowledgeGaps; }
    public void setKnowledgeGaps(List<KnowledgeGap> knowledgeGaps) { this.knowledgeGaps = knowledgeGaps; }

    public Map<String, Double> getInfluenceGraph() { return influenceGraph; }
    public List<String> getExecutionFlows() { return executionFlows; }
    public List<String> getDecisionFlows() { return decisionFlows; }
    public List<String> getSelectedFiles() { return selectedFiles; }
    public List<String> getImpactPaths() { return impactPaths; }
    public List<String> getLessons() { return lessons; }
    public List<String> getPatterns() { return patterns; }
    public List<String> getReferenceImplementations() { return referenceImplementations; }

    public Map<String, Object> getArchitectureView() { return architectureView; }
    public Map<String, Object> getImplementationView() { return implementationView; }
    public Map<String, Object> getGenomeView() { return genomeView; }

    public double getRealityCompleteness() { return realityCompleteness; }
    public void setRealityCompleteness(double realityCompleteness) { this.realityCompleteness = realityCompleteness; }

    public Map<String, String> getDimensions() { return dimensions; }

    public List<String> getObjectives() { return objectives; }
    public List<String> getRisks() { return risks; }

    public void addHotspot(Hotspot hotspot) { this.hotspots.add(hotspot); }
    public void addArchitecturalFact(ArchitecturalFact fact) { this.architecturalFacts.add(fact); }
    public void addSubsystem(Subsystem subsystem) { this.subsystems.add(subsystem); }
    public void addGene(ArchitecturalGene gene) { this.genes.add(gene); }
    public void addKnowledgeGap(KnowledgeGap gap) {
        if (this.knowledgeGaps.stream().noneMatch(g -> g.getDescription().equals(gap.getDescription()))) {
            this.knowledgeGaps.add(gap);
        }
    }
}
