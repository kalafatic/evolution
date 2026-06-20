package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;

/**
 * Defines the execution parameters for an evolutionary iteration.
 * Decouples the evolutionary kernel from software-engineering specific operators.
 * This class is immutable to ensure turn-level isolation.
 */
public final class EvolutionExecutionProfile {
    private final int intensity;
    private final CapabilityType capability;
    private final boolean useGit;
    private final boolean useCompiler;
    private final boolean useTests;
    private final boolean useSemanticEvaluation;
    private final boolean useParallelBranches;
    private final boolean requireUserSelection;
    private final boolean persistBranches;
    private final boolean useImplementation;

    private EvolutionExecutionProfile(
            CapabilityType capability,
            int intensity,
            boolean useGit,
            boolean useCompiler,
            boolean useTests,
            boolean useSemanticEvaluation,
            boolean useParallelBranches,
            boolean requireUserSelection,
            boolean persistBranches,
            boolean useImplementation) {
        this.capability = capability;
        this.intensity = intensity;
        this.useGit = useGit;
        this.useCompiler = useCompiler;
        this.useTests = useTests;
        this.useSemanticEvaluation = useSemanticEvaluation;
        this.useParallelBranches = useParallelBranches;
        this.requireUserSelection = requireUserSelection;
        this.persistBranches = persistBranches;
        this.useImplementation = useImplementation;
    }

    public static EvolutionExecutionProfile create(CapabilityType capability, int intensity) {
        boolean useGit = false;
        boolean useCompiler = false;
        boolean useTests = false;
        boolean useImplementation = true;
        boolean useParallelBranches = true;
        boolean requireUserSelection = true;
        boolean persistBranches = true;

        // 1. Capability-based defaults
        switch (capability) {
            case CHAT:
                useGit = false;
                useCompiler = false;
                useTests = false;
                useImplementation = false;
                useParallelBranches = false;
                requireUserSelection = false;
                persistBranches = false;
                break;
            case CODE:
                useGit = true;
                useCompiler = true;
                useTests = true;
                useImplementation = true;
                break;
            case ARCHITECTURE:
                useGit = true;
                useCompiler = false;
                useTests = false;
                useImplementation = true;
                break;
            case EVOLUTION:
                useGit = true;
                useCompiler = true;
                useTests = true;
                useImplementation = true;
                break;
        }

        // 2. Intensity-based overrides
        if (intensity == 1) {
            useParallelBranches = false;
            requireUserSelection = false;
            useImplementation = false; // Intensity 1 is always text-only
        }

        return new EvolutionExecutionProfile(
            capability, intensity, useGit, useCompiler, useTests,
            true, // useSemanticEvaluation always true
            useParallelBranches, requireUserSelection, persistBranches, useImplementation);
    }

    public int getIntensity() { return intensity; }
    public CapabilityType getCapability() { return capability; }
    public boolean useGit() { return useGit; }
    public boolean useCompiler() { return useCompiler; }
    public boolean useTests() { return useTests; }
    public boolean useSemanticEvaluation() { return useSemanticEvaluation; }
    public boolean useParallelBranches() { return useParallelBranches; }
    public boolean requireUserSelection() { return requireUserSelection; }
    public boolean persistBranches() { return persistBranches; }
    public boolean useImplementation() { return useImplementation; }
}
