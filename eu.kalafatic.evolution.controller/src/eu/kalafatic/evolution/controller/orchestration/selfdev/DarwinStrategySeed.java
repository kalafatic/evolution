package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Seed for spawning a specific Darwin evolutionary branch.
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

    public static DarwinStrategySeed implementation() {
        return new DarwinStrategySeed(
            DarwinStrategyType.IMPLEMENTATION,
            "Focus on concrete execution, file changes, implementation completion, and functional behavior. Propose the most direct path to fulfilling the user goal.",
            true
        );
    }

    public static DarwinStrategySeed analytical() {
        return new DarwinStrategySeed(
            DarwinStrategyType.ANALYTICAL,
            "Focus on architecture impact, dependency analysis, integration risks, and regression detection. Propose changes that improve system understanding or safety.",
            true
        );
    }

    public static DarwinStrategySeed stabilization() {
        return new DarwinStrategySeed(
            DarwinStrategyType.STABILIZATION,
            "Focus on system stability, bug fixes, edge case handling, and hardening existing logic. Propose changes that make the current implementation more robust.",
            false
        );
    }

    public static DarwinStrategySeed exploration() {
        return new DarwinStrategySeed(
            DarwinStrategyType.EXPLORATION,
            "Focus on alternative architectural paths, performance optimizations, or hidden project dependencies. Propose an innovative or non-obvious solution.",
            false
        );
    }
}
