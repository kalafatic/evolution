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

    public static DarwinStrategySeed exploration() {
        return new DarwinStrategySeed(
            DarwinStrategyType.EXPLORATION,
            "Focus on innovation and alternative architectural futures. Explore different solution hypotheses, design patterns, or performance-oriented models that fulfill the user goal. Priority: Innovation and tradeoff exploration.",
            true
        );
    }

    public static DarwinStrategySeed analytical() {
        return new DarwinStrategySeed(
            DarwinStrategyType.ANALYTICAL,
            "Focus on system understanding and impact analysis. Identify integration risks, dependency conflicts, and potential failure modes. Priority: Correctness reasoning and architectural insight.",
            true
        );
    }

    public static DarwinStrategySeed stabilization() {
        return new DarwinStrategySeed(
            DarwinStrategyType.STABILIZATION,
            "Focus on safe evolution paths and safe refactor strategies. Propose incremental improvements and structural cleanup that satisfy the goal while maintaining system reliability. Priority: Reliability and controlled change.",
            true
        );
    }

    // Compatibility bridge
    public static DarwinStrategySeed implementation() {
        return exploration();
    }

    public static DarwinStrategySeed conservativeFuture() {
        return stabilization();
    }

    public static DarwinStrategySeed innovativeFuture() {
        return exploration();
    }

    public static DarwinStrategySeed structuralFuture() {
        return analytical();
    }
}
