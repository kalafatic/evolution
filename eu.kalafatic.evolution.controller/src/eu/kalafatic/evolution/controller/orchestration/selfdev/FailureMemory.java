package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.HashMap;
import java.util.Map;

public class FailureMemory {
    private Map<String, Integer> fingerprints = new HashMap<>();
    private Map<String, Integer> strategyFailures = new HashMap<>();
    private Map<String, Double> mutationEffectiveness = new HashMap<>();

    public void addFingerprint(String fingerprint) {
        fingerprints.put(fingerprint, fingerprints.getOrDefault(fingerprint, 0) + 1);
    }

    public void recordStrategyFailure(String strategy) {
        strategyFailures.put(strategy, strategyFailures.getOrDefault(strategy, 0) + 1);
    }

    public int getStrategyFailureCount(String strategy) {
        return strategyFailures.getOrDefault(strategy, 0);
    }

    public void updateMutationEffectiveness(String strategy, double score) {
        double current = mutationEffectiveness.getOrDefault(strategy, 0.5);
        mutationEffectiveness.put(strategy, (current * 0.7) + (score * 0.3));
    }

    public double getMutationEffectiveness(String strategy) {
        return mutationEffectiveness.getOrDefault(strategy, 0.5);
    }

    public int getCount(String fingerprint) {
        return fingerprints.getOrDefault(fingerprint, 0);
    }

    public boolean isRepeating(String fingerprint) {
        return getCount(fingerprint) >= 2;
    }

    public Map<String, Integer> getFingerprints() {
        return fingerprints;
    }

    public Map<String, Integer> getStrategyFailures() {
        return strategyFailures;
    }

    public Map<String, Double> getMutationEffectiveness() {
        return mutationEffectiveness;
    }

    public synchronized void restore(
            Map<String, Integer> fingerprints,
            Map<String, Integer> strategyFailures,
            Map<String, Double> mutationEffectiveness) {
        if (fingerprints != null) {
            this.fingerprints.clear();
            this.fingerprints.putAll(fingerprints);
        }
        if (strategyFailures != null) {
            this.strategyFailures.clear();
            this.strategyFailures.putAll(strategyFailures);
        }
        if (mutationEffectiveness != null) {
            this.mutationEffectiveness.clear();
            this.mutationEffectiveness.putAll(mutationEffectiveness);
        }
    }
}
