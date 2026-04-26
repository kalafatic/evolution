package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
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
     * @param context The task context
     * @return The LLM response
     * @throws Exception If an error occurs
     */
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        AiMode mode = orchestrator.getAiMode();
        if (context != null) context.log("LlmRouter: Routing request in " + mode + " mode.");

        if (mode == AiMode.REMOTE) {
            return sendRemoteRequest(orchestrator, prompt, temperature, proxyUrl, context);
        } else if (mode == AiMode.HYBRID) {
            // HYBRID: 3-step process
            if (context != null) context.log("LlmRouter-Hybrid: Step 1 - Optimizing prompt locally...");
            // 1. Optimize prompt using local model
            String optimizedPrompt = optimizePromptLocally(orchestrator, prompt, temperature, proxyUrl, context);

            // Check for mandatory clarification request (Small Models Strategy)
            if (optimizedPrompt != null && optimizedPrompt.toUpperCase().startsWith("CLARIFICATION:")) {
                if (context != null) context.log("LlmRouter-Hybrid: Clarification required. Stopping pipeline.");
                return optimizedPrompt;
            }

            if (context != null) context.log("LlmRouter-Hybrid: Step 2 - Executing remote request...");
            // 2. Execute using remote model
            String remoteResponse = sendRemoteRequest(orchestrator, optimizedPrompt, temperature, proxyUrl, context);

            if (context != null) context.log("LlmRouter-Hybrid: Step 3 - Simplifying response locally...");
            // 3. Simplify response using local model
            return simplifyResponseLocally(orchestrator, remoteResponse, temperature, proxyUrl, context);
        } else {
            // LOCAL, ollama+selected local model
            String model = orchestrator.getLocalModel();
            if (model != null && !model.isEmpty()) {
                if (orchestrator.getOllama() == null) {
                    orchestrator.setOllama(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOllama());
                }
                orchestrator.getOllama().setModel(model);
            }
            if (context != null) context.log("LlmRouter-Local: Using Ollama model: " + (model != null ? model : "default"));
            return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
        }
    }

    private String sendRemoteRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String remoteModel = orchestrator.getRemoteModel();

        if (context != null) context.log("LlmRouter-Remote: Using model " + (remoteModel != null ? remoteModel : "default (deepseek)"));

        // Default to deepseek if none selected
        if (remoteModel == null || remoteModel.isEmpty()) {
            remoteModel = "deepseek";
            orchestrator.setRemoteModel(remoteModel);
        }

        final String finalRemoteModel = remoteModel;

        String format = "openai"; // default

        // 1. Check custom provider in model
        if (orchestrator.getAiProviders() != null) {
            eu.kalafatic.evolution.model.orchestration.AIProvider custom = orchestrator.getAiProviders().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(finalRemoteModel))
                    .findFirst().orElse(null);
            if (custom != null) {
                format = custom.getFormat();
            } else {
                // 2. Check static config
                ProviderConfig config = AiProviders.PROVIDERS.get(remoteModel.toLowerCase());
                if (config != null) {
                    format = config.getFormat();
                }
            }
        } else {
            // 2. Check static config
            ProviderConfig config = AiProviders.PROVIDERS.get(remoteModel.toLowerCase());
            if (config != null) {
                format = config.getFormat();
            }
        }

        if ("google".equals(format)) {
            return geminiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
        }

        if ("ollama".equals(format)) {
            return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
        }

        // TODO: implement anthropic, cohere if needed.
        // For now, default to common calling (OpenAI format)
        return openAiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    private String optimizePromptLocally(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String hybridModel = orchestrator.getHybridModel();
        if (hybridModel != null && !hybridModel.isEmpty()) {
            if (orchestrator.getOllama() == null) {
                orchestrator.setOllama(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOllama());
            }
            orchestrator.getOllama().setModel(hybridModel);
        }

        String optimizationPrompt = "Analyze the following user request and optimize it for AI-to-AI communication. " +
                "Fix errors, clarify intent, and simplify or rewrite the request to be more effective for a large language model. " +
                "If the request is ambiguous or missing critical information (like target files, specific programming language, or clear goal), " +
                "start your response with 'CLARIFICATION:' followed by your questions. " +
                "Otherwise, provide ONLY the optimized request text.\n\n" +
                "Request: " + prompt;

        return ollamaProvider.sendRequest(orchestrator, optimizationPrompt, temperature, proxyUrl, context);
    }

    private String simplifyResponseLocally(Orchestrator orchestrator, String remoteResponse, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String simplificationPrompt = "The following is a response from a 'big' AI model. " +
                "Analyze it and simplify it for a human user. " +
                "Focus on the most important information and make it easy to understand. " +
                "Provide ONLY the simplified response text.\n\n" +
                "Response: " + remoteResponse;

        return ollamaProvider.sendRequest(orchestrator, simplificationPrompt, temperature, proxyUrl, context);
    }

    /**
     * Tests the connection to the appropriate LLM provider.
     *
     * @param orchestrator The orchestrator model
     * @param temperature The temperature setting
     * @param proxyUrl Optional proxy URL
     * @param context The task context
     * @return The LLM response
     * @throws Exception If an error occurs
     */
    public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
        AiMode mode = orchestrator.getAiMode();
        if (mode == AiMode.REMOTE || mode == AiMode.HYBRID) {
            // For HYBRID, test remote connection as it's the most critical part
            String remoteModel = orchestrator.getRemoteModel();

            // Default to deepseek if none selected
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
                orchestrator.setRemoteModel(remoteModel);
            }

            final String finalRemoteModel = remoteModel;
            String format = "openai";

            // 1. Check custom provider in model
            if (orchestrator.getAiProviders() != null) {
                eu.kalafatic.evolution.model.orchestration.AIProvider custom = orchestrator.getAiProviders().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(finalRemoteModel))
                        .findFirst().orElse(null);
                if (custom != null) {
                    format = custom.getFormat();
                } else {
                    ProviderConfig config = AiProviders.PROVIDERS.get(remoteModel.toLowerCase());
                    if (config != null) format = config.getFormat();
                }
            } else {
                ProviderConfig config = AiProviders.PROVIDERS.get(remoteModel.toLowerCase());
                if (config != null) format = config.getFormat();
            }

            if ("google".equals(format)) {
                return geminiProvider.testConnection(orchestrator, temperature, proxyUrl, context);
            }

            if ("ollama".equals(format)) {
                return ollamaProvider.testConnection(orchestrator, temperature, proxyUrl, context);
            }

            // Default to common calling (OpenAI format)
            return openAiProvider.testConnection(orchestrator, temperature, proxyUrl, context);

        } else {
            return ollamaProvider.testConnection(orchestrator, temperature, proxyUrl, context);
        }
    }
}
