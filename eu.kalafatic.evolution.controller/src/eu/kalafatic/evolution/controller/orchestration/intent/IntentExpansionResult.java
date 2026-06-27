package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionAxis;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;

/**
 * Container for the results of the expansion engine.
 */
public class IntentExpansionResult {
    private String originalPrompt;
    private List<IntentDimension> dimensions = new ArrayList<>();
    private List<EvolutionDimension> unresolvedDimensions = new ArrayList<>();
    private String activeDimensionId;
    private List<IntentHypothesis> hypotheses = new ArrayList<>();
    private IntentConfidence confidence;
    private String recommendedClarification;
    private List<EvolutionAxis> evolutionaryAxes = new ArrayList<>();

    // Refactored IntentResolution model
    private InterpretationState state = InterpretationState.CLEAR;
    private String dominantIntent;
    private double dominantConfidence;
    private double ambiguityScore;
    private double executionRiskScore;
    private double evolutionOpportunityScore;
    private List<String> implementationStrategies = new ArrayList<>();

    // Merged from IntentAnalysisResult
    private String language;
    private String framework;
    private String targetPlatform;
    private String expectedOutput;
    private List<String> constraints = new ArrayList<>();
    private List<MissingRequirement> missingInformation = new ArrayList<>();
    private List<Ambiguity> ambiguities = new ArrayList<>();
    private List<String> contradictions = new ArrayList<>();
    private String clarificationQuestion;

    public String getOriginalPrompt() { return originalPrompt; }
    public void setOriginalPrompt(String originalPrompt) { this.originalPrompt = originalPrompt; }

    public List<IntentDimension> getDimensions() { return dimensions; }
    public void setDimensions(List<IntentDimension> dimensions) { this.dimensions = dimensions; }

    public List<EvolutionDimension> getUnresolvedDimensions() { return unresolvedDimensions; }
    public void setUnresolvedDimensions(List<EvolutionDimension> unresolvedDimensions) { this.unresolvedDimensions = unresolvedDimensions; }

    public String getActiveDimensionId() { return activeDimensionId; }
    public void setActiveDimensionId(String activeDimensionId) { this.activeDimensionId = activeDimensionId; }

    public List<IntentHypothesis> getHypotheses() { return hypotheses; }
    public void setHypotheses(List<IntentHypothesis> hypotheses) { this.hypotheses = hypotheses; }

    public IntentConfidence getConfidence() { return confidence; }
    public void setConfidence(IntentConfidence confidence) { this.confidence = confidence; }

    public String getRecommendedClarification() { return recommendedClarification; }
    public void setRecommendedClarification(String recommendedClarification) { this.recommendedClarification = recommendedClarification; }

    public List<EvolutionAxis> getEvolutionaryAxes() { return evolutionaryAxes; }
    public void setEvolutionaryAxes(List<EvolutionAxis> evolutionaryAxes) { this.evolutionaryAxes = evolutionaryAxes; }

    public InterpretationState getState() { return state; }
    public void setState(InterpretationState state) { this.state = state; }

    public String getDominantIntent() { return dominantIntent; }
    public void setDominantIntent(String dominantIntent) { this.dominantIntent = dominantIntent; }

    public double getDominantConfidence() { return dominantConfidence; }
    public void setDominantConfidence(double dominantConfidence) { this.dominantConfidence = dominantConfidence; }

    public double getAmbiguityScore() { return ambiguityScore; }
    public void setAmbiguityScore(double ambiguityScore) { this.ambiguityScore = ambiguityScore; }

    public double getExecutionRiskScore() { return executionRiskScore; }
    public void setExecutionRiskScore(double executionRiskScore) { this.executionRiskScore = executionRiskScore; }

    public double getEvolutionOpportunityScore() { return evolutionOpportunityScore; }
    public void setEvolutionOpportunityScore(double evolutionOpportunityScore) { this.evolutionOpportunityScore = evolutionOpportunityScore; }

    public List<String> getImplementationStrategies() { return implementationStrategies; }
    public void setImplementationStrategies(List<String> implementationStrategies) { this.implementationStrategies = implementationStrategies; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }

    public String getTargetPlatform() { return targetPlatform; }
    public void setTargetPlatform(String targetPlatform) { this.targetPlatform = targetPlatform; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public List<String> getConstraints() { return constraints; }
    public void setConstraints(List<String> constraints) { this.constraints = constraints; }

    public List<MissingRequirement> getMissingInformation() { return missingInformation; }
    public void setMissingInformation(List<MissingRequirement> missingInformation) { this.missingInformation = missingInformation; }

    public List<Ambiguity> getAmbiguities() { return ambiguities; }
    public void setAmbiguities(List<Ambiguity> ambiguities) { this.ambiguities = ambiguities; }

    public List<String> getContradictions() { return contradictions; }
    public void setContradictions(List<String> contradictions) { this.contradictions = contradictions; }

    public String getClarificationQuestion() { return clarificationQuestion; }
    public void setClarificationQuestion(String question) { this.clarificationQuestion = question; }

    public boolean isAmbiguous() {
        return !ambiguities.isEmpty() || !missingInformation.isEmpty() || !contradictions.isEmpty() || state == InterpretationState.NEEDS_CLARIFICATION;
    }
}
