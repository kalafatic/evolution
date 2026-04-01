package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;

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
            String remoteModel = orchestrator.getRemoteModel();

            // Default to deepseek if none selected
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
                orchestrator.setRemoteModel(remoteModel);
            }

            ProviderConfig config = AiProviders.PROVIDERS.get(remoteModel.toLowerCase());

            if (config != null && "google".equals(config.getFormat())) {
                return geminiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl);
            }

            // Default to common calling (OpenAI format)
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

    /**
     * Tests the connection to the appropriate LLM provider.
     *
     * @param orchestrator The orchestrator model
     * @param temperature The temperature setting
     * @param proxyUrl Optional proxy URL
     * @return The LLM response
     * @throws Exception If an error occurs
     */
    public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl) throws Exception {
        AiMode mode = orchestrator.getAiMode();
        if (mode == AiMode.REMOTE) {
            String remoteModel = orchestrator.getRemoteModel();

            // Default to deepseek if none selected
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
                orchestrator.setRemoteModel(remoteModel);
            }

            ProviderConfig config = AiProviders.PROVIDERS.get(remoteModel.toLowerCase());

            if (config != null && "google".equals(config.getFormat())) {
                return geminiProvider.testConnection(orchestrator, temperature, proxyUrl);
            }

            // Default to common calling (OpenAI format)
            return openAiProvider.testConnection(orchestrator, temperature, proxyUrl);

        } else if (mode == AiMode.HYBRID) {
            return ollamaProvider.testConnection(orchestrator, temperature, proxyUrl);
        } else {
            return ollamaProvider.testConnection(orchestrator, temperature, proxyUrl);
        }
    }
}
