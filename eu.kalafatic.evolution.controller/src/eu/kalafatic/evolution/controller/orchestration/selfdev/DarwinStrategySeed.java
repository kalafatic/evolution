package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Seed for spawning a specific Darwin evolutionary branch trajectory.
 */
public class DarwinStrategySeed {
    private final DarwinStrategyType type;
    private final String instructions;
    private final boolean mandatory;

    // Semantic metadata for mutation-driven exploration
    private String interpretation;
    private String assumption;
    private String futureGoal;

    public DarwinStrategySeed(DarwinStrategyType type, String instructions, boolean mandatory) {
        this.type = type;
        this.instructions = instructions;
        this.mandatory = mandatory;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public String getAssumption() {
        return assumption;
    }

    public void setAssumption(String assumption) {
        this.assumption = assumption;
    }

    public String getFutureGoal() {
        return futureGoal;
    }

    public void setFutureGoal(String futureGoal) {
        this.futureGoal = futureGoal;
    }

    public DarwinStrategyType getType() {
        return type;
    }

    public String getInstructions() {
        return instructions;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public static DarwinStrategySeed probableSurvivor() {
        return new DarwinStrategySeed(
            DarwinStrategyType.PROBABLE_SURVIVOR,
            "Generate the most probable surviving engineering future. Focus on direct, high-confidence execution of the primary objective with standard architectural patterns.",
            true
        );
    }

    public static DarwinStrategySeed philosophyMutation() {
        return new DarwinStrategySeed(
            DarwinStrategyType.PHILOSOPHY_MUTATION,
            "Analyze the previous probable trajectory and intentionally mutate the engineering philosophy (e.g., flip from service-oriented to atomic utility, or from synchronous to event-driven).",
            true
        );
    }

    public static DarwinStrategySeed maximalDivergence() {
        return new DarwinStrategySeed(
            DarwinStrategyType.MAXIMAL_DIVERGENCE,
            "Maximize conceptual distance from all previous trajectories. Explore unconventional engineering tradeoffs, high-risk/high-payoff architectures, or radically different implementation scopes.",
            true
        );
    }

    public static DarwinStrategySeed stabilizationRecovery() {
        return new DarwinStrategySeed(
            DarwinStrategyType.STABILIZATION_RECOVERY,
            "Focus on system stability, risk reduction, or architectural analysis. This future prioritizes understanding and mapping the system before or instead of direct implementation mutation.",
            true
        );
    }

    // Semantic metadata wrapper for flexible future spawning
    public static DarwinStrategySeed semanticFuture(DarwinStrategyType type, String interpretation, String assumption, String futureGoal) {
        DarwinStrategySeed seed = new DarwinStrategySeed(
            type,
            "Realize this specific engineering future: " + futureGoal + ". Grounded in interpretation: " + interpretation + " and architectural assumption: " + assumption,
            false
        );
        seed.setInterpretation(interpretation);
        seed.setAssumption(assumption);
        seed.setFutureGoal(futureGoal);
        return seed;
    }

    /**
     * Legacy support for semanticFuture with 3 args.
     * Defaults to PHILOSOPHY_MUTATION as it usually represents a variation.
     */
    public static DarwinStrategySeed semanticFuture(String interpretation, String assumption, String futureGoal) {
        return semanticFuture(DarwinStrategyType.PHILOSOPHY_MUTATION, interpretation, assumption, futureGoal);
    }

    // Compatibility bridge
    public static DarwinStrategySeed keeperEvolution() {
        return probableSurvivor();
    }

    public static DarwinStrategySeed divergenceA() {
        return philosophyMutation();
    }

    public static DarwinStrategySeed divergenceB() {
        return maximalDivergence();
    }

    public static DarwinStrategySeed synthesisHybrid() {
        return stabilizationRecovery();
    }

    // --- Mediated Cognitive Seeds ---

    public static DarwinStrategySeed architectureMapping() {
        return new DarwinStrategySeed(
            DarwinStrategyType.ARCHITECTURE_MAPPING,
            "Focus on mapping the core architecture, identification of primary components, and high-level structural patterns.",
            true
        );
    }

    public static DarwinStrategySeed dependencyExploration() {
        return new DarwinStrategySeed(
            DarwinStrategyType.DEPENDENCY_EXPLORATION,
            "Analyze module relationships, dependency graphs, and cross-cutting concerns to understand the ripple effects of changes.",
            true
        );
    }

    public static DarwinStrategySeed refactorHotspotAnalysis() {
        return new DarwinStrategySeed(
            DarwinStrategyType.REFACTOR_HOTSPOT_ANALYSIS,
            "Identify areas of high complexity, technical debt, or frequent instability that are prime candidates for refactoring.",
            true
        );
    }

    public static DarwinStrategySeed contextReduction() {
        return new DarwinStrategySeed(
            DarwinStrategyType.CONTEXT_REDUCTION,
            "Evolve a minimal, high-signal context package. Identify the exact set of files and metadata needed for external reasoning with zero noise.",
            true
        );
    }
}
