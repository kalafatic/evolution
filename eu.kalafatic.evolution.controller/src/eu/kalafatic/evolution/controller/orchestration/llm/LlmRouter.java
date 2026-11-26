package eu.kalafatic.evolution.controller.orchestration.llm;

import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.AiMode;
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
    private ILlmProvider nativeEvoProvider = new NativeEvoProvider();
    private volatile ILlmProvider enrProvider;

    public void setLocalProvider(ILlmProvider provider) {
        this.ollamaProvider = provider;
    }

    public ILlmProvider getLocalProvider() {
        return ollamaProvider;
    }

    public void setNativeEvoProvider(ILlmProvider provider) {
        this.nativeEvoProvider = provider;
    }

    public ILlmProvider getNativeEvoProvider() {
        return nativeEvoProvider;
    }

    /**
     * Optional ENR (Evolution Neural Runtime) provider bridge, registered by the ENR bundle
     * when present (Phase 6). When configured, ENR becomes the default local inference path;
     * the direct Ollama/native providers remain available as fallbacks.
     */
    public void setEnrProvider(ILlmProvider provider) {
        this.enrProvider = provider;
    }

    public ILlmProvider getEnrProvider() {
        return enrProvider;
    }

    public boolean isEnrAvailable() {
        return enrProvider != null;
    }

    private final ILlmProvider openAiProvider = new OpenAIProvider();
    private final ILlmProvider geminiProvider = new GeminiProvider();

    /**
     * Routes the request to the appropriate LLM provider and returns normalized LlmResponse.
     */
    public LlmResponse sendLlmRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String activeModel = "unknown";
        if (orchestrator != null) {
            AiMode mode = orchestrator.getAiMode();
            if (mode == AiMode.REMOTE) {
                activeModel = orchestrator.getRemoteModel();
            } else if (mode == AiMode.HYBRID) {
                activeModel = orchestrator.getHybridModel();
                if (activeModel == null || activeModel.isEmpty()) {
                    activeModel = orchestrator.getLocalModel();
                }
                if (activeModel == null || activeModel.isEmpty()) {
                    activeModel = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getModel() : "Hybrid";
                }
            } else {
                activeModel = orchestrator.getLocalModel();
                if (activeModel == null || activeModel.isEmpty()) {
                    activeModel = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getModel() : "local";
                }
            }
        }

        if (context != null) {
            context.log("Stage: LLM\nProvider: dynamic\nModel: " + (activeModel != null ? activeModel : "unknown"));
            context.log("LlmRouter: Routing request via dynamic policies.");
        }

        String rawResponse = null;
        for (IRoutingPolicy policy : RoutingPolicyRegistry.getPolicies()) {
            if (policy.applies(orchestrator, context)) {
                rawResponse = policy.handle(this, orchestrator, prompt, temperature, proxyUrl, context);
                break;
            }
        }

        if (rawResponse == null) {
            // Default fallback to local
            return ollamaProvider.sendLlmRequest(orchestrator, prompt, temperature, proxyUrl, context);
        }

        // Record successful usage to improve rating
        if (rawResponse != null && !rawResponse.isEmpty() && orchestrator != null && activeModel != null && !"unknown".equals(activeModel)) {
            try {
                eu.kalafatic.evolution.controller.services.FeedbackService.getInstance().recordUsage(orchestrator, activeModel);
            } catch (Exception e) {
                // Ignore silent failure
            }
        }

        ReasoningProtocol protocol = ReasoningProtocolRegistry.resolve(orchestrator, context);
        return protocol.parse(rawResponse);
    }

    /**
     * Routes the request to the appropriate LLM provider and returns final user-facing content.
     */
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        LlmResponse response = sendLlmRequest(orchestrator, prompt, temperature, proxyUrl, context);
        return response.getContent();
    }

    public String sendNativeEvoRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        return nativeEvoProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    /**
     * System property selecting the default local LLM provider (family convention,
     * consistent with {@code evo.enr.port}, {@code evo.ollama.url}, …). Supported
     * values: {@code ollama} (current default), {@code enr}, {@code native}, or
     * {@code auto}. ENR becomes the default only after explicit configuration and
     * functional validation; Ollama remains fully supported and selectable.
     */
    public static final String SYS_DEFAULT_LOCAL_PROVIDER = "evo.llm.defaultLocalProvider";

    /** Resolves the configured default local provider; {@code auto} when unset. */
    public static String resolveDefaultLocalProvider() {
        String value = System.getProperty(SYS_DEFAULT_LOCAL_PROVIDER);
        return value == null || value.isBlank() ? "auto" : value.trim().toLowerCase();
    }

    /**
     * Whether the given provider/engine selection value refers to the ENR provider.
     *
     * <p>Accepts the canonical {@code EVO Native ENR} label, the legacy
     * {@code ENR-EVO Native} value persisted by earlier AI Commander versions, and the
     * plain {@code ENR}/{@code enr} identifiers, so existing sessions restore onto the
     * unified ENR provider selection.</p>
     */
    public static boolean isEnrEngine(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return v.equals("enr") || v.startsWith("enr") || v.endsWith("enr")
                || v.contains(" enr ");
    }

    /**
     * Normalizes a provider selection value onto the canonical local provider key:
     * {@code "ollama"}, {@code "llama-cpp"} or {@code "enr"}. Returns an empty string
     * for unrecognized values.
     */
    public static String normalizeProviderKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (isEnrEngine(value)) {
            return "enr";
        }
        String v = value.trim().toLowerCase();
        if (v.contains("llama-cpp") || v.contains("llama_cpp") || v.contains("llama.cpp")
                || v.equals("cpp")) {
            return "llama-cpp";
        }
        if (v.contains("ollama")) {
            return "ollama";
        }
        return "";
    }

    /**
     * Discovers the models available through the selected local provider using that
     * provider's own model-listing mechanism:
     * <ul>
     * <li>{@code ollama} — the Ollama registry ({@code /api/tags})</li>
     * <li>{@code llama-cpp} — the local GGUF artifacts resolvable by the llama.cpp
     * integration</li>
     * <li>{@code enr} — the ENR unified model catalog (all registered Ollama models
     * plus native {@code *.evo} and llama.cpp models, with origins retained)</li>
     * </ul>
     *
     * @param providerSelection The provider selection (display label or key).
     * @param orchestrator The orchestrator model (may be null); used to resolve the
     *        provider endpoint where applicable.
     * @return The discovered models, each carrying its inference identifier and origin.
     * @throws IllegalStateException When the selected provider is not registered.
     * @throws IllegalArgumentException For an unrecognized provider selection.
     * @throws Exception When the provider's discovery operation fails.
     */
    public List<LocalModelDescriptor> discoverModels(String providerSelection,
            Orchestrator orchestrator) throws Exception {
        String key = normalizeProviderKey(providerSelection);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Unsupported local provider selection: "
                    + providerSelection);
        }
        List<LocalModelDescriptor> models;
        switch (key) {
            case "enr" -> {
                if (enrProvider == null) {
                    throw new IllegalStateException("ENR provider is not registered (ENR bundle not present)");
                }
                models = enrProvider.discoverModels(orchestrator);
            }
            case "llama-cpp" -> models = eu.kalafatic.evolution.controller.manager.LlamaService
                    .discoverLlamaCppModels();
            default -> models = ollamaProvider.discoverModels(orchestrator);
        }
        if (models == null) {
            models = java.util.List.of();
        }
        eu.kalafatic.utils.log.Log.log("[AI-CMD][DISCOVERY] Provider discovery requested: "
                + providerSelection + " -> key=" + key + ", models=" + models.size());
        return models;
    }

    public String sendLocalRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String configured = resolveDefaultLocalProvider();
        switch (configured) {
            case "enr" -> {
                if (enrProvider == null) {
                    throw new IllegalStateException("ENR is selected as the default local provider ("
                            + SYS_DEFAULT_LOCAL_PROVIDER + "=enr) but the ENR provider is not registered");
                }
                return sendEnrRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
            case "native" -> {
                return sendNativeEvoRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
            case "ollama" -> {
                return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
            default -> {
                // "auto" (default): infer from the request/model — ENR when explicitly
                // requested, native for .evo models, otherwise Ollama. An explicit
                // llama.cpp or Ollama provider selection takes precedence over
                // name-based native detection.
                if (isEnrRequested(orchestrator, context)) {
                    if (enrProvider == null) {
                        throw new IllegalStateException("ENR is selected as the local provider "
                                + "but the ENR provider is not registered (ENR bundle not present)");
                    }
                    return sendEnrRequest(orchestrator, prompt, temperature, proxyUrl, context);
                }
                String overrideKey = "";
                if (context != null && context.getMetadata() != null) {
                    Object engineSelection = context.getMetadata().get("inferenceEngine");
                    if (engineSelection != null) {
                        overrideKey = normalizeProviderKey(String.valueOf(engineSelection));
                    }
                }
                String model = resolveModelName(orchestrator, context);
                if (isNativeEvoModel(model) && !"llama-cpp".equals(overrideKey)
                        && !"ollama".equals(overrideKey)) {
                    return sendNativeEvoRequest(orchestrator, prompt, temperature, proxyUrl, context);
                }
                return ollamaProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
            }
        }
    }

    /**
     * Routes the request through the ENR inference runtime (Phase 6). ENR unifies the local
     * Ollama, llama.cpp and native EVO engines behind one stable inference contract.
     */
    public String sendEnrRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        if (enrProvider == null) {
            throw new IllegalStateException("ENR provider is not registered (ENR bundle not present)");
        }
        return enrProvider.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
    }

    static boolean isEnrRequested(Orchestrator orchestrator, TaskContext context) {
        if (context != null && context.getMetadata() != null) {
            Object provider = context.getMetadata().get("provider");
            if (provider != null && isEnrEngine(String.valueOf(provider))) {
                return true;
            }
            Object engine = context.getMetadata().get("engine");
            if (engine != null && isEnrEngine(String.valueOf(engine))) {
                return true;
            }
            Object inferenceEngine = context.getMetadata().get("inferenceEngine");
            if (inferenceEngine != null && isEnrEngine(String.valueOf(inferenceEngine))) {
                return true;
            }
            String model = resolveModelName(orchestrator, context);
            if (model != null && model.toLowerCase().startsWith("enr:")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNativeEvoModel(String model) {
        if (model == null) return false;
        String m = model.toLowerCase().trim();
        return m.endsWith(".evo") || m.equals("evo") || m.startsWith("evo:") || m.contains("native") || m.contains("forge");
    }

    private static String resolveModelName(Orchestrator orchestrator, TaskContext context) {
        if (context != null && context.getMetadata() != null) {
            String m = (String) context.getMetadata().get("model");
            if (m != null && !m.trim().isEmpty()) {
                return m;
            }
        }
        if (orchestrator != null) {
            if (orchestrator.getLocalModel() != null && !orchestrator.getLocalModel().trim().isEmpty()) {
                return orchestrator.getLocalModel();
            }
            if (orchestrator.getOllama() != null && orchestrator.getOllama().getModel() != null && !orchestrator.getOllama().getModel().trim().isEmpty()) {
                return orchestrator.getOllama().getModel();
            }
        }
        return "local";
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

    public String sendImageRequest(Orchestrator orchestrator, String prompt, String imagePath, TaskContext context) throws Exception {
        if (context != null) context.log("LlmRouter: Routing Multi-Modal Image request.");

        // Multi-modal is currently supported via Ollama or specialized OpenAI-compatible endpoints
        if (orchestrator.getAiMode() == AiMode.REMOTE) {
             return openAiProvider.sendImageRequest(orchestrator, prompt, imagePath, context);
        }
        return ollamaProvider.sendImageRequest(orchestrator, prompt, imagePath, context);
    }

    /**
     * Tests the connection to the LLM.
     */
    public String testConnection(Orchestrator orchestrator, float temperature, String proxyUrl, TaskContext context) throws Exception {
        return sendRequest(orchestrator, "Ping", temperature, proxyUrl, context);
    }
}
