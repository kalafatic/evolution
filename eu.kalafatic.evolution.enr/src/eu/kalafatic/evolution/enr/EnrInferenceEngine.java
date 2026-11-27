package eu.kalafatic.evolution.enr;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * ENR — Evolution Neural Runtime — stable inference contract (ENR 1.0, Phase 6).
 *
 * <p>ENR is the family inference layer: model discovery, model lifecycle, generation,
 * streaming, provider management and runtime health. It does not own application sessions,
 * coding tasks or repository workflows; those remain with the calling application
 * (EVO / TRYST / DStudio) or the coding harness (DCode).</p>
 *
 * <p>Implementations of this interface unify the existing EVO inference stack — Ollama,
 * llama.cpp and the native EVO engine — behind one published contract. Other inference
 * engines remain usable; this interface is not an exclusive dependency.</p>
 */
public interface EnrInferenceEngine {

    /** Protocol/contract version. */
    String ENGINE_VERSION = "1.0";

    /**
     * Overall runtime health (readiness, version, timestamp).
     */
    EnrHealth health();

    /**
     * Discovers all models known to the runtime across its configured providers.
     */
    List<EnrModel> listModels();

    /**
     * Resolves a single model by its stable id.
     */
    Optional<EnrModel> findModel(String modelId);

    /**
     * Loads the model into memory/VRAM (Ollama keep-alive warm-up, native artifact
     * resolution, llama.cpp path verification).
     */
    boolean loadModel(String modelId);

    /**
     * Unloads the model from memory/VRAM where supported by the underlying engine.
     */
    boolean unloadModel(String modelId);

    /**
     * Deletes the model from its provider registry where supported.
     */
    boolean deleteModel(String modelId);

    /**
     * Pulls/downloads a model from its provider registry, reporting progress.
     */
    boolean pullModel(String modelId, Consumer<EnrProgress> progressConsumer);

    /**
     * Blocking chat generation. The runtime selects the engine from the requested model id.
     */
    EnrChatResponse chat(EnrChatRequest request);

    /**
     * Streaming chat generation. Chunks are delivered as they arrive; the final chunk has
     * {@link EnrChatChunk#done()} == true. Engines without token-level streaming deliver
     * the complete response as one or more bounded chunks.
     */
    void streamChat(EnrChatRequest request, Consumer<EnrChatChunk> chunkConsumer,
            BooleanSupplier cancellationSupplier) throws Exception;

    /**
     * Configured inference providers (Ollama, native EVO, remote endpoints).
     */
    List<EnrProviderInfo> listProviders();

    /**
     * CPU/memory/GPU runtime status.
     */
    EnrRuntimeStatus runtimeStatus();

    /**
     * Capabilities exposed by this runtime to family capability discovery.
     */
    List<EnrCapability> capabilities();

    // ------------------------------------------------------------------
    // Wire/domain records (shared with the ENR HTTP contract, ENR 1.0)
    // ------------------------------------------------------------------

    record EnrHealth(String status, boolean ready, String version, String timestamp) {
        public static EnrHealth up() {
            return new EnrHealth("UP", true, ENGINE_VERSION,
                    java.time.Instant.now().toString());
        }

        /**
         * A degraded runtime is NOT inference-ready: {@code ready} is false so that the
         * {@code /health} endpoint never reports readiness from mere HTTP-server
         * availability when the backing inference provider is unreachable.
         */
        public static EnrHealth degraded(String reason) {
            return new EnrHealth("DEGRADED", false, ENGINE_VERSION,
                    java.time.Instant.now().toString());
        }
    }

    record EnrModel(String id, String name, String provider, String engine,
            long sizeBytes, boolean available, long contextLength, boolean contextLengthKnown) {
    }

    record EnrProgress(String status, long completed, long total) {
    }

    record EnrChatMessage(String role, String content) {
        public static EnrChatMessage system(String content) {
            return new EnrChatMessage("system", content);
        }

        public static EnrChatMessage user(String content) {
            return new EnrChatMessage("user", content);
        }

        public static EnrChatMessage assistant(String content) {
            return new EnrChatMessage("assistant", content);
        }
    }

    record EnrChatRequest(String model, List<EnrChatMessage> messages,
            double temperature, int maxTokens) {
        public EnrChatRequest {
            messages = messages == null ? List.of() : List.copyOf(messages);
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("ENR chat request requires a model id");
            }
        }
    }

    /**
     * Chat response with token usage metadata. Counters are 0 when the
     * backing engine cannot report them; consumers must treat 0 as
     * "unknown", never as zero tokens consumed.
     */
    record EnrChatResponse(String content, String finishReason, String engine,
            String model, long durationMs, long promptTokens, long completionTokens) {
    }

    record EnrChatChunk(String text, boolean done, String engine) {
        public static EnrChatChunk done(String engine) {
            return new EnrChatChunk("", true, engine);
        }
    }

    record EnrProviderInfo(String id, String name, String type, String baseUrl,
            boolean local, boolean available) {
    }

    record EnrGpuStatus(boolean detected, boolean available, String deviceName,
            long totalVramMb, long usedVramMb, long freeVramMb, String driverVersion,
            String message) {
        public static EnrGpuStatus undetected(String message) {
            return new EnrGpuStatus(false, false, "", 0, 0, 0, "",
                    message == null ? "GPU detection not available" : message);
        }
    }

    record EnrRuntimeStatus(boolean ready, int availableProcessors, double systemLoadAverage,
            long maxHeapMb, long usedHeapMb, EnrGpuStatus gpu, List<String> activeEngines) {
    }

    record EnrCapability(String id, String name, String description,
            List<String> features) {
    }
}
