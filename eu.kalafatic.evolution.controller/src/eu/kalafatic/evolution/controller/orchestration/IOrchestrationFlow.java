package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Strategy interface for different orchestration flows.
 */
public interface IOrchestrationFlow {
    /**
     * Executes the specific orchestration flow.
     * @param request The user request or goal.
     * @param context The execution context.
     * @return The response or evaluation result.
     * @throws Exception if execution fails.
     */
    OrchestratorResponse execute(String request, TaskContext context) throws Exception;
}
