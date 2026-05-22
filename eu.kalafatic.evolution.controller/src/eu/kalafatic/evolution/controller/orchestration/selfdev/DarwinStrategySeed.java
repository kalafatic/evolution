package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Seed for spawning a specific Darwin evolutionary branch role.
 */
public class DarwinStrategySeed {
    private final DarwinStrategyType type;
    private final String instructions;
    private final boolean mandatory;

    public DarwinStrategySeed(DarwinStrategyType type, String instructions, boolean mandatory) {
        this.type = type;
        this.instructions = instructions;
        this.mandatory = mandatory;
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

    public static DarwinStrategySeed keeperEvolution() {
        return new DarwinStrategySeed(
            DarwinStrategyType.KEEPER_EVOLUTION,
            "Take the previous selected branch (if exists) and produce the BEST continuation or improvement. Focus on direct solution evolution and refinement of the current trajectory.",
            true
        );
    }

    public static DarwinStrategySeed divergenceA() {
        return new DarwinStrategySeed(
            DarwinStrategyType.DIVERGENCE_A,
            "Explicitly FORBID repeating the architecture, execution model, or core approach of previous branches. Generate a DIFFERENT, structurally distinct solution direction.",
            true
        );
    }

    public static DarwinStrategySeed divergenceB() {
        return new DarwinStrategySeed(
            DarwinStrategyType.DIVERGENCE_B,
            "Ensure divergence from all previous branches. Choose an alternative architecture, a major refactor direction, or a risk/stability-focused redesign that has not been explored yet.",
            true
        );
    }

    public static DarwinStrategySeed synthesisHybrid() {
        return new DarwinStrategySeed(
            DarwinStrategyType.SYNTHESIS_HYBRID,
            "Perform a synthesis of all previous branches. Compare tradeoffs, identify risks, and propose a hybrid or optimized trajectory. MUST NOT duplicate prior branches.",
            true
        );
    }

    // Compatibility bridge - mapping old roles to new mutation steps
    public static DarwinStrategySeed exploration() {
        return keeperEvolution();
    }

    public static DarwinStrategySeed analytical() {
        return synthesisHybrid();
    }

    public static DarwinStrategySeed stabilization() {
        return divergenceA();
    }

    public static DarwinStrategySeed implementation() {
        return keeperEvolution();
    }

    public static DarwinStrategySeed conservativeFuture() {
        return divergenceB();
    }

    public static DarwinStrategySeed innovativeFuture() {
        return divergenceA();
    }

    public static DarwinStrategySeed structuralFuture() {
        return synthesisHybrid();
    }
}
