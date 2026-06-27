package eu.kalafatic.evolution.controller.orchestration.cognitive;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents the full cognitive analysis of an interaction, containing all candidates.
 */
public class CapabilityAnalysis {
    private final List<CapabilitySignal> candidates = new ArrayList<>();
    private CapabilitySignal winner;

    public void addCandidate(CapabilitySignal signal) {
        candidates.add(signal);
    }

    public List<CapabilitySignal> getCandidates() {
        return candidates;
    }

    public CapabilitySignal getWinner() {
        return winner;
    }

    public void setWinner(CapabilitySignal winner) {
        this.winner = winner;
    }

    public CapabilitySignal getBestCandidate() {
        return candidates.stream()
                .max((s1, s2) -> Double.compare(s1.getScore(), s2.getScore()))
                .orElse(null);
    }
}
