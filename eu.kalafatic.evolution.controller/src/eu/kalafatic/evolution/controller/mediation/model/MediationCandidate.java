package eu.kalafatic.evolution.controller.mediation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete proposal for a mediation package.
 * This is the primary evolutionary unit in Mediated Mode.
 */
public class MediationCandidate {
    private String prompt;
    private List<String> selectedFiles = new ArrayList<>();
    private String architectureSummary;
    private List<ArchitecturalFact> architecturalFacts = new ArrayList<>();
    private List<Subsystem> subsystems = new ArrayList<>();
    private List<ArchitecturalGene> genes = new ArrayList<>();
    private List<KnowledgeGap> knowledgeGaps = new ArrayList<>();
    private String dependencies;
    private String executionInstructions;
    private Map<String, Object> metadata = new HashMap<>();
    private String evaluation;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<String> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<String> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

    public String getArchitectureSummary() {
        return architectureSummary;
    }

    public void setArchitectureSummary(String architectureSummary) {
        this.architectureSummary = architectureSummary;
    }

    public List<ArchitecturalFact> getArchitecturalFacts() {
        return architecturalFacts;
    }

    public void setArchitecturalFacts(List<ArchitecturalFact> architecturalFacts) {
        this.architecturalFacts = architecturalFacts;
    }

    public List<Subsystem> getSubsystems() {
        return subsystems;
    }

    public void setSubsystems(List<Subsystem> subsystems) {
        this.subsystems = subsystems;
    }

    public List<ArchitecturalGene> getGenes() {
        return genes;
    }

    public void setGenes(List<ArchitecturalGene> genes) {
        this.genes = genes;
    }

    public List<KnowledgeGap> getKnowledgeGaps() {
        return knowledgeGaps;
    }

    public void setKnowledgeGaps(List<KnowledgeGap> knowledgeGaps) {
        this.knowledgeGaps = knowledgeGaps;
    }

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public String getExecutionInstructions() {
        return executionInstructions;
    }

    public void setExecutionInstructions(String executionInstructions) {
        this.executionInstructions = executionInstructions;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }
}
