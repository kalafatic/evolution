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
        List<CapabilitySignal> history = state.getCapabilityHistory();
        if (history.isEmpty()) return;

        List<CapabilityType> types = history.stream()
                .map(CapabilitySignal::getCapability)
                .collect(Collectors.toList());

        state.setTrajectory(types);

        // 1. Calculate dominant trend
        CapabilityType dominant = calculateDominantTrend(history);
        state.setDominantTrend(dominant);

        // 2. Calculate direction and metrics
        state.setCurrentDirection(calculateDirection(state, history));
        double velocity = calculateVelocity(history);
        state.setAcceleration(velocity - state.getVelocity());
        state.setVelocity(velocity);
        state.setTrendStability(calculateStability(history, dominant));
    }

    private CapabilityType calculateDominantTrend(List<CapabilitySignal> history) {
        int lookback = Math.min(history.size(), 10);
        Map<CapabilityType, Integer> counts = new HashMap<>();
        for (int i = history.size() - lookback; i < history.size(); i++) {
            CapabilityType type = history.get(i).getCapability();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(CapabilityType.CHAT);
    }

    private CognitiveDirection calculateDirection(SessionCognitiveState state, List<CapabilitySignal> history) {
        if (history.size() < 2) return CognitiveDirection.STABLE;

        CapabilityType last = history.get(history.size() - 1).getCapability();
        CapabilityType prev = history.get(history.size() - 2).getCapability();

        if (getOrdinal(last) > getOrdinal(prev)) return mapToDirection(last); // Deepening
        if (getOrdinal(last) < getOrdinal(prev)) return mapToDirection(last); // Shallowing

        return mapToDirection(last);
    }

    private double calculateVelocity(List<CapabilitySignal> history) {
        if (history.size() < 2) return 0.0;
        int lookback = Math.min(history.size(), 5);
        double totalChange = 0;
        for (int i = history.size() - lookback + 1; i < history.size(); i++) {
            totalChange += Math.abs(getOrdinal(history.get(i).getCapability()) - getOrdinal(history.get(i-1).getCapability()));
        }
        return totalChange / (lookback - 1);
    }

    private double calculateStability(List<CapabilitySignal> history, CapabilityType dominant) {
        if (history.isEmpty()) return 1.0;
        int lookback = Math.min(history.size(), 10);
        long matches = 0;
        for (int i = history.size() - lookback; i < history.size(); i++) {
            if (history.get(i).getCapability() == dominant) matches++;
        }
        return (double) matches / lookback;
    }

    private int getOrdinal(CapabilityType type) {
        switch (type) {
            case EVOLUTION: return 4;
            case ARCHITECTURE: return 3;
            case CODE: return 2;
            case CHAT: return 1;
            default: return 0;
        }
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
