package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tracks interaction history and calculates metrics like velocity and dominant trend.
 */
public class CognitiveTrajectoryEngine {
    private static final int MAX_HISTORY = 20;

    public void updateTrajectory(SessionCognitiveState state) {
        List<CapabilityType> history = state.getCapabilityHistory().stream()
                .map(CapabilitySignal::getCapability)
                .collect(Collectors.toList());

        state.setTrajectory(history);

        state.setCurrentDirection(calculateDirection(state));
    }

    private CognitiveDirection calculateDirection(SessionCognitiveState state) {
        List<CapabilitySignal> history = state.getCapabilityHistory();
        if (history.isEmpty()) return CognitiveDirection.STABLE;

        // Calculate dominant trend in the last 5 signals
        int size = history.size();
        int lookback = Math.min(size, 5);

        Map<CapabilityType, Integer> counts = new HashMap<>();
        for (int i = size - lookback; i < size; i++) {
            CapabilityType type = history.get(i).getCapability();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }

        CapabilityType dominant = null;
        int max = 0;
        for (Map.Entry<CapabilityType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                dominant = entry.getKey();
            }
        }

        if (dominant == CapabilityType.CHAT && max < 3 && state.getCurrentCapability() != CapabilityType.CHAT) {
             // Hysteresis: Stay in current deep state if chat signals are weak
             return mapToDirection(state.getCurrentCapability());
        }

        if (dominant == null) return CognitiveDirection.STABLE;

        return mapToDirection(dominant);
    }

    private CognitiveDirection mapToDirection(CapabilityType type) {
        switch (type) {
            case CHAT: return CognitiveDirection.EXPLORING;
            case CODE: return CognitiveDirection.CODING;
            case ARCHITECTURE: return CognitiveDirection.ANALYZING;
            case EVOLUTION: return CognitiveDirection.EVOLVING;
            default: return CognitiveDirection.STABLE;
        }
    }
}
