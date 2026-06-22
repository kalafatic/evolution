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

        // Prioritize by significance score if available
        candidates.sort((d1, d2) -> Double.compare(d2.getSignificanceScore(), d1.getSignificanceScore()));

        return candidates.get(0);
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
