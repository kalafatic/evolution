package eu.kalafatic.evolution.controller.orchestration.behavior;

/**
 * A functional interface for policy modification rules.
 */
@FunctionalInterface
public interface PolicyRule {
    /**
     * Evaluates and potentially modifies the given policy based on rule logic.
     * @param policy The policy to evaluate and modify.
     */
    void apply(ExecutionPolicy policy);
}
