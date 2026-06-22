package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;

/**
 * Defines the execution parameters for an evolutionary iteration.
 * Decouples the evolutionary kernel from software-engineering specific operators.
 * This class is immutable to ensure turn-level isolation.
 */
public final class EvolutionProfile {
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
    private final boolean requiresRepository;
    private final boolean shouldPerformRealityCheck;
    private final boolean shouldShowEvolutionSummary;
    private final boolean shouldShowRepositoryChanges;

    private EvolutionProfile(
            CapabilityType capability,
            int intensity,
            boolean useGit,
            boolean useCompiler,
            boolean useTests,
            boolean useSemanticEvaluation,
            boolean useParallelBranches,
            boolean requireUserSelection,
            boolean persistBranches,
            boolean useImplementation,
            boolean requiresRepository,
            boolean shouldPerformRealityCheck,
            boolean shouldShowEvolutionSummary,
            boolean shouldShowRepositoryChanges) {
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
        this.requiresRepository = requiresRepository;
        this.shouldPerformRealityCheck = shouldPerformRealityCheck;
        this.shouldShowEvolutionSummary = shouldShowEvolutionSummary;
        this.shouldShowRepositoryChanges = shouldShowRepositoryChanges;
    }

    public static EvolutionProfile create(CapabilityType capability, int intensity) {
        boolean useGit = false;
        boolean useCompiler = false;
        boolean useTests = false;
        boolean useImplementation = true;
        boolean useParallelBranches = true;
        boolean requireUserSelection = true;
        boolean persistBranches = true;
        boolean requiresRepository = true;
        boolean shouldPerformRealityCheck = true;
        boolean shouldShowEvolutionSummary = true;
        boolean shouldShowRepositoryChanges = true;

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
                requiresRepository = false;
                shouldPerformRealityCheck = false;
                shouldShowEvolutionSummary = false;
                shouldShowRepositoryChanges = false;
                break;
            case CODE:
                useGit = true;
                useCompiler = true;
                useTests = true;
                useImplementation = true;
                requiresRepository = true;
                shouldPerformRealityCheck = true;
                break;
            case ARCHITECTURE:
                useGit = true;
                useCompiler = false;
                useTests = false;
                useImplementation = true;
                requiresRepository = true;
                shouldPerformRealityCheck = true;
                break;
            case EVOLUTION:
                useGit = true;
                useCompiler = true;
                useTests = true;
                useImplementation = true;
                requiresRepository = true;
                shouldPerformRealityCheck = true;
                break;
        }

        // 2. Intensity-based overrides
        if (intensity == 1) {
            if (capability == CapabilityType.CHAT) {
                useParallelBranches = false;
                requireUserSelection = false;
                useImplementation = false;
            } else {
                useParallelBranches = true;
                requireUserSelection = true;
                useImplementation = true;
                shouldPerformRealityCheck = true;
            }
        }

        return new EvolutionProfile(
            capability, intensity, useGit, useCompiler, useTests,
            true, // useSemanticEvaluation always true
            useParallelBranches, requireUserSelection, persistBranches, useImplementation,
            requiresRepository, shouldPerformRealityCheck, shouldShowEvolutionSummary, shouldShowRepositoryChanges);
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
    public boolean requiresRepository() { return requiresRepository; }
    public boolean shouldPerformRealityCheck() { return shouldPerformRealityCheck; }
    public boolean shouldShowEvolutionSummary() { return shouldShowEvolutionSummary; }
    public boolean shouldShowRepositoryChanges() { return shouldShowRepositoryChanges; }

    public String getPhaseDisplayName(EvolutionPhase phase) {
        if (capability == CapabilityType.CHAT) {
            switch (phase) {
                case INTENT_EXPANSION: return "UNDERSTANDING";
                case FINAL_SYNTHESIS: return "GENERATING";
                case TERMINAL_SUCCESS: return "DONE";
                default: return phase.name();
            }
        }
        return phase.name();
    }

    public String getVariantDisplayName() {
        return capability == CapabilityType.CHAT ? "Candidate Response" : "Branch Variant";
    }
}
