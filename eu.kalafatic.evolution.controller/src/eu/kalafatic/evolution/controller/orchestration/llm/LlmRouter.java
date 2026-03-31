package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Router that chooses between LLM providers based on orchestrator settings.
 */
public class LlmRouter {

    private final ILlmProvider ollamaProvider = new OllamaProvider();
    private final ILlmProvider openAiProvider = new OpenAIProvider();
    private final ILlmProvider geminiProvider = new GeminiProvider();

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
        AiMode mode = orchestrator.getAiMode();
        if (mode == AiMode.REMOTE) {
            // REMOTE, direct chat with selected llm (gemini,deepseek,chatgpt...)
            String model = orchestrator.getRemoteModel();
            if (model != null && model.toLowerCase().contains("gemini")) {
                return geminiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl);
            }

            // Default to OpenAIProvider which supports custom URLs (for proxy/compat APIs)
            if (model != null && !model.isEmpty()) {
                orchestrator.setOpenAiModel(model);
            }
            return openAiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl);
        } else if (mode == AiMode.HYBRID) {
            // HYBRID, ollama+selected local model which connects to remote like translattor or proxy
            String model = orchestrator.getHybridModel();
            if (model != null && !model.isEmpty()) {
                orchestrator.getOllama().setModel(model);
            }
            return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl);
        } else {
            // LOCAL, ollama+selected local model
            String model = orchestrator.getLocalModel();
            if (model != null && !model.isEmpty()) {
                orchestrator.getOllama().setModel(model);
            }
            return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl);
        }
    }
}
