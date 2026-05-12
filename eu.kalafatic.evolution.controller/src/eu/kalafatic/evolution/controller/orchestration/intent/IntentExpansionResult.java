package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for the results of the expansion engine.
 */
public class IntentExpansionResult {
    private String originalPrompt;
    private List<IntentDimension> dimensions = new ArrayList<>();
    private List<IntentHypothesis> hypotheses = new ArrayList<>();
    private IntentConfidence confidence;
    private String recommendedClarification;

    public String getOriginalPrompt() { return originalPrompt; }
    public void setOriginalPrompt(String originalPrompt) { this.originalPrompt = originalPrompt; }

    public List<IntentDimension> getDimensions() { return dimensions; }
    public void setDimensions(List<IntentDimension> dimensions) { this.dimensions = dimensions; }

    public List<IntentHypothesis> getHypotheses() { return hypotheses; }
    public void setHypotheses(List<IntentHypothesis> hypotheses) { this.hypotheses = hypotheses; }

    public IntentConfidence getConfidence() { return confidence; }
    public void setConfidence(IntentConfidence confidence) { this.confidence = confidence; }

    public String getRecommendedClarification() { return recommendedClarification; }
    public void setRecommendedClarification(String recommendedClarification) { this.recommendedClarification = recommendedClarification; }
}
