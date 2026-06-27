package eu.kalafatic.evolution.controller.orchestration.intent;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one unresolved axis of intent.
 */
public class IntentDimension {
    private String dimensionId;
    private String name;
    private double confidence;
    private String inferredValue;
    private List<String> candidateValues = new ArrayList<>();
    private double ambiguityScore;
    private boolean requiresUserInput;
    private String rationale;

    public String getDimensionId() { return dimensionId; }
    public void setDimensionId(String dimensionId) { this.dimensionId = dimensionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getInferredValue() { return inferredValue; }
    public void setInferredValue(String inferredValue) { this.inferredValue = inferredValue; }

    public List<String> getCandidateValues() { return candidateValues; }
    public void setCandidateValues(List<String> candidateValues) { this.candidateValues = candidateValues; }

    public double getAmbiguityScore() { return ambiguityScore; }
    public void setAmbiguityScore(double ambiguityScore) { this.ambiguityScore = ambiguityScore; }

    public boolean isRequiresUserInput() { return requiresUserInput; }
    public void setRequiresUserInput(boolean requiresUserInput) { this.requiresUserInput = requiresUserInput; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    @Override
    public String toString() {
        return name + " (ID: " + dimensionId + ", Confidence: " + confidence + ", Ambiguity: " + ambiguityScore + ")";
    }
}
