package eu.kalafatic.evolution.controller.orchestration.llm;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Registry and resolver for model-independent reasoning protocols.
 */
public class ReasoningProtocolRegistry {

    private static final ReasoningProtocol DEFAULT_TAG_PROTOCOL = new TagBasedReasoningProtocol();
    private static final ReasoningProtocol SEPARATED_FIELD_PROTOCOL = new SeparatedFieldReasoningProtocol();
    private static final ReasoningProtocol STANDARD_PROTOCOL = new StandardReasoningProtocol();

    /**
     * Resolves appropriate ReasoningProtocol based on model name, metadata, or provider context.
     */
    public static ReasoningProtocol resolve(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return DEFAULT_TAG_PROTOCOL;
        }

        String lower = modelName.toLowerCase();

        // Model capability / protocol resolution without hardcoded provider hacks
        if (lower.contains("deepseek") || lower.contains("r1") || lower.contains("qwen") || lower.contains("think") || lower.contains("reasoning") || lower.contains("qwq")) {
            return DEFAULT_TAG_PROTOCOL;
        }

        return DEFAULT_TAG_PROTOCOL;
    }

    public static ReasoningProtocol resolve(Orchestrator orchestrator, TaskContext context) {
        if (orchestrator != null) {
            String activeModel = orchestrator.getLocalModel();
            if (activeModel == null || activeModel.isEmpty()) {
                activeModel = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getModel() : null;
            }
            if (activeModel != null) {
                return resolve(activeModel);
            }
        }
        return DEFAULT_TAG_PROTOCOL;
    }

    public static ReasoningProtocol getTagProtocol() {
        return DEFAULT_TAG_PROTOCOL;
    }

    public static ReasoningProtocol getSeparatedFieldProtocol() {
        return SEPARATED_FIELD_PROTOCOL;
    }

    public static ReasoningProtocol getStandardProtocol() {
        return STANDARD_PROTOCOL;
    }
}
