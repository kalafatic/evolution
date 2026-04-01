package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;

/**
 * Router that chooses between LLM providers based on orchestrator settings.
 * Implements HYBRID mode with local proxy optimization and simplification.
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
            return sendRemoteRequest(orchestrator, prompt, temperature, proxyUrl);
        } else if (mode == AiMode.HYBRID) {
            // HYBRID: 3-step process
            // 1. Optimize prompt using local model
            String optimizedPrompt = optimizePromptLocally(orchestrator, prompt, temperature, proxyUrl);

            // 2. Execute using remote model
            String remoteResponse = sendRemoteRequest(orchestrator, optimizedPrompt, temperature, proxyUrl);

            // 3. Simplify response using local model
            return simplifyResponseLocally(orchestrator, remoteResponse, temperature, proxyUrl);
        } else {
            // LOCAL, ollama+selected local model
            String model = orchestrator.getLocalModel();
            if (model != null && !model.isEmpty()) {
                if (orchestrator.getOllama() == null) {
                    orchestrator.setOllama(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOllama());
                }
                orchestrator.getOllama().setModel(model);
            }
            return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl);
        }
    }

    private String sendRemoteRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl) throws Exception {
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
    }

    private String optimizePromptLocally(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl) throws Exception {
        String hybridModel = orchestrator.getHybridModel();
        if (hybridModel != null && !hybridModel.isEmpty()) {
            if (orchestrator.getOllama() == null) {
                orchestrator.setOllama(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOllama());
            }
            orchestrator.getOllama().setModel(hybridModel);
        }

        String optimizationPrompt = "Analyze the following user request and optimize it for AI-to-AI communication. " +
                "Fix errors, clarify intent, and simplify or rewrite the request to be more effective for a large language model. " +
                "Provide ONLY the optimized request text.\n\n" +
                "Request: " + prompt;

        return ollamaProvider.sendRequest(orchestrator, optimizationPrompt, temperature, proxyUrl);
    }

    private String simplifyResponseLocally(Orchestrator orchestrator, String remoteResponse, float temperature, String proxyUrl) throws Exception {
        String simplificationPrompt = "The following is a response from a 'big' AI model. " +
                "Analyze it and simplify it for a human user. " +
                "Focus on the most important information and make it easy to understand. " +
                "Provide ONLY the simplified response text.\n\n" +
                "Response: " + remoteResponse;

        return ollamaProvider.sendRequest(orchestrator, simplificationPrompt, temperature, proxyUrl);
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
        if (mode == AiMode.REMOTE || mode == AiMode.HYBRID) {
            // For HYBRID, test remote connection as it's the most critical part
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

        } else {
            return ollamaProvider.testConnection(orchestrator, temperature, proxyUrl);
        }
    }
}
