package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;

/**
 * Result of an evolutionary assessment of a goal.
 */
public class EvolutionAssessment {

    private final List<EvolutionDimension> unresolvedDimensions = new ArrayList<>();

    public void addDimension(EvolutionDimension dimension) {
        unresolvedDimensions.add(dimension);
    }

    public List<EvolutionDimension> getUnresolvedDimensions() {
        return unresolvedDimensions;
    }

    public boolean hasUnresolvedDimensions() {
        return !unresolvedDimensions.isEmpty();
    }
}
