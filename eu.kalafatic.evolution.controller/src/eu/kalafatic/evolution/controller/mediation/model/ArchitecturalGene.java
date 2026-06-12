package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reusable architectural gene discovered during evolution.
 * Genes are evolutionary knowledge, while files are implementation evidence.
 */
public class ArchitecturalGene {
    private String id;
    private String pattern;
    private String purpose;
    private String rationale;
    private String name;
    private List<String> requiredArtifacts = new ArrayList<>();
    private List<String> facts = new ArrayList<>();
    private List<String> evidence = new ArrayList<>(); // paths to artifacts
    private List<String> dependencies = new ArrayList<>(); // dependent genes or artifacts
    private List<String> exampleFiles = new ArrayList<>();
    private String activationConditions;
    private String transferability; // how easily this can be applied to other systems
    private int discoveryIteration;

    public ArchitecturalGene() {}

    public ArchitecturalGene(String id, String purpose) {
        this.id = id;
        this.purpose = purpose;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public List<String> getRequiredArtifacts() { return requiredArtifacts; }
    public void setRequiredArtifacts(List<String> requiredArtifacts) { this.requiredArtifacts = requiredArtifacts; }

    public List<String> getFacts() { return facts; }
    public void setFacts(List<String> facts) { this.facts = facts; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

    public List<String> getExampleFiles() { return exampleFiles; }
    public void setExampleFiles(List<String> exampleFiles) { this.exampleFiles = exampleFiles; }

    public String getActivationConditions() { return activationConditions; }
    public void setActivationConditions(String activationConditions) { this.activationConditions = activationConditions; }

    public String getTransferability() { return transferability; }
    public void setTransferability(String transferability) { this.transferability = transferability; }

    public int getDiscoveryIteration() { return discoveryIteration; }
    public void setDiscoveryIteration(int discoveryIteration) { this.discoveryIteration = discoveryIteration; }

    @Override
    public String toString() {
        return "Gene[" + id + "]: " + purpose;
    }
}
