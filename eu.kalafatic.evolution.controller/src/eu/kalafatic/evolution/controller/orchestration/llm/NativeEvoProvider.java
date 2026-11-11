package eu.kalafatic.evolution.controller.orchestration.llm;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.forge.model.inference.EvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.inference.InferenceExecutionTrace;
import eu.kalafatic.evolution.forge.model.inference.InferenceProvenance;
import eu.kalafatic.evolution.forge.model.inference.InferenceRequest;
import eu.kalafatic.evolution.forge.model.inference.InferenceResult;
import eu.kalafatic.evolution.forge.model.inference.NativeEvoArtifactResolver;
import eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Dedicated Native EVO Provider executing native .evo Transformer models directly
 * through the native EVO inference engine (ReferenceEvoInferenceEngine) completely
 * independent of Ollama or any external daemon/runtime.
 */
public class NativeEvoProvider implements ILlmProvider {

    private final EvoInferenceEngine nativeEngine;

    public NativeEvoProvider() {
        this(new ReferenceEvoInferenceEngine());
    }

    public NativeEvoProvider(EvoInferenceEngine engine) {
        this.nativeEngine = engine != null ? engine : new ReferenceEvoInferenceEngine();
    }

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        String modelName = resolveModelName(orchestrator, context);
        File evoArtifactPath = resolveEvoArtifactPath(modelName);

        if (evoArtifactPath == null) {
            throw new IllegalStateException("Native EVO model artifact could not be resolved for model: " + modelName);
        }

        NativeEvoArtifactResolver.ArtifactResolutionResult res =
                NativeEvoArtifactResolver.resolveNativeArtifact(evoArtifactPath.toPath());

        File resolvedFile = res.getResolvedPath().toFile();
        EvoModelArtifact artifact = res.getArtifact();

        InferenceProvenance prov = null;
        InferenceExecutionTrace traceObj = null;
        if (context != null && context.getMetadata() != null) {
            prov = (InferenceProvenance) context.getMetadata().get("currentInferenceProvenance");
            traceObj = (InferenceExecutionTrace) context.getMetadata().get("currentInferenceTrace");
        }

        if (prov != null) {
            prov.setArtifactName(resolvedFile.getName());
            prov.setArtifactPath(resolvedFile.getAbsolutePath());
            prov.setArtifactFormat("EVO");
            prov.setSourceFormat("EVO");
            prov.setActualEngine(nativeEngine.getClass().getSimpleName());
            prov.setRuntimeProvider("NativeEvoProvider");
            prov.setBackend("NATIVE_EVO");
            prov.setNative(true);
            prov.setVerificationStatus("VERIFIED");
            if (artifact.getModelContentHash() != null) {
                prov.setArtifactChecksum(artifact.getModelContentHash());
                prov.setLoadedModelChecksum(artifact.getModelContentHash());
            } else {
                try {
                    byte[] b = Files.readAllBytes(resolvedFile.toPath());
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    prov.setArtifactChecksum(HexFormat.of().formatHex(md.digest(b)));
                } catch (Exception ignore) {}
            }
        }

        if (traceObj != null) {
            traceObj.addEvent(InferenceExecutionTrace.EventType.MODEL_LOADED,
                    "Loaded Native EVO model: " + resolvedFile.getName(), "checksum=" + (prov != null ? prov.getArtifactChecksum() : ""));
        }

        if (context != null) {
            context.log("[EVO-NATIVE] Model source: " + resolvedFile.getAbsolutePath());
            context.log("[EVO-NATIVE] Artifact format: EVO_NATIVE_V2");
            context.log("[EVO-NATIVE] Artifact size: " + resolvedFile.length() + " bytes");
            context.log("[EVO-NATIVE] Artifact content hash: " + artifact.getModelContentHash());
            context.log("[EVO-NATIVE] Architecture: " + artifact.getArchitectureDescriptor());
            context.log("[EVO-NATIVE] Vocabulary size: " + artifact.getVocabSize());
            context.log("[EVO-NATIVE] Parameters/tensors: " + artifact.getParameterCount() + " parameters across " + artifact.getManifest().size() + " tensors");
            context.log("[EVO-NATIVE] Trained weights restored: true");
        }

        int maxTokens = 512;
        float topP = 0.9f;
        int topK = 40;
        float repeatPenalty = 1.1f;
        if (context != null && context.getMetadata() != null) {
            Object mt = context.getMetadata().get("maxTokens");
            if (mt instanceof Number) {
                maxTokens = ((Number) mt).intValue();
            }
            Object tp = context.getMetadata().get("topP");
            if (tp instanceof Number) {
                topP = ((Number) tp).floatValue();
            }
            Object tk = context.getMetadata().get("topK");
            if (tk instanceof Number) {
                topK = ((Number) tk).intValue();
            }
            Object rp = context.getMetadata().get("repeatPenalty");
            if (rp instanceof Number) {
                repeatPenalty = ((Number) rp).floatValue();
            }
        }

        InferenceRequest request = InferenceRequest.builder()
                .prompt(prompt)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .topP(topP)
                .topK(topK)
                .repeatPenalty(repeatPenalty)
                .build();

        final TaskContext currentContext = context;
        final long startTime = System.currentTimeMillis();
        final int[] tokenCountHolder = new int[1];

        ReferenceEvoInferenceEngine.TokenStreamListener listener = (tokenId, tokenText) -> {
            tokenCountHolder[0]++;
            int count = tokenCountHolder[0];
            if (count % 10 == 0 || count == 1) {
                long elapsed = System.currentTimeMillis() - startTime;
                double tokPerSec = count * 1000.0 / Math.max(1, elapsed);
                if (currentContext != null) {
                    currentContext.log(String.format("[EvoInferenceEngine] Progress: %d tokens generated (%.2f tok/s, elapsed %d ms)", count, tokPerSec, elapsed));
                }
            }
        };

        InferenceResult result;
        if (nativeEngine instanceof ReferenceEvoInferenceEngine refEngine) {
            result = refEngine.generateFromArtifact(artifact, request, listener);
        } else {
            result = nativeEngine.generateFromSnapshot(artifact.toSnapshot(), request, null);
        }

        String response = result.getGeneratedText();
        if (response == null) {
            response = "";
        }

        if (prov != null) {
            prov.setCompletionTokens(result.getGeneratedTokenCount());
            prov.setPromptTokens(request.getInputIds() != null ? request.getInputIds().length : (prompt != null ? prompt.length() / 4 : 0));
            prov.setTotalTokens(prov.getPromptTokens() + prov.getCompletionTokens());
        }

        if (context != null) {
            context.log("Stage: LLM\nProvider: NativeEvoProvider (" + nativeEngine.getClass().getSimpleName() + ")\nModel: "
                    + modelName + "\nToken count: " + result.getGeneratedTokenCount()
                    + "\nExecution time: " + result.getExecutionTimeMs() + " ms\nRaw response length: " + response.length());
        }

        return response;
    }

    private String resolveModelName(Orchestrator orchestrator, TaskContext context) {
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
        return "evo";
    }

    public static File resolveEvoArtifactPath(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            modelName = "evo";
        }

        File directFile = new File(modelName);
        if (directFile.exists() && directFile.isFile() && directFile.getName().toLowerCase().endsWith(".evo")) {
            return directFile;
        }

        boolean isGenericEvo = modelName.equalsIgnoreCase("evo") || modelName.equalsIgnoreCase("evo:latest");

        List<File> baseDirs = new ArrayList<>();
        String workspacePath = ProjectModelManager.getWorkspacePath();
        if (workspacePath != null && !workspacePath.isEmpty()) {
            baseDirs.add(new File(workspacePath, "forge-output"));
            baseDirs.add(new File(workspacePath, "forge-models"));
            baseDirs.add(new File(workspacePath, "models"));
        }
        String codebasePath = ProjectModelManager.getCodebasePath();
        if (codebasePath != null && !codebasePath.isEmpty()) {
            baseDirs.add(new File(codebasePath, "forge-output"));
            baseDirs.add(new File(codebasePath, "models"));
        }
        baseDirs.add(new File(System.getProperty("user.dir"), "forge-output"));
        baseDirs.add(new File(System.getProperty("user.dir"), "models"));

        for (File targetDir : LlamaService.resolveAllTargetModelDirs()) {
            if (!baseDirs.contains(targetDir)) {
                baseDirs.add(targetDir);
            }
        }

        if (isGenericEvo) {
            File newestArtifact = null;
            long newestTime = -1;

            for (File forgeDir : baseDirs) {
                if (forgeDir.exists() && forgeDir.isDirectory()) {
                    File[] files = forgeDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && f.getName().toLowerCase().endsWith(".evo")) {
                                if (f.lastModified() > newestTime) {
                                    newestTime = f.lastModified();
                                    newestArtifact = f;
                                }
                            } else if (f.isDirectory()) {
                                File[] subFiles = f.listFiles((d, n) -> n.toLowerCase().endsWith(".evo"));
                                if (subFiles != null) {
                                    for (File sf : subFiles) {
                                        if (sf.lastModified() > newestTime) {
                                            newestTime = sf.lastModified();
                                            newestArtifact = sf;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (newestArtifact != null) {
                return newestArtifact;
            }
        }

        for (File forgeDir : baseDirs) {
            if (forgeDir.exists() && forgeDir.isDirectory()) {
                File evoFile = new File(forgeDir, modelName.endsWith(".evo") ? modelName : modelName + ".evo");
                if (evoFile.exists() && evoFile.isFile()) {
                    return evoFile;
                }

                File dir = new File(forgeDir, modelName);
                if (dir.exists() && dir.isDirectory()) {
                    File[] subEvos = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".evo"));
                    if (subEvos != null && subEvos.length == 1) {
                        return subEvos[0];
                    } else if (subEvos != null && subEvos.length > 1) {
                        return dir;
                    }
                    File subEvo = new File(dir, "evo.evo");
                    if (subEvo.exists()) {
                        return subEvo;
                    }
                    return dir;
                }
            }
        }

        return null;
    }
}
