package eu.kalafatic.evolution.controller.orchestration.capability;

import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;

/**
 * Darwin capability exposing the ENR inference engine to capability discovery and
 * selection (Phase 6, WP-6.8).
 *
 * <p>ENR is discovered through the existing Darwin LLM router slot
 * ({@link LlmRouter#isEnrAvailable()}), which the ENR bundle wires at startup. No parallel
 * inference registry or orchestrator is introduced; when ENR is absent this capability
 * reports unavailable and the direct Ollama/native provider paths remain unchanged.</p>
 */
public class EnrFamilyCapability implements ICapability {

    public static final String CAPABILITY_ID = "family.enr.inference";
    public static final String CONTRACT_INFERENCE_GENERATION = "inference.generation";
    public static final String CONTRACT_INFERENCE_STREAMING = "inference.streaming";
    public static final String CONTRACT_INFERENCE_MODEL_LIFECYCLE = "inference.model.lifecycle";

    private CapabilityStatus status = CapabilityStatus.INITIALIZED;

    @Override
    public String getCapabilityId() {
        return CAPABILITY_ID;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public CapabilityStatus getStatus() {
        return status;
    }

    @Override
    public void initialize(CapabilityContext context) {
        this.status = CapabilityStatus.INITIALIZED;
    }

    @Override
    public void start() {
        this.status = CapabilityStatus.STARTED;
    }

    @Override
    public void stop() {
        this.status = CapabilityStatus.STOPPED;
    }

    @Override
    public List<String> getSupportedContracts() {
        return List.of(CONTRACT_INFERENCE_GENERATION, CONTRACT_INFERENCE_STREAMING,
                CONTRACT_INFERENCE_MODEL_LIFECYCLE);
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    public CapabilityHealth getHealth() {
        boolean available;
        try {
            available = LlmRouter.getInstance().isEnrAvailable();
        } catch (Throwable t) {
            available = false;
        }
        return new CapabilityHealth(available ? 1.0 : 0.0,
                available ? "ENR inference runtime wired into the Darwin LLM router"
                        : "ENR inference runtime not present",
                -1);
    }
}
