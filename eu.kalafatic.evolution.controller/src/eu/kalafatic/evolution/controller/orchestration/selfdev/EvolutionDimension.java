package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a semantic dimension in the evolutionary cognition space.
 * Evolution proceeds by resolving exactly one dimension per iteration.
 */
public class EvolutionDimension {
    private String id;
    private String description;
    private AbstractionLevel abstractionLevel;
    private SemanticDomain semanticDomain;
    private DimensionState state = DimensionState.DISCOVERED;

    private double ambiguityScore;
    private double evolutionaryPressure;
    private double branchabilityScore;
    private double significanceScore;
    private double informationGain;

    private List<String> dependencyDimensions = new ArrayList<>();
    private List<BranchVariant> candidateBranches = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonCreator
    public EvolutionDimension(@com.fasterxml.jackson.annotation.JsonProperty("id") String id,
                              @com.fasterxml.jackson.annotation.JsonProperty("description") String description,
                              @com.fasterxml.jackson.annotation.JsonProperty("abstractionLevel") AbstractionLevel level,
                              @com.fasterxml.jackson.annotation.JsonProperty("semanticDomain") SemanticDomain domain) {
        this.id = id;
        this.description = description;
        this.abstractionLevel = level;
        this.semanticDomain = domain;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AbstractionLevel getAbstractionLevel() { return abstractionLevel; }
    public void setAbstractionLevel(AbstractionLevel abstractionLevel) { this.abstractionLevel = abstractionLevel; }
    public SemanticDomain getSemanticDomain() { return semanticDomain; }
    public void setSemanticDomain(SemanticDomain semanticDomain) { this.semanticDomain = semanticDomain; }
    public DimensionState getState() { return state; }
    public void setState(DimensionState state) { this.state = state; }
    public double getAmbiguityScore() { return ambiguityScore; }
    public void setAmbiguityScore(double ambiguityScore) { this.ambiguityScore = ambiguityScore; }
    public double getEvolutionaryPressure() { return evolutionaryPressure; }
    public void setEvolutionaryPressure(double evolutionaryPressure) { this.evolutionaryPressure = evolutionaryPressure; }
    public double getBranchabilityScore() { return branchabilityScore; }
    public void setBranchabilityScore(double branchabilityScore) { this.branchabilityScore = branchabilityScore; }
    public double getSignificanceScore() { return significanceScore; }
    public void setSignificanceScore(double significanceScore) { this.significanceScore = significanceScore; }
    public double getInformationGain() { return informationGain; }
    public void setInformationGain(double informationGain) { this.informationGain = informationGain; }
    public List<String> getDependencyDimensions() { return dependencyDimensions; }
    public List<BranchVariant> getCandidateBranches() { return candidateBranches; }
}
