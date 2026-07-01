package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Determines the next mutable dimension in the evolutionary process.
 * Responsible for maintaining mutation order and avoiding repeated dimensions.
 */
public class GenomeDimensionScheduler {

    /**
     * Selects the next dimension to mutate based on the current state of the genome.
     */
    public EvolutionDimension selectNextDimension(SemanticGenome genome) {
        List<EvolutionDimension> candidates = genome.getDimensions().stream()
                .filter(d -> !genome.isLocked(d.getId()))
                .filter(d -> areDependenciesLocked(d, genome))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            // If everything is locked or no candidates meet dependencies, we might need to unlock something
            // or we have reached a stable state.
            return null;
        }

        // Prioritize by composite score: significance, pressure, and ambiguity
        candidates.sort((d1, d2) -> {
            double score1 = calculateCompositeScore(d1);
            double score2 = calculateCompositeScore(d2);
            return Double.compare(score2, score1);
        });

        return candidates.get(0);
    }

    /**
     * Calculates a composite score for dimension prioritization.
     * Weights: Significance (50%), Pressure (30%), Ambiguity (20%)
     */
    private double calculateCompositeScore(EvolutionDimension dim) {
        return (dim.getSignificanceScore() * 0.5) +
               (dim.getEvolutionaryPressure() * 0.3) +
               (dim.getAmbiguityScore() * 0.2);
    }

    private boolean areDependenciesLocked(EvolutionDimension dimension, SemanticGenome genome) {
        List<String> deps = dimension.getDependencyDimensions();
        if (deps == null || deps.isEmpty()) {
            return true;
        }
        for (String depId : deps) {
            if (!genome.isLocked(depId)) {
                return false;
            }
        }
        return true;
    }
}
