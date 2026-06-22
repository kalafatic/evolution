package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the semantic state of the organism.
 * It survives every iteration and contains the complete mutation history and constraints.
 */
public class SemanticGenome {
    private final String goal;
    private final List<EvolutionDimension> dimensions = new ArrayList<>();
    private final Map<String, List<String>> dependencies = new HashMap<>();
    private final List<String> allowedMutations = new ArrayList<>();
    private final List<MutationRecord> discoveredMutations = new ArrayList<>();
    private final List<MutationRecord> rejectedMutations = new ArrayList<>();
    private final Set<String> lockedDimensions = new HashSet<>();

    public SemanticGenome(String goal) {
        this.goal = goal;
    }

    public String getGoal() {
        return goal;
    }

    public List<EvolutionDimension> getDimensions() {
        return Collections.unmodifiableList(dimensions);
    }

    public void addDimension(EvolutionDimension dimension) {
        this.dimensions.add(dimension);
    }

    public Map<String, List<String>> getDependencies() {
        return Collections.unmodifiableMap(dependencies);
    }

    public void addDependency(String dimensionId, String dependsOnId) {
        dependencies.computeIfAbsent(dimensionId, k -> new ArrayList<>()).add(dependsOnId);
    }

    public List<String> getAllowedMutations() {
        return Collections.unmodifiableList(allowedMutations);
    }

    public void addAllowedMutation(String mutation) {
        allowedMutations.add(mutation);
    }

    public List<MutationRecord> getDiscoveredMutations() {
        return Collections.unmodifiableList(discoveredMutations);
    }

    public void recordMutation(MutationRecord mutation) {
        discoveredMutations.add(mutation);
    }

    public List<MutationRecord> getRejectedMutations() {
        return Collections.unmodifiableList(rejectedMutations);
    }

    public void recordRejection(MutationRecord mutation) {
        rejectedMutations.add(mutation);
    }

    public Set<String> getLockedDimensions() {
        return Collections.unmodifiableSet(lockedDimensions);
    }

    public void lockDimension(String dimensionId) {
        lockedDimensions.add(dimensionId);
    }

    public void unlockDimension(String dimensionId) {
        lockedDimensions.remove(dimensionId);
    }

    public boolean isLocked(String dimensionId) {
        return lockedDimensions.contains(dimensionId);
    }

    /**
     * Creates a deep copy of the genome for snapshotting.
     */
    public SemanticGenome copy() {
        SemanticGenome copy = new SemanticGenome(this.goal);
        copy.dimensions.addAll(this.dimensions); // EvolutionDimension is assumed immutable enough or shared
        this.dependencies.forEach((k, v) -> copy.dependencies.put(k, new ArrayList<>(v)));
        copy.allowedMutations.addAll(this.allowedMutations);
        copy.discoveredMutations.addAll(this.discoveredMutations);
        copy.rejectedMutations.addAll(this.rejectedMutations);
        copy.lockedDimensions.addAll(this.lockedDimensions);
        return copy;
    }
}
