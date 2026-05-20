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
            "Focus on architectural alignment and safety within the scope of the goal. Analyze how the proposed change impacts existing components and identify integration risks. Avoid generic architectural restructuring unless strictly necessary for the goal.",
            true
        );
    }

    public static DarwinStrategySeed stabilization() {
        return new DarwinStrategySeed(
            DarwinStrategyType.STABILIZATION,
            "Focus on making the implementation robust. Handle edge cases, improve error reporting, and ensure the solution is idiomatic and stable. Stay grounded in the user's specific request.",
            false
        );
    }

    public static DarwinStrategySeed exploration() {
        return new DarwinStrategySeed(
            DarwinStrategyType.EXPLORATION,
            "Focus on alternative technical approaches to the same goal. Explore different libraries, patterns, or performance optimizations that remain strictly relevant to the user objective. Avoid 'hallucinating' unrelated project needs.",
            false
        );
    }
}
