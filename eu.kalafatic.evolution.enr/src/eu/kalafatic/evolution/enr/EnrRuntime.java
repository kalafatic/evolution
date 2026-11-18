package eu.kalafatic.evolution.enr;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.NativeEvoProvider;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.NativeEvoArtifactResolver;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.utils.log.Log;

/**
 * Default ENR runtime implementation (ENR 1.0, Phase 6).
 *
 * <p>Unifies the existing EVO inference stack behind the stable {@link EnrInferenceEngine}
 * contract. No new inference path or model registry is introduced: model discovery and
 * lifecycle operations delegate to {@link OllamaService}/{@link OllamaManager}, native .evo
 * generation delegates to {@link NativeEvoProvider} (the native EVO engine), llama.cpp
 * execution delegates to {@link LlamaService}, and GPU status reuses the existing
 * eu.kalafatic.evolution.gpu detector when present on the classpath.</p>
 */
public class EnrRuntime implements EnrInferenceEngine {

    public static final String PROVIDER_OLLAMA_ID = "ollama";
    public static final String PROVIDER_NATIVE_ID = "evo-native";
    public static final String PROVIDER_LLAMACPP_ID = "llama-cpp";

    public static final String ENGINE_OLLAMA = "ollama";
    public static final String ENGINE_NATIVE = "evo native";
    public static final String ENGINE_LLAMACPP = "llama-cpp";

    public static final String SYS_OLLAMA_URL = "evo.ollama.url";
    public static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    private static final EnrRuntime INSTANCE = new EnrRuntime();

    private final NativeEvoProvider nativeEvoProvider = new NativeEvoProvider();

    private EnrRuntime() {
    }

    public static EnrRuntime instance() {
        return INSTANCE;
    }

    public static String resolveOllamaUrl() {
        String property = System.getProperty(SYS_OLLAMA_URL);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String env = System.getenv("EVO_OLLAMA_URL");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return DEFAULT_OLLAMA_URL;
    }

    @Override
    public EnrHealth health() {
        boolean ollamaUp = false;
        try {
            ollamaUp = ollamaService("").ping();
        } catch (Throwable ignored) {
            // provider probe failures do not fail overall health
        }
        return ollamaUp ? EnrHealth.up() : EnrHealth.degraded("Ollama provider unreachable; "
                + "native and remote providers remain available");
    }

    @Override
    public List<EnrModel> listModels() {
        List<EnrModel> models = new ArrayList<>();
        try {
            for (OllamaModel model : ollamaService("").loadModels()) {
                // Ollama tag discovery does not report context length; the
                // conservative default (4096) is advertised with
                // contextLengthKnown=false so consumers never mistake it
                // for a verified limit.
                models.add(new EnrModel("ollama:" + model.getName(), model.getName(),
                        PROVIDER_OLLAMA_ID, ENGINE_OLLAMA, model.getSize(), true, 4096, false));
            }
        } catch (Throwable e) {
            log("ENR Ollama model discovery failed: " + e.getMessage());
        }
        for (File evoFile : discoverNativeArtifacts(".evo")) {
            models.add(new EnrModel("evo-native:" + evoFile.getName(), evoFile.getName(),
                    PROVIDER_NATIVE_ID, ENGINE_NATIVE, evoFile.length(), true, 0, false));
        }
        for (File ggufFile : discoverNativeArtifacts(".gguf")) {
            models.add(new EnrModel("llama-cpp:" + ggufFile.getName(), ggufFile.getName(),
                    PROVIDER_LLAMACPP_ID, ENGINE_LLAMACPP, ggufFile.length(), true, 0, false));
        }
        models.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
        return models;
    }

    @Override
    public Optional<EnrModel> findModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return Optional.empty();
        }
        return listModels().stream().filter(m -> m.id().equals(modelId)
                || m.name().equals(modelId)).findFirst();
    }

    @Override
    public boolean loadModel(String modelId) {
        String engine = engineOf(modelId);
        if (ENGINE_OLLAMA.equals(engine)) {
            try {
                OllamaService service = ollamaService(stripProviderPrefix(modelId));
                return service.ping() && service.loadModels().stream()
                        .anyMatch(m -> m.getName().equals(stripProviderPrefix(modelId)));
            } catch (Throwable e) {
                log("ENR load model failed: " + e.getMessage());
                return false;
            }
        }
        if (ENGINE_NATIVE.equals(engine)) {
            return NativeEvoProvider.resolveEvoArtifactPath(stripProviderPrefix(modelId)) != null;
        }
        if (ENGINE_LLAMACPP.equals(engine)) {
            return LlamaService.resolveEvoModelPath(stripProviderPrefix(modelId)) != null;
        }
        return false;
    }

    @Override
    public boolean unloadModel(String modelId) {
        String engine = engineOf(modelId);
        if (ENGINE_OLLAMA.equals(engine)) {
            return ollamaService("").unloadModel(stripProviderPrefix(modelId));
        }
        // Native and llama.cpp engines are process-local: unload is a no-op that succeeds.
        return findModel(modelId).isPresent();
    }

    @Override
    public boolean deleteModel(String modelId) {
        String engine = engineOf(modelId);
        if (ENGINE_OLLAMA.equals(engine)) {
            return ollamaService("").deleteModel(stripProviderPrefix(modelId));
        }
        throw new UnsupportedOperationException("Model deletion is not supported for engine '"
                + engine + "' (provider-managed artifact)");
    }

    @Override
    public boolean pullModel(String modelId, Consumer<EnrProgress> progressConsumer) {
        String engine = engineOf(modelId);
        if (!ENGINE_OLLAMA.equals(engine)) {
            throw new UnsupportedOperationException("Model pull is only supported for "
                    + "Ollama-registered models (engine '" + engine + "')");
        }
        try {
            ollamaService("").pullModel(stripProviderPrefix(modelId),
                    progress -> {
                        if (progressConsumer != null) {
                            progressConsumer.accept(new EnrProgress(progress.status(),
                                    progress.completed(), progress.total()));
                        }
                    });
            return true;
        } catch (Exception e) {
            log("ENR pull model failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public EnrChatResponse chat(EnrChatRequest request) {
        long start = System.currentTimeMillis();
        String modelId = request.model();
        String engine = engineOf(modelId);
        String modelName = stripProviderPrefix(modelId);
        String prompt = toPrompt(request);
        try {
            if (ENGINE_NATIVE.equals(engine)) {
                TaskContext context = contextFor(modelName);
                String content = nativeEvoProvider.sendRequest(null, prompt,
                        (float) request.temperature(), null, context);
                return new EnrChatResponse(content, "stop", ENGINE_NATIVE, modelName,
                        System.currentTimeMillis() - start, 0, 0);
            }
            if (ENGINE_LLAMACPP.equals(engine)) {
                File path = LlamaService.resolveEvoModelPath(modelName);
                if (path == null) {
                    throw new IllegalStateException("llama.cpp model artifact not found: "
                            + modelName);
                }
                LlamaService service = new LlamaService(modelName, path.getAbsolutePath());
                // Honor the request's sampling configuration instead of ignoring it.
                if (request.temperature() > 0) {
                    service.setTemperature((float) request.temperature());
                }
                if (request.maxTokens() > 2048) {
                    service.setContextLength(request.maxTokens() + 512);
                }
                String content = service.generate(prompt);
                return new EnrChatResponse(content, "stop", ENGINE_LLAMACPP, modelName,
                        System.currentTimeMillis() - start, 0, 0);
            }
            OllamaService service = ollamaService(modelName);
            OllamaService.OllamaChatResult result = service.chatWithUsage(prompt, "enr");
            return new EnrChatResponse(result.content(), "stop", ENGINE_OLLAMA, modelName,
                    System.currentTimeMillis() - start, result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            throw new IllegalStateException("ENR chat failed for model '" + modelId
                    + "' (engine " + engine + "): " + e.getMessage(), e);
        }
    }

    @Override
    public void streamChat(EnrChatRequest request, Consumer<EnrChatChunk> chunkConsumer,
            BooleanSupplier cancellationSupplier) throws Exception {
        String modelId = request.model();
        String engine = engineOf(modelId);
        String modelName = stripProviderPrefix(modelId);
        String prompt = toPrompt(request);

        if (ENGINE_NATIVE.equals(engine)) {
            File artifactPath = NativeEvoProvider.resolveEvoArtifactPath(modelName);
            if (artifactPath == null) {
                throw new IllegalStateException("Native EVO model artifact not found: "
                        + modelName);
            }
            NativeEvoArtifactResolver.ArtifactResolutionResult resolution =
                    NativeEvoArtifactResolver.resolveNativeArtifact(artifactPath.toPath());
            java.util.concurrent.atomic.AtomicBoolean nativeCancel = new java.util.concurrent.atomic.AtomicBoolean(false);
            InferenceRequest inferenceRequest = InferenceRequest.builder()
                    .prompt(prompt)
                    .maxTokens(request.maxTokens())
                    .temperature((float) request.temperature())
                    .cancellationFlag(nativeCancel)
                    .build();
            ReferenceEvoInferenceEngine engineImpl = new ReferenceEvoInferenceEngine();
            engineImpl.generateFromArtifact(resolution.getArtifact(), inferenceRequest,
                    (tokenId, tokenText) -> {
                        // Propagate the caller's cancellation signal into the engine.
                        if (cancellationSupplier != null && cancellationSupplier.getAsBoolean()) {
                            nativeCancel.set(true);
                        }
                        if (tokenText != null && !tokenText.isEmpty()) {
                            chunkConsumer.accept(new EnrChatChunk(tokenText, false,
                                    ENGINE_NATIVE));
                        }
                    });
            chunkConsumer.accept(EnrChatChunk.done(ENGINE_NATIVE));
            return;
        }

        if (ENGINE_OLLAMA.equals(engine)) {
            OllamaService service = ollamaService(modelName);
            service.streamChat(prompt, "enr-" + UUID.randomUUID().toString().substring(0, 8),
                    text -> chunkConsumer.accept(new EnrChatChunk(text, false, ENGINE_OLLAMA)));
            chunkConsumer.accept(EnrChatChunk.done(ENGINE_OLLAMA));
            return;
        }

        // llama.cpp has no token-level streaming surface: deliver the complete response.
        EnrChatResponse response = chat(request);
        String text = response.content() == null ? "" : response.content();
        int chunkSize = 512;
        for (int offset = 0; offset < text.length(); offset += chunkSize) {
            if (cancellationSupplier != null && cancellationSupplier.getAsBoolean()) {
                return;
            }
            chunkConsumer.accept(new EnrChatChunk(text.substring(offset,
                    Math.min(text.length(), offset + chunkSize)), false, ENGINE_LLAMACPP));
        }
        chunkConsumer.accept(EnrChatChunk.done(ENGINE_LLAMACPP));
    }

    @Override
    public List<EnrProviderInfo> listProviders() {
        List<EnrProviderInfo> providers = new ArrayList<>();
        String ollamaUrl = resolveOllamaUrl();
        boolean ollamaUp = false;
        try {
            ollamaUp = ollamaService("").ping();
        } catch (Throwable ignored) {
        }
        providers.add(new EnrProviderInfo(PROVIDER_OLLAMA_ID, "Ollama", "OLLAMA",
                ollamaUrl, true, ollamaUp));
        providers.add(new EnrProviderInfo(PROVIDER_NATIVE_ID, "Native EVO Engine",
                "NATIVE_EVO", "", true, true));
        providers.add(new EnrProviderInfo(PROVIDER_LLAMACPP_ID, "llama.cpp",
                "LLAMACPP", "", true, LlamaService.resolveLlamaCppLibDir() != null));
        return providers;
    }

    @Override
    public EnrRuntimeStatus runtimeStatus() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        List<String> engines = new ArrayList<>();
        try {
            if (ollamaService("").ping()) {
                engines.add(ENGINE_OLLAMA);
            }
        } catch (Throwable ignored) {
        }
        if (!discoverNativeArtifacts(".evo").isEmpty()) {
            engines.add(ENGINE_NATIVE);
        }
        if (!discoverNativeArtifacts(".gguf").isEmpty()) {
            engines.add(ENGINE_LLAMACPP);
        }
        return new EnrRuntimeStatus(true, os.getAvailableProcessors(),
                os.getSystemLoadAverage(),
                runtime.maxMemory() / (1024 * 1024),
                (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
                detectGpu(), engines);
    }

    @Override
    public List<EnrCapability> capabilities() {
        return List.of(
                new EnrCapability("ENR_INFERENCE", "Local LLM Inference",
                        "Blocking and streaming generation across Ollama, llama.cpp and "
                                + "the native EVO engine", List.of("chat", "streamChat",
                                        "modelDiscovery", "modelLifecycle", "health")),
                new EnrCapability("ENR_MODEL_MANAGEMENT", "Model Discovery & Lifecycle",
                        "Model discovery, load, unload, pull and delete", List.of(
                                "discovery", "load", "unload", "pull", "delete")),
                new EnrCapability("ENR_RUNTIME_MONITORING", "Runtime Health Monitoring",
                        "CPU, memory and GPU runtime status", List.of("cpu", "memory",
                                "gpu", "providers")));
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private OllamaService ollamaService(String model) {
        OllamaService cached = OllamaManager.getInstance().getService(resolveOllamaUrl());
        if (model == null || model.isBlank()) {
            return cached;
        }
        return new OllamaService(resolveOllamaUrl(), model);
    }

    private static String engineOf(String modelId) {
        if (modelId == null) {
            return ENGINE_OLLAMA;
        }
        if (modelId.startsWith("evo-native:")) {
            return ENGINE_NATIVE;
        }
        if (modelId.startsWith("llama-cpp:")) {
            return ENGINE_LLAMACPP;
        }
        if (modelId.startsWith("ollama:")) {
            return ENGINE_OLLAMA;
        }
        return LlamaService.detectInferenceEngine(modelId);
    }

    private static String stripProviderPrefix(String modelId) {
        if (modelId == null) {
            return "";
        }
        int colon = modelId.indexOf(':');
        return colon > 0 ? modelId.substring(colon + 1) : modelId;
    }

    private static String toPrompt(EnrChatRequest request) {
        StringBuilder prompt = new StringBuilder();
        for (EnrChatMessage message : request.messages()) {
            String role = message.role() == null ? "user" : message.role();
            switch (role) {
                case "system" -> prompt.append("System: ").append(message.content()).append('\n');
                case "assistant" -> prompt.append("Assistant: ").append(message.content())
                        .append('\n');
                default -> prompt.append("User: ").append(message.content()).append('\n');
            }
        }
        String text = prompt.toString().trim();
        return text.isEmpty() ? "Respond." : text;
    }

    private static TaskContext contextFor(String modelName) {
        TaskContext context = new TaskContext(null, new File(System.getProperty("user.dir",
                ".")));
        context.getMetadata().put("model", modelName);
        return context;
    }

    private static List<File> discoverNativeArtifacts(String extension) {
        Map<String, File> found = new LinkedHashMap<>();
        List<File> dirs = new ArrayList<>(LlamaService.resolveAllTargetModelDirs());
        addModelDir(dirs, ProjectModelManager.getWorkspacePath());
        addModelDir(dirs, ProjectModelManager.getCodebasePath());
        addModelDir(dirs, System.getProperty("user.dir", "."));
        for (File dir : dirs) {
            if (dir == null || !dir.isDirectory()) {
                continue;
            }
            File[] direct = dir.listFiles((d, name) -> name.toLowerCase()
                    .endsWith(extension));
            if (direct != null) {
                for (File file : direct) {
                    found.putIfAbsent(file.getName(), file);
                }
            }
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!child.isDirectory()) {
                        continue;
                    }
                    File[] nested = child.listFiles((d, name) -> name.toLowerCase()
                            .endsWith(extension));
                    if (nested != null) {
                        for (File file : nested) {
                            found.putIfAbsent(file.getName(), file);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    private static void addModelDir(List<File> dirs, String base) {
        if (base == null || base.isBlank()) {
            return;
        }
        dirs.add(new File(base, "forge-output"));
        dirs.add(new File(base, "forge-models"));
        dirs.add(new File(base, "models"));
    }

    private EnrGpuStatus detectGpu() {
        try {
            Class<?> detectorClass = Class.forName(
                    "eu.kalafatic.evolution.gpu.diag.NvidiaGpuDetector");
            Object detector = detectorClass.getDeclaredConstructor().newInstance();
            Object device = detectorClass.getMethod("detectGpu").invoke(detector);
            boolean available = (Boolean) device.getClass().getMethod("isAvailable")
                    .invoke(device);
            String name = String.valueOf(device.getClass().getMethod("getDeviceName")
                    .invoke(device));
            long total = (Long) device.getClass().getMethod("getTotalVramMb").invoke(device);
            long used = (Long) device.getClass().getMethod("getUsedVramMb").invoke(device);
            long free = (Long) device.getClass().getMethod("getFreeVramMb").invoke(device);
            String driver = String.valueOf(device.getClass().getMethod("getDriverVersion")
                    .invoke(device));
            return new EnrGpuStatus(true, available, name, total, used, free, driver,
                    available ? "NVIDIA GPU detected" : "NVIDIA GPU present but unavailable");
        } catch (ClassNotFoundException e) {
            return EnrGpuStatus.undetected("GPU detector bundle not present ("
                    + "eu.kalafatic.evolution.gpu)");
        } catch (Throwable t) {
            return EnrGpuStatus.undetected("GPU detection failed: " + t.getMessage());
        }
    }

    private static void log(String message) {
        try {
            Log.log("[ENR] " + message);
        } catch (Throwable ignored) {
        }
    }
}
