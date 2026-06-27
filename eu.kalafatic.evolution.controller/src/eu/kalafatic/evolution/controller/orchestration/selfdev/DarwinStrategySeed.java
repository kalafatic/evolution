package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
            "TRAJECTORY: MOST PRACTICAL PATH. Goal: most practical and direct engineering future. Characteristics: low risk, direct implementation, predictable execution, minimal architecture, and zero bloat.",
            true
        );
    }

    public static DarwinStrategySeed philosophyMutation() {
        return new DarwinStrategySeed(
            DarwinStrategyType.PHILOSOPHY_MUTATION,
            "TRAJECTORY: ALTERNATIVE PHILOSOPHY. Goal: alternative engineering philosophy. Characteristics: different abstraction level, different extensibility tradeoffs, different maintainability assumptions. Intentionally mutate the core engineering philosophy (e.g., pivot from service-oriented to atomic utility).",
            true
        );
    }

    public static DarwinStrategySeed maximalDivergence() {
        return new DarwinStrategySeed(
            DarwinStrategyType.MAXIMAL_DIVERGENCE,
            "TRAJECTORY: OPPOSITE EXECUTION. Goal: explore opposite execution philosophy. Characteristics: opposite runtime assumptions, opposite architecture style, different dependency strategy, different implementation philosophy. Maximize conceptual distance from all previous trajectories.",
            true
        );
    }

    public static DarwinStrategySeed stabilizationRecovery() {
        return new DarwinStrategySeed(
            DarwinStrategyType.STABILIZATION_RECOVERY,
            "TRAJECTORY: SAFETY FIRST. Goal: safety-first trajectory. Characteristics: validation-heavy, testing-first, minimal change surface, rollback-friendly, and repository-safe. Prioritize stability and risk reduction over feature mutation.",
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

    public static DarwinStrategySeed dependencyRiskAudit() {
        return new DarwinStrategySeed(
            DarwinStrategyType.DEPENDENCY_EXPLORATION,
            "TRAJECTORY: DEPENDENCY RISK AUDIT. Goal: Identify high-risk dependency chains and ripple effects. Focus on structural integrity and external contract safety.",
            false
        );
    }

    public static DarwinStrategySeed runtimeBehaviorInspection() {
        return new DarwinStrategySeed(
            DarwinStrategyType.ARCHITECTURE_MAPPING,
            "TRAJECTORY: RUNTIME BEHAVIOR INSPECTION. Goal: Analyze execution flows and concurrency patterns. Focus on thread safety, resource lifecycles, and performance hotspots.",
            false
        );
    }

    public static DarwinStrategySeed modularityReview() {
        return new DarwinStrategySeed(
            DarwinStrategyType.REFACTOR_HOTSPOT_ANALYSIS,
            "TRAJECTORY: MODULARITY REVIEW. Goal: Assess encapsulation and decoupling. Focus on identifying monolithic leaks and suggesting cleaner module boundaries.",
            false
        );
    }
}
