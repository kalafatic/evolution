package eu.kalafatic.forge.controller.service;

public class EvolutionPolicy {
    private int minEpochsBeforeMutation = 100;
    private long mutationCooldownMs = 60000; // 1 minute
    private double stabilityThreshold = 0.95;

    private long lastMutationTime = 0;
    private int epochsSinceLastMutation = 0;

    public boolean isMutationAllowed(int currentEpochs, double currentStability) {
        long now = System.currentTimeMillis();
        boolean cooldownPassed = (now - lastMutationTime) > mutationCooldownMs;
        boolean stabilityMet = currentStability >= stabilityThreshold;
        boolean epochsMet = currentEpochs >= minEpochsBeforeMutation;

        return cooldownPassed && stabilityMet && epochsMet;
    }

    public void recordMutation() {
        this.lastMutationTime = System.currentTimeMillis();
        this.epochsSinceLastMutation = 0;
    }

    // Getters and setters
    public int getMinEpochsBeforeMutation() { return minEpochsBeforeMutation; }
    public void setMinEpochsBeforeMutation(int minEpochsBeforeMutation) { this.minEpochsBeforeMutation = minEpochsBeforeMutation; }
    public long getMutationCooldownMs() { return mutationCooldownMs; }
    public void setMutationCooldownMs(long mutationCooldownMs) { this.mutationCooldownMs = mutationCooldownMs; }
    public double getStabilityThreshold() { return stabilityThreshold; }
    public void setStabilityThreshold(double stabilityThreshold) { this.stabilityThreshold = stabilityThreshold; }
}
