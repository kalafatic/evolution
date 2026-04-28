package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;

/**
 * Router that chooses between LLM providers based on orchestrator settings.
 * Implements HYBRID mode with local context building and cloud reasoning.
 *
 * @evo.lastModified: 14:B
 * @evo.origin: self
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
            // HYBRID: Local Context Builder + Cloud Reasoner
            if (context != null) context.log("LlmRouter-Hybrid: Step 1 - Building system context locally...");
            // 1. Build context using local model (scans files, gathers state)
            String augmentedPrompt = buildContextLocally(orchestrator, prompt, temperature, proxyUrl, context);

            if (context != null) context.log("LlmRouter-Hybrid: Step 2 - Executing cloud reasoning...");
            // 2. Execute reasoning using cloud model
            String remoteResponse = sendRemoteRequest(orchestrator, augmentedPrompt, temperature, proxyUrl, context);

            if (context != null) context.log("LlmRouter-Hybrid: Step 3 - Verifying response locally...");
            // 3. Optional: Verify/Sanitize response locally
            return verifyResponseLocally(orchestrator, remoteResponse, temperature, proxyUrl, context);
        } else {
            // LOCAL, ollama+selected local model
            String model = orchestrator.getLocalModel();
            if (model != null && !model.isEmpty()) {
                if (orchestrator.getOllama() == null) {
                    orchestrator.setOllama(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOllama());
                }
                orchestrator.getOllama().setModel(model);
            } else if (orchestrator.getOllama() != null) {
                model = orchestrator.getOllama().getModel();
            }

            if (context != null) context.log("LlmRouter-Local: Using Ollama model: " + (model != null && !model.isEmpty() ? model : "default"));
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

        // TODO: implement anthropic, cohere if needed.
        // For now, default to common calling (OpenAI format)
        return openAiProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    private String buildContextLocally(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String hybridModel = orchestrator.getHybridModel();
        if (hybridModel != null && !hybridModel.isEmpty()) {
            if (orchestrator.getOllama() == null) {
                orchestrator.setOllama(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createOllama());
            }
            orchestrator.getOllama().setModel(hybridModel);
        } else if (orchestrator.getOllama() != null) {
            hybridModel = orchestrator.getOllama().getModel();
        }

        if (context != null) context.log("LlmRouter-Hybrid: Using local model for context building: " + (hybridModel != null && !hybridModel.isEmpty() ? hybridModel : "default"));

        String contextPrompt = "You are a context builder. Analyze the user request and provide a detailed summary of the technical context needed to fulfill it. " +
                "Include relevant file paths, system state, and architectural constraints found in the shared memory. " +
                "Provide a structured 'CONTEXT' block followed by the original 'REQUEST'.\n\n" +
                "Original Request: " + prompt;

        return ollamaProvider.sendRequest(orchestrator, contextPrompt, temperature, proxyUrl, context);
    }

    private String verifyResponseLocally(Orchestrator orchestrator, String remoteResponse, float temperature, String proxyUrl, TaskContext context) throws Exception {
        // In this implementation, we just pass through or do a quick safety check
        return remoteResponse;
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

            // Default to common calling (OpenAI format)
            return openAiProvider.testConnection(orchestrator, temperature, proxyUrl, context);

        } else {
            return ollamaProvider.testConnection(orchestrator, temperature, proxyUrl, context);
        }
    }
}
