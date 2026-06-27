package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a specific engineering path within an EvolutionAxis.
 * Predefines the divergence before the LLM materializes the trajectory.
 */
public class TrajectoryBlueprint {
    private String id;
    private String goal;
    private String strategy;
    private String philosophy;
    private String survivalArgument;
    private String tradeoffs;
    private List<String> requiredCharacteristics = new ArrayList<>();
    private List<String> forbiddenOverlaps = new ArrayList<>();
    private String mutationPressure;
    private String architecturalDirection;
    private String mutationPhilosophy;
    private DarwinStrategyType strategyType = DarwinStrategyType.PHILOSOPHY_MUTATION;
    private TrajectoryVector targetVector = new TrajectoryVector();
    private java.util.Map<String, String> engineeringDimensions = new java.util.HashMap<>();

    public TrajectoryBlueprint(String id, String goal, String strategy) {
        this.id = id;
        this.goal = goal;
        this.strategy = strategy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getPhilosophy() {
        return philosophy;
    }

    public void setPhilosophy(String philosophy) {
        this.philosophy = philosophy;
    }

    public List<String> getRequiredCharacteristics() {
        return requiredCharacteristics;
    }

    public void addRequiredCharacteristic(String characteristic) {
        this.requiredCharacteristics.add(characteristic);
    }

    public List<String> getForbiddenOverlaps() {
        return forbiddenOverlaps;
    }

    public void addForbiddenOverlap(String overlap) {
        this.forbiddenOverlaps.add(overlap);
    }

    public String getMutationPressure() {
        return mutationPressure;
    }

    public void setMutationPressure(String mutationPressure) {
        this.mutationPressure = mutationPressure;
    }

    public String getArchitecturalDirection() {
        return architecturalDirection;
    }

    public void setArchitecturalDirection(String architecturalDirection) {
        this.architecturalDirection = architecturalDirection;
    }

    public String getMutationPhilosophy() {
        return mutationPhilosophy;
    }

    public void setMutationPhilosophy(String mutationPhilosophy) {
        this.mutationPhilosophy = mutationPhilosophy;
    }

    public java.util.Map<String, String> getEngineeringDimensions() {
        return engineeringDimensions;
    }

    public void setEngineeringDimensions(java.util.Map<String, String> engineeringDimensions) {
        this.engineeringDimensions = engineeringDimensions;
    }

    public DarwinStrategyType getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(DarwinStrategyType strategyType) {
        this.strategyType = strategyType;
    }

    public TrajectoryVector getTargetVector() {
        return targetVector;
    }

    public void setTargetVector(TrajectoryVector targetVector) {
        this.targetVector = targetVector;
    }

	public String getSurvivalArgument() { return survivalArgument; }
    public void setSurvivalArgument(String survivalArgument) { this.survivalArgument = survivalArgument; }

    public String getTradeoffs() { return tradeoffs; }
    public void setTradeoffs(String tradeoffs) { this.tradeoffs = tradeoffs; }
}
