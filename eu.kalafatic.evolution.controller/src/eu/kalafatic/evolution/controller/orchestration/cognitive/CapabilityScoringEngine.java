package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.Map;

/**
 * Manages capability scores and implements hysteresis to prevent oscillation.
 */
public class CapabilityScoringEngine {
    private static final double DECAY_FACTOR = 0.9;
    private static final double HYSTERESIS_THRESHOLD = 5.0;

    public void updateScores(SessionCognitiveState state, CapabilitySignal signal) {
        Map<CapabilityType, Double> scores = state.getCapabilityScores();

        // Apply decay to all scores
        for (CapabilityType type : CapabilityType.values()) {
            scores.put(type, scores.get(type) * DECAY_FACTOR);
        }

        // Add current signal weight
        double currentScore = scores.getOrDefault(signal.getCapability(), 0.0);
        scores.put(signal.getCapability(), currentScore + signal.getWeight());

        // Determine if capability should transition
        CapabilityType winner = null;
        double maxScore = -1.0;

        for (Map.Entry<CapabilityType, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                winner = entry.getKey();
            }
        }

        if (winner != null && winner != state.getCurrentCapability()) {
            double currentCapScore = scores.getOrDefault(state.getCurrentCapability(), 0.0);
            // Transition only if the new capability significantly outweighs the current one
            if (maxScore > currentCapScore + HYSTERESIS_THRESHOLD) {
                state.setCurrentCapability(winner);
            }
        }
    }
}
