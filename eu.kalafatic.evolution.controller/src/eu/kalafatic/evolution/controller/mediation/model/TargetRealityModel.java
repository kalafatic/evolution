package eu.kalafatic.evolution.controller.mediation.model;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
    private List<ArchitecturalUseCase> useCases = new ArrayList<>();
    private List<KnowledgeGap> knowledgeGaps = new ArrayList<>();

    // Repository Statistics
    private int filesScanned;
    private int metadataEntries;
    private int architectureNodes;
    private int architectureRelationships;
    private long analysisDurationMs;
    private String analysisWarning;

    // Canonical Reconstruction Fields
    private final Map<String, Double> influenceGraph = new HashMap<>(); // Node ID -> influence score
    private final List<String> executionFlows = new ArrayList<>();
    private final List<String> decisionFlows = new ArrayList<>();
    private final List<String> architecturalAuthorityFiles = new ArrayList<>();
    private final List<String> implementationFrontierFiles = new ArrayList<>();
    private final List<String> selectedFiles = new ArrayList<>();
    private final List<String> impactPaths = new ArrayList<>();
    private final List<String> lessons = new ArrayList<>();
    private final List<String> patterns = new ArrayList<>();
    private final List<String> referenceImplementations = new ArrayList<>();
    private final Map<String, Object> metadata = new HashMap<>();

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

    public List<ArchitecturalUseCase> getUseCases() { return useCases; }
    public void setUseCases(List<ArchitecturalUseCase> useCases) { this.useCases = useCases; }

    public List<KnowledgeGap> getKnowledgeGaps() { return knowledgeGaps; }
    public void setKnowledgeGaps(List<KnowledgeGap> knowledgeGaps) { this.knowledgeGaps = knowledgeGaps; }

    public Map<String, Double> getInfluenceGraph() { return influenceGraph; }
    public List<String> getExecutionFlows() { return executionFlows; }
    public List<String> getDecisionFlows() { return decisionFlows; }
    public List<String> getArchitecturalAuthorityFiles() { return architecturalAuthorityFiles; }
    public List<String> getImplementationFrontierFiles() { return implementationFrontierFiles; }
    public List<String> getSelectedFiles() { return selectedFiles; }
    public List<String> getImpactPaths() { return impactPaths; }
    public List<String> getLessons() { return lessons; }
    public List<String> getPatterns() { return patterns; }
    public List<String> getReferenceImplementations() { return referenceImplementations; }
    public Map<String, Object> getMetadata() { return metadata; }

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
    public void addUseCase(ArchitecturalUseCase useCase) { this.useCases.add(useCase); }

    public int getFilesScanned() { return filesScanned; }
    public void setFilesScanned(int filesScanned) { this.filesScanned = filesScanned; }

    public int getMetadataEntries() { return metadataEntries; }
    public void setMetadataEntries(int metadataEntries) { this.metadataEntries = metadataEntries; }

    public int getArchitectureNodes() { return architectureNodes; }
    public void setArchitectureNodes(int architectureNodes) { this.architectureNodes = architectureNodes; }

    public int getArchitectureRelationships() { return architectureRelationships; }
    public void setArchitectureRelationships(int architectureRelationships) { this.architectureRelationships = architectureRelationships; }

    public long getAnalysisDurationMs() { return analysisDurationMs; }
    public void setAnalysisDurationMs(long analysisDurationMs) { this.analysisDurationMs = analysisDurationMs; }

    public String getAnalysisWarning() { return analysisWarning; }
    public void setAnalysisWarning(String analysisWarning) { this.analysisWarning = analysisWarning; }

    public void addKnowledgeGap(KnowledgeGap gap) {
        if (this.knowledgeGaps.stream().noneMatch(g -> g.getDescription().equals(gap.getDescription()))) {
            this.knowledgeGaps.add(gap);
        }
    }
}
