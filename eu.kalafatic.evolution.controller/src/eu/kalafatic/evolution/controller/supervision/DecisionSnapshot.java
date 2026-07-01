package eu.kalafatic.evolution.controller.supervision;

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
    private final double disagreementMetric;
    private boolean explorationTriggered = false;
    private double avgShortTermFitness;
    private double avgLongTermStability;
    private long timestamp;

    @com.fasterxml.jackson.annotation.JsonCreator
    public DecisionSnapshot(@com.fasterxml.jackson.annotation.JsonProperty("iterationId") String iterationId,
                            @com.fasterxml.jackson.annotation.JsonProperty("selectedVariantId") String selectedVariantId,
                            @com.fasterxml.jackson.annotation.JsonProperty("rankedVariants") List<String> rankedVariants,
                            @com.fasterxml.jackson.annotation.JsonProperty("aggregatedScores") Map<String, Double> aggregatedScores,
                            @com.fasterxml.jackson.annotation.JsonProperty("criticalFailures") List<String> criticalFailures,
                            @com.fasterxml.jackson.annotation.JsonProperty("activationReason") String activationReason,
                            @com.fasterxml.jackson.annotation.JsonProperty("resolverPolicy") String resolverPolicy,
                            @com.fasterxml.jackson.annotation.JsonProperty("resolverConfidence") double resolverConfidence,
                            @com.fasterxml.jackson.annotation.JsonProperty("recommendationSummary") String recommendationSummary,
                            @com.fasterxml.jackson.annotation.JsonProperty("disagreementMetric") double disagreementMetric) {
        this.iterationId = iterationId;
        this.selectedVariantId = selectedVariantId;
        this.rankedVariants = rankedVariants != null ? Collections.unmodifiableList(new ArrayList<>(rankedVariants)) : Collections.emptyList();
        this.aggregatedScores = aggregatedScores != null ? Collections.unmodifiableMap(new HashMap<>(aggregatedScores)) : Collections.emptyMap();
        this.criticalFailures = criticalFailures != null ? Collections.unmodifiableList(new ArrayList<>(criticalFailures)) : Collections.emptyList();
        this.activationReason = activationReason;
        this.resolverPolicy = resolverPolicy;
        this.resolverConfidence = resolverConfidence;
        this.recommendationSummary = recommendationSummary;
        this.disagreementMetric = disagreementMetric;
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
    public double getDisagreementMetric() { return disagreementMetric; }
    public boolean isExplorationTriggered() { return explorationTriggered; }
    public double getAvgShortTermFitness() { return avgShortTermFitness; }
    public void setAvgShortTermFitness(double avgShortTermFitness) { this.avgShortTermFitness = avgShortTermFitness; }
    public double getAvgLongTermStability() { return avgLongTermStability; }
    public void setAvgLongTermStability(double avgLongTermStability) { this.avgLongTermStability = avgLongTermStability; }
    public void setExplorationTriggered(boolean explorationTriggered) { this.explorationTriggered = explorationTriggered; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public static DecisionSnapshot fromJson(org.json.JSONObject json) {
        List<String> ranked = new ArrayList<>();
        org.json.JSONArray rArr = json.optJSONArray("rankedVariants");
        if (rArr != null) for (int i=0; i<rArr.length(); i++) ranked.add(rArr.getString(i));

        List<String> fails = new ArrayList<>();
        org.json.JSONArray fArr = json.optJSONArray("criticalFailures");
        if (fArr != null) for (int i=0; i<fArr.length(); i++) fails.add(fArr.getString(i));

        Map<String, Double> scores = new HashMap<>();
        org.json.JSONObject sObj = json.optJSONObject("aggregatedScores");
        if (sObj != null) {
            for (Object key : sObj.keySet()) scores.put((String)key, sObj.getDouble((String)key));
        }

        DecisionSnapshot snap = new DecisionSnapshot(
            json.optString("iterationId"),
            json.optString("selectedVariantId"),
            ranked,
            scores,
            fails,
            json.optString("activationReason"),
            json.optString("resolverPolicy"),
            json.optDouble("resolverConfidence", 0.0),
            json.optString("recommendationSummary"),
            json.optDouble("disagreementMetric", 0.0)
        );
        snap.setExplorationTriggered(json.optBoolean("explorationTriggered"));
        snap.setAvgShortTermFitness(json.optDouble("avgShortTermFitness"));
        snap.setAvgLongTermStability(json.optDouble("avgLongTermStability"));
        snap.setTimestamp(json.optLong("timestamp", System.currentTimeMillis()));
        return snap;
    }

    @Override
    public String toString() {
        return String.format("DecisionSnapshot[iteration=%s, selected=%s, policy=%s, confidence=%.2f, reason=%s]",
                iterationId, selectedVariantId, resolverPolicy, resolverConfidence, activationReason);
    }
}
