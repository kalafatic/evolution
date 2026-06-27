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
    private String goal;
    private final List<EvolutionDimension> dimensions = new ArrayList<>();
    private final Map<String, List<String>> dependencies = new HashMap<>();
    private final List<String> allowedMutations = new ArrayList<>();
    private final List<MutationRecord> discoveredMutations = new ArrayList<>();
    private final List<MutationRecord> rejectedMutations = new ArrayList<>();
    private final Set<String> lockedDimensions = new HashSet<>();

    public SemanticGenome() {
        // Default constructor for Jackson
    }

    public SemanticGenome(String goal) {
        this.goal = goal;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public List<EvolutionDimension> getDimensions() {
        return Collections.unmodifiableList(dimensions);
    }

    public void addDimension(EvolutionDimension dimension) {
        this.dimensions.add(dimension);
    }

    public void setDimensions(List<EvolutionDimension> dimensions) {
        this.dimensions.clear();
        if (dimensions != null) {
            this.dimensions.addAll(dimensions);
        }
    }

    public Map<String, List<String>> getDependencies() {
        return Collections.unmodifiableMap(dependencies);
    }

    public void addDependency(String dimensionId, String dependsOnId) {
        dependencies.computeIfAbsent(dimensionId, k -> new ArrayList<>()).add(dependsOnId);
    }

    public void setDependencies(Map<String, List<String>> dependencies) {
        this.dependencies.clear();
        if (dependencies != null) {
            this.dependencies.putAll(dependencies);
        }
    }

    public List<String> getAllowedMutations() {
        return Collections.unmodifiableList(allowedMutations);
    }

    public void addAllowedMutation(String mutation) {
        allowedMutations.add(mutation);
    }

    public void setAllowedMutations(List<String> allowedMutations) {
        this.allowedMutations.clear();
        if (allowedMutations != null) {
            this.allowedMutations.addAll(allowedMutations);
        }
    }

    public List<MutationRecord> getDiscoveredMutations() {
        return Collections.unmodifiableList(discoveredMutations);
    }

    public void recordMutation(MutationRecord mutation) {
        discoveredMutations.add(mutation);
    }

    public void setDiscoveredMutations(List<MutationRecord> discoveredMutations) {
        this.discoveredMutations.clear();
        if (discoveredMutations != null) {
            this.discoveredMutations.addAll(discoveredMutations);
        }
    }

    public List<MutationRecord> getRejectedMutations() {
        return Collections.unmodifiableList(rejectedMutations);
    }

    public void recordRejection(MutationRecord mutation) {
        rejectedMutations.add(mutation);
    }

    public void setRejectedMutations(List<MutationRecord> rejectedMutations) {
        this.rejectedMutations.clear();
        if (rejectedMutations != null) {
            this.rejectedMutations.addAll(rejectedMutations);
        }
    }

    public Set<String> getLockedDimensions() {
        return Collections.unmodifiableSet(lockedDimensions);
    }

    public void lockDimension(String dimensionId) {
        lockedDimensions.add(dimensionId);
    }

    public void setLockedDimensions(Set<String> lockedDimensions) {
        this.lockedDimensions.clear();
        if (lockedDimensions != null) {
            this.lockedDimensions.addAll(lockedDimensions);
        }
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
