package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Router that chooses between LLM providers based on dynamic policies.
 * Refactored to remove hardcoded mode-based branching.
 */
public class LlmRouter {
    private static final LlmRouter INSTANCE = new LlmRouter();

    public static LlmRouter getInstance() {
        return INSTANCE;
    }

    private ILlmProvider ollamaProvider = new OllamaProvider();

    public void setLocalProvider(ILlmProvider provider) {
        this.ollamaProvider = provider;
    }

    public ILlmProvider getLocalProvider() {
        return ollamaProvider;
    }

    private final ILlmProvider openAiProvider = new OpenAIProvider();
    private final ILlmProvider geminiProvider = new GeminiProvider();

    /**
     * Routes the request to the appropriate LLM provider.
     */
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        if (context != null) {
            String remoteModel = (orchestrator != null) ? orchestrator.getRemoteModel() : "unknown";
            String localModel = (orchestrator != null && orchestrator.getOllama() != null) ? orchestrator.getOllama().getModel() : "local";
            context.log("Stage: LLM\nProvider: dynamic\nModel: " + (remoteModel != null ? remoteModel : localModel));
            context.log("LlmRouter: Routing request via dynamic policies.");
        }

        for (IRoutingPolicy policy : RoutingPolicyRegistry.getPolicies()) {
            if (policy.applies(orchestrator, context)) {
                return policy.handle(this, orchestrator, prompt, temperature, proxyUrl, context);
            }
        }

        // Default fallback to local
        return sendLocalRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    public String sendLocalRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    public String sendRemoteRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        try {
            String provider = orchestrator.getRemoteModel();
            if (provider != null && provider.toLowerCase().contains("gemini")) {
                return geminiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
            return openAiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
        } catch (Exception e) {
            if (context != null) context.log("LlmRouter: Remote request failed (" + e.getMessage() + "). Falling back to LOCAL mode.");
            return sendLocalRequest(orchestrator, prompt, temperature, proxyUrl, context);
        }
    }

    public String buildContextLocally(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        // Optimization phase using local LLM
        String optimizationPrompt = "Please optimize and enrich the following user request with technical context: " + prompt;
        return sendLocalRequest(orchestrator, optimizationPrompt, temperature, proxyUrl, context);
    }

    public String verifyResponseLocally(Orchestrator orchestrator, String response, float temperature, String proxyUrl, TaskContext context) throws Exception {
        // Semantic verification/simplification phase
        String prompt = "Please simplify and verify the following response from a large model: " + response;
        return sendLocalRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    /**
     * Tests the connection to the LLM.
     */
    public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
        return sendRequest(orchestrator, "Ping", temperature, proxyUrl, context);
    }
}
