package eu.kalafatic.evolution.controller.orchestration.cognitive;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.Map;

/**
 * Manages capability scores and implements hysteresis to prevent oscillation.
 */
public class CapabilityScoringEngine {
    private static final double DECAY_FACTOR = 0.95;
    private static final double HYSTERESIS_THRESHOLD = 5.0;
    private static final double DOWNGRADE_PENALTY = 2.0;

    public void updateScores(SessionCognitiveState state, CapabilitySignal signal) {
        Map<CapabilityType, Double> scores = state.getCapabilityScores();

        // 1. Apply gradual decay to all scores
        for (CapabilityType type : CapabilityType.values()) {
            scores.put(type, scores.getOrDefault(type, 0.0) * DECAY_FACTOR);
        }

        // 2. Add current signal weight
        double currentSignalScore = scores.getOrDefault(signal.getCapability(), 0.0);
        scores.put(signal.getCapability(), currentSignalScore + signal.getWeight());

        // 3. Determine potential winner
        CapabilityType candidate = state.getCurrentCapability();
        double maxScore = scores.getOrDefault(candidate, 0.0);

        for (Map.Entry<CapabilityType, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                candidate = entry.getKey();
            }
        }

        // 4. Implement Hysteresis & Depth Bias
        if (candidate != state.getCurrentCapability()) {
            double currentCapScore = scores.getOrDefault(state.getCurrentCapability(), 0.0);

            // Bias: harder to move from ARCHITECTURE/EVOLUTION back to CHAT/CODE
            double transitionThreshold = HYSTERESIS_THRESHOLD;
            if (isDeepCapability(state.getCurrentCapability()) && !isDeepCapability(candidate)) {
                transitionThreshold += DOWNGRADE_PENALTY;
            }

            // Require multiple contrary signals (accumulated score) before transition
            if (maxScore > currentCapScore + transitionThreshold) {
                state.setCurrentCapability(candidate);
            }
        }
    }

    private boolean isDeepCapability(CapabilityType type) {
        return type == CapabilityType.ARCHITECTURE || type == CapabilityType.EVOLUTION || type == CapabilityType.SELF_DEV;
    }
}
