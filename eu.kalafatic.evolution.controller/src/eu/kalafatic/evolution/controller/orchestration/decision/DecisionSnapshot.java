package eu.kalafatic.evolution.controller.orchestration.decision;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Immutable record of an activation decision made by the ActivationResolver.
 * Serves as the audit and debug artifact for every Darwin cycle.
 */
public final class DecisionSnapshot {
    private final String iterationId;
    private final String selectedVariantId;
    private final List<String> rankedVariants;
    private final Map<String, Double> aggregatedScores;
    private final List<String> criticalFailures;
    private final String activationReason;
    private final String resolverPolicy;
    private final double resolverConfidence;
    private final String recommendationSummary;
    private final long timestamp;

    public DecisionSnapshot(String iterationId, String selectedVariantId, List<String> rankedVariants,
                            Map<String, Double> aggregatedScores, List<String> criticalFailures,
                            String activationReason, String resolverPolicy, double resolverConfidence,
                            String recommendationSummary) {
        this.iterationId = iterationId;
        this.selectedVariantId = selectedVariantId;
        this.rankedVariants = rankedVariants != null ? Collections.unmodifiableList(new ArrayList<>(rankedVariants)) : Collections.emptyList();
        this.aggregatedScores = aggregatedScores != null ? Collections.unmodifiableMap(new HashMap<>(aggregatedScores)) : Collections.emptyMap();
        this.criticalFailures = criticalFailures != null ? Collections.unmodifiableList(new ArrayList<>(criticalFailures)) : Collections.emptyList();
        this.activationReason = activationReason;
        this.resolverPolicy = resolverPolicy;
        this.resolverConfidence = resolverConfidence;
        this.recommendationSummary = recommendationSummary;
        this.timestamp = System.currentTimeMillis();
    }

    public String getIterationId() { return iterationId; }
    public String getSelectedVariantId() { return selectedVariantId; }
    public List<String> getRankedVariants() { return rankedVariants; }
    public Map<String, Double> getAggregatedScores() { return aggregatedScores; }
    public List<String> getCriticalFailures() { return criticalFailures; }
    public String getActivationReason() { return activationReason; }
    public String getResolverPolicy() { return resolverPolicy; }
    public double getResolverConfidence() { return resolverConfidence; }
    public String getRecommendationSummary() { return recommendationSummary; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("DecisionSnapshot[iteration=%s, selected=%s, policy=%s, confidence=%.2f, reason=%s]",
                iterationId, selectedVariantId, resolverPolicy, resolverConfidence, activationReason);
    }
}
