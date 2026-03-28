package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.handlers.OrchestrationCommandHandler;

/**
 * Service to handle AI requests, decoupling agents from Eclipse handlers.
 */
public class AiService {
    private final OrchestrationCommandHandler handler = new OrchestrationCommandHandler();

    public String sendRequest(Orchestrator orchestrator, String prompt) throws Exception {
        return handler.sendRequest(orchestrator, prompt);
    }
}
