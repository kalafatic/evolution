package eu.kalafatic.evolution.controller.orchestration.llm;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Interface for LLM routing policies.
 */
public interface IRoutingPolicy {
    boolean applies(Orchestrator orchestrator, TaskContext context);
    String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception;
}
