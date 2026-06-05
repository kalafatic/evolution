package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Service to handle AI requests, decoupled from Eclipse UI handlers.
 * Refactored to delegate to LlmRouter for unified routing logic.
 */
public class AiService {

    private LlmRouter llmRouter = LlmRouter.getInstance();

    public void setLlmRouter(LlmRouter router) {
        this.llmRouter = router;
    }

    public String sendRequest(Orchestrator orchestrator, String prompt) throws Exception {
        return sendRequest(orchestrator, prompt, null, null);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, TaskContext context) throws Exception {
        return sendRequest(orchestrator, prompt, null, context);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, String proxyUrl, TaskContext context) throws Exception {
        return sendRequest(orchestrator, prompt, 0.7f, proxyUrl, context);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        return sendRequest(orchestrator, prompt, temperature, proxyUrl, context, null);
    }

    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context, String forcedModel) throws Exception {
        if (forcedModel != null) {
            orchestrator.setLocalModel(forcedModel);
        }
        return llmRouter.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    // Refactored to delegate to ProjectModelManager for unified model loading logic.
    public String[] getOllamaModels(Orchestrator orchestrator) {
        java.util.List<String> models = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().getLlmModels(orchestrator, eu.kalafatic.evolution.model.orchestration.AiMode.LOCAL);
        return models.toArray(new String[0]);
    }
}
