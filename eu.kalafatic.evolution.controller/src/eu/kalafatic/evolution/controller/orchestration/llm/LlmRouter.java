package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Router that chooses between LLM providers based on orchestrator settings.
 */
public class LlmRouter {

    private final ILlmProvider ollamaProvider = new OllamaProvider();
    private final ILlmProvider openAiProvider = new OpenAIProvider();

    /**
     * Routes the request to the appropriate LLM provider.
     *
     * @param orchestrator The orchestrator model
     * @param prompt The prompt string
     * @param temperature The temperature setting
     * @param proxyUrl Optional proxy URL
     * @return The LLM response
     * @throws Exception If an error occurs
     */
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl) throws Exception {
        if (orchestrator.isOfflineMode()) {
            return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl);
        } else {
            return openAiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl);
        }
    }
}
