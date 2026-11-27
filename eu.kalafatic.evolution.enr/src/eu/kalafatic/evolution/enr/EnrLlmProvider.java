package eu.kalafatic.evolution.enr;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.ILlmProvider;
import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;
import eu.kalafatic.evolution.forge.model.inference.InferenceProvenance;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Bridges the ENR runtime into the Darwin LLM provider abstraction ({@link ILlmProvider}),
 * making ENR selectable by the existing LlmRouter (model selection, routing and reasoning
 * protocol parsing stay unchanged). Only {@link #sendRequest} is implemented; the default
 * {@link ILlmProvider#sendLlmRequest} reasoning-protocol parsing is reused as-is.
 */
public class EnrLlmProvider implements ILlmProvider {

    private final EnrInferenceEngine engine;

    public EnrLlmProvider(EnrInferenceEngine engine) {
        this.engine = engine == null ? EnrRuntime.instance() : engine;
    }

    @Override
    public List<LocalModelDescriptor> discoverModels(Orchestrator orchestrator) throws Exception {
        List<EnrInferenceEngine.EnrModel> enrModels = engine.listModels();
        List<LocalModelDescriptor> models = new ArrayList<>();
        for (EnrInferenceEngine.EnrModel m : enrModels) {
            // Preserve the ENR-stable id (provider-prefixed) as the inference
            // identifier; the origin is retained so the UI can distinguish
            // [OLLAMA] / [EVO NATIVE] / [LLAMA-CPP] entries.
            models.add(new LocalModelDescriptor(m.id(), m.name(), m.provider(), m.engine(),
                    m.available()));
        }
        eu.kalafatic.utils.log.Log.log("[AI-CMD][DISCOVERY] ENR provider discovery: "
                + models.size() + " models (ollama="
                + models.stream().filter(m -> LocalModelDescriptor.ORIGIN_OLLAMA.equals(m.origin())).count()
                + ", evo-native="
                + models.stream().filter(m -> LocalModelDescriptor.ORIGIN_EVO_NATIVE.equals(m.origin())).count()
                + ", llama-cpp="
                + models.stream().filter(m -> LocalModelDescriptor.ORIGIN_LLAMA_CPP.equals(m.origin())).count()
                + ")");
        return models;
    }

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature,
            String proxyUrl, TaskContext context) {
        String model = resolveModel(orchestrator, context);
        eu.kalafatic.utils.log.Log.log("[AI-CMD][INFERENCE] ENR dispatch: model=" + model);

        // Record the actual inference provenance (provider + model) so the UI can
        // display which provider/model generated the response.
        if (context != null && context.getMetadata() != null) {
            InferenceProvenance prov = (InferenceProvenance) context.getMetadata()
                    .get("currentInferenceProvenance");
            if (prov == null) {
                prov = new InferenceProvenance();
                prov.setInferenceId(java.util.UUID.randomUUID().toString());
                prov.setSessionId(context.getSessionId() != null ? context.getSessionId() : "Default");
                context.getMetadata().put("currentInferenceProvenance", prov);
            }
            prov.setRuntimeProvider("ENR");
            prov.setBackend("ENR");
            prov.setActualEngine("EnrRuntime");
            prov.setLoadedModelIdentity(model);
        }

        List<EnrInferenceEngine.EnrChatMessage> messages = new ArrayList<>();
        messages.add(EnrInferenceEngine.EnrChatMessage.user(
                prompt == null ? "" : prompt));
        try {
            EnrInferenceEngine.EnrChatResponse response = engine.chat(
                    new EnrInferenceEngine.EnrChatRequest(model, messages, temperature, 512));
            return response.content();
        } catch (RuntimeException e) {
            eu.kalafatic.utils.log.Log.log("[AI-CMD][INFERENCE] ENR inference FAILED: model="
                    + model + " : " + e.getMessage());
            throw e;
        }
    }

    private static String resolveModel(Orchestrator orchestrator, TaskContext context) {
        if (context != null && context.getMetadata() != null) {
            Object model = context.getMetadata().get("model");
            if (model != null && !String.valueOf(model).isBlank()) {
                return String.valueOf(model);
            }
        }
        if (orchestrator != null) {
            if (orchestrator.getLocalModel() != null
                    && !orchestrator.getLocalModel().isBlank()) {
                return orchestrator.getLocalModel();
            }
            if (orchestrator.getOllama() != null
                    && orchestrator.getOllama().getModel() != null
                    && !orchestrator.getOllama().getModel().isBlank()) {
                return orchestrator.getOllama().getModel();
            }
        }
        return "llama3.2:3b";
    }
}
