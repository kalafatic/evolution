package eu.kalafatic.evolution.controller.trajectory;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Multi-dimensional fitness evaluation for evolutionary branches.
 * Provides deterministic normalized scoring based on various architectural and behavioral signals.
 */
public final class FitnessEvaluation {
    private final String variantId;
    private final Map<String, Double> dimensions = new HashMap<>();
    private final Map<String, Double> weights = new HashMap<>();

    public FitnessEvaluation(String variantId) {
        this.variantId = variantId;
        initializeDefaultWeights();
    }

    private void initializeDefaultWeights() {
        weights.put("test_success", 0.30);
        weights.put("compilation_success", 0.20);
        weights.put("architecture_stability", 0.15);
        weights.put("semantic_alignment", 0.15);
        weights.put("patch_quality", 0.05);
        weights.put("complexity_penalty", 0.05);
        weights.put("trajectory_confidence", 0.05);
        weights.put("risk_weighting", 0.05);
    }

    public void setDimension(String name, double value) {
        dimensions.put(name, Math.max(0.0, Math.min(1.0, value)));
    }

    public void setWeight(String name, double weight) {
        weights.put(name, weight);
    }

    /**
     * Calculates the normalized aggregate fitness score.
     * @return a score between 0.0 and 1.0
     */
    public double calculateNormalizedScore() {
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String dimension = entry.getKey();
            double weight = entry.getValue();
            double value = dimensions.getOrDefault(dimension, 0.5); // Default to neutral 0.5 if not set

            weightedSum += value * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? weightedSum / totalWeight : 0.5;
    }

    public String getVariantId() {
        return variantId;
    }

    public Map<String, Double> getDimensions() {
        return new HashMap<>(dimensions);
    }

    @Override
    public String toString() {
        return String.format("FitnessEvaluation[variant=%s, score=%.4f, dimensions=%s]",
            variantId, calculateNormalizedScore(), dimensions);
    }
}
