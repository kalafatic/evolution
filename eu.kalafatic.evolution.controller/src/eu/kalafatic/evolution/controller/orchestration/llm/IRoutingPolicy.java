package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Interface for LLM routing policies.
 */
public interface IRoutingPolicy {
    boolean applies(Orchestrator orchestrator, TaskContext context);
    String handle(LlmRouter router, Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception;
}
