package eu.kalafatic.evolution.controller.manager.removal;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Service enforcing the 9-stage model removal lifecycle.
 */
public class ModelRemovalService {

    private static final ModelRemovalService INSTANCE = new ModelRemovalService();

    private ModelRemovalService() {}

    public static ModelRemovalService getInstance() {
        return INSTANCE;
    }

    /**
     * Executes batch removal for a list of selected models.
     *
     * @param orchestrator Orchestrator EMF model instance.
     * @param selectedModels List of AIProvider items selected for removal.
     * @return List of ModelRemovalResult items for each model.
     */
    public List<ModelRemovalResult> removeModels(Orchestrator orchestrator, List<AIProvider> selectedModels) {
        List<ModelRemovalResult> results = new ArrayList<>();
        if (selectedModels == null || selectedModels.isEmpty()) {
            return results;
        }

        for (AIProvider provider : selectedModels) {
            String removalId = UUID.randomUUID().toString().substring(0, 8);
            ModelRemovalResult result = removeSingleModel(removalId, orchestrator, provider);
            results.add(result);
        }

        return results;
    }

    /**
     * Executes the 9-stage removal lifecycle for a single model.
     */
    public ModelRemovalResult removeSingleModel(String removalId, Orchestrator orchestrator, AIProvider provider) {
        String rawName = provider != null ? provider.getName() : "unknown";
        String modelType = determineModelType(provider);
        String endpointUrl = resolveEndpointUrl(orchestrator, provider);

        ModelRemovalResult result = new ModelRemovalResult(removalId, rawName, modelType, endpointUrl);
        result.log("Starting removal lifecycle for model '" + rawName + "' (Type: " + modelType + ", Endpoint: " + endpointUrl + ")");

        // Stage 1: Resolve Identity & Ownership
        String modelName = rawName;
        if (modelName == null || modelName.isBlank()) {
            result.addStepResult(new ModelRemovalStepResult("STAGE_1_RESOLVE_IDENTITY", null, false, "Model name is null or blank", null));
            result.setStatus(ModelRemovalStatus.FAILED);
            return result;
        }

        Path directArtifactPath = resolveDirectArtifactPath(provider);
        result.addStepResult(new ModelRemovalStepResult("STAGE_1_RESOLVE_IDENTITY", directArtifactPath, true,
                "Resolved model type: " + modelType + ", Direct artifact path: " + (directArtifactPath != null ? directArtifactPath : "None"), null));

        // Stage 2: Validate Operations
        if ("REMOTE".equals(modelType)) {
            result.log("Remote model provider selected. Local filesystem/remote model deletion is unsupported.");
            // For remote providers, we only remove provider config in Stage 6
        }

        // Stage 3: Unload
        boolean unloadSuccess = true;
        if ("OLLAMA".equals(modelType)) {
            OllamaService ollamaService = OllamaManager.getInstance().getService(endpointUrl);
            if (ollamaService != null) {
                OllamaService.OllamaOperationResult unloadRes = ollamaService.unloadModelEx(modelName);
                if (unloadRes.success()) {
                    result.addStepResult(new ModelRemovalStepResult("STAGE_3_UNLOAD", null, true, "Unloaded model from Ollama memory/VRAM", null));
                } else {
                    unloadSuccess = false;
                    result.addStepResult(new ModelRemovalStepResult("STAGE_3_UNLOAD", null, false, "Failed to unload model from Ollama: " + unloadRes.message(), unloadRes.exception()));
                }
            }
        } else {
            result.addStepResult(new ModelRemovalStepResult("STAGE_3_UNLOAD", null, true, "Unload not required or unsupported for model type " + modelType, null));
        }

        // Stage 4: Remove Runtime Registration
        boolean runtimeRegistrationRemoved = true;
        if ("OLLAMA".equals(modelType)) {
            OllamaService ollamaService = OllamaManager.getInstance().getService(endpointUrl);
            if (ollamaService != null) {
                OllamaService.OllamaOperationResult deleteRes = ollamaService.deleteModelEx(modelName);
                if (deleteRes.success()) {
                    result.addStepResult(new ModelRemovalStepResult("STAGE_4_REMOVE_RUNTIME_REGISTRATION", null, true, "Removed model from Ollama registry", null));
                } else {
                    // Check if model was already absent
                    boolean stillPresent = isModelPresentInOllama(ollamaService, modelName);
                    if (!stillPresent) {
                        result.addStepResult(new ModelRemovalStepResult("STAGE_4_REMOVE_RUNTIME_REGISTRATION", null, true, "Model was already absent from Ollama registry", null));
                    } else {
                        runtimeRegistrationRemoved = false;
                        result.addStepResult(new ModelRemovalStepResult("STAGE_4_REMOVE_RUNTIME_REGISTRATION", null, false, "Failed to delete model from Ollama registry: " + deleteRes.message(), deleteRes.exception()));
                    }
                }
            }
        } else {
            result.addStepResult(new ModelRemovalStepResult("STAGE_4_REMOVE_RUNTIME_REGISTRATION", null, true, "Runtime registration removal not required for " + modelType, null));
        }

        // Stage 5: Remove Owned Artifacts
        boolean artifactsRemoved = true;
        if (directArtifactPath != null && ("EVO_NATIVE".equals(modelType) || "STANDALONE_GGUF".equals(modelType) || "OLLAMA".equals(modelType))) {
            if (isValidOwnedArtifactPath(directArtifactPath, provider)) {
                if (Files.isDirectory(directArtifactPath)) {
                    FileDeletionUtil.DirectoryDeletionResult dirRes = FileDeletionUtil.deleteDirectoryRecursive(directArtifactPath);
                    if (dirRes.success()) {
                        result.addStepResult(new ModelRemovalStepResult("STAGE_5_REMOVE_OWNED_ARTIFACTS", directArtifactPath, true, "Successfully deleted owned directory artifact", null));
                    } else {
                        artifactsRemoved = false;
                        result.addStepResult(new ModelRemovalStepResult("STAGE_5_REMOVE_OWNED_ARTIFACTS", directArtifactPath, false, "Failed to delete owned directory artifact", null));
                    }
                } else {
                    FileDeletionUtil.DeletionStepResult fileRes = FileDeletionUtil.deleteFile(directArtifactPath);
                    if (fileRes.deleted()) {
                        result.addStepResult(new ModelRemovalStepResult("STAGE_5_REMOVE_OWNED_ARTIFACTS", directArtifactPath, true, "Successfully deleted owned file artifact", null));
                    } else {
                        artifactsRemoved = false;
                        result.addStepResult(new ModelRemovalStepResult("STAGE_5_REMOVE_OWNED_ARTIFACTS", directArtifactPath, false, fileRes.message(), fileRes.exception()));
                    }
                }
            } else {
                result.addStepResult(new ModelRemovalStepResult("STAGE_5_REMOVE_OWNED_ARTIFACTS", directArtifactPath, true, "Skipped deleting artifact path because ownership validation returned false (ambiguous/shared path)", null));
            }
        } else {
            result.addStepResult(new ModelRemovalStepResult("STAGE_5_REMOVE_OWNED_ARTIFACTS", null, true, "No explicitly owned physical file/directory artifact to delete", null));
        }

        // Stage 6: Update Configuration
        boolean configUpdated = false;
        if (orchestrator != null) {
            AIProvider realProvider = orchestrator.getAiProviders().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(modelName))
                    .findFirst().orElse(null);

            if (realProvider != null) {
                orchestrator.getAiProviders().remove(realProvider);
                configUpdated = true;
            }
            if (modelName.equalsIgnoreCase(orchestrator.getLocalModel())) {
                ProjectModelManager.getInstance().updateLocalModel(orchestrator, "");
                configUpdated = true;
            }
            if (modelName.equalsIgnoreCase(orchestrator.getRemoteModel())) {
                ProjectModelManager.getInstance().updateRemoteModel(orchestrator, "");
                configUpdated = true;
            }
            result.addStepResult(new ModelRemovalStepResult("STAGE_6_UPDATE_CONFIGURATION", null, true, "Updated orchestrator configuration references (Removed provider: " + (realProvider != null) + ")", null));
        }

        // Stage 7: Verify
        boolean runtimeVerifiedAbsent = true;
        if ("OLLAMA".equals(modelType)) {
            OllamaService ollamaService = OllamaManager.getInstance().getService(endpointUrl);
            if (ollamaService != null) {
                runtimeVerifiedAbsent = !isModelPresentInOllama(ollamaService, modelName);
            }
        }

        boolean artifactVerifiedAbsent = true;
        if (directArtifactPath != null && isValidOwnedArtifactPath(directArtifactPath, provider)) {
            artifactVerifiedAbsent = !Files.exists(directArtifactPath);
        }

        boolean verificationSuccess = runtimeVerifiedAbsent && artifactVerifiedAbsent;
        result.addStepResult(new ModelRemovalStepResult("STAGE_7_VERIFY", directArtifactPath, verificationSuccess,
                "Verification result - Runtime absent: " + runtimeVerifiedAbsent + ", Artifact absent: " + artifactVerifiedAbsent, null));

        // Stage 8: Refresh Caches
        if ("OLLAMA".equals(modelType)) {
            OllamaService ollamaService = OllamaManager.getInstance().getService(endpointUrl);
            if (ollamaService != null) {
                ollamaService.clearCache();
                ollamaService.refreshModels();
            }
        }
        result.addStepResult(new ModelRemovalStepResult("STAGE_8_REFRESH", null, true, "Refreshed model caches", null));

        // Stage 9: Report & Set Overall Status
        if ("REMOTE".equals(modelType)) {
            result.setStatus(configUpdated ? ModelRemovalStatus.SUCCESS : ModelRemovalStatus.UNSUPPORTED);
        } else if (runtimeRegistrationRemoved && artifactsRemoved && verificationSuccess) {
            result.setStatus(ModelRemovalStatus.SUCCESS);
        } else if (runtimeRegistrationRemoved || artifactsRemoved || configUpdated) {
            result.setStatus(ModelRemovalStatus.PARTIAL_SUCCESS);
        } else {
            result.setStatus(ModelRemovalStatus.FAILED);
        }

        result.log("Removal lifecycle completed with overall status: " + result.getStatus());
        return result;
    }

    private String determineModelType(AIProvider provider) {
        if (provider == null) return "UNKNOWN";
        if (!provider.isLocal()) return "REMOTE";
        String name = provider.getName() != null ? provider.getName().toLowerCase() : "";
        if (name.startsWith("demo/")) return "DEMO";
        if (name.endsWith(".evo") || "evo_native".equalsIgnoreCase(provider.getFormat())) return "EVO_NATIVE";
        if (name.endsWith(".gguf")) return "STANDALONE_GGUF";
        return "OLLAMA";
    }

    private String resolveEndpointUrl(Orchestrator orchestrator, AIProvider provider) {
        if (provider != null && provider.getUrl() != null && provider.getUrl().startsWith("http")) {
            return provider.getUrl();
        }
        if (orchestrator != null && orchestrator.getOllama() != null && orchestrator.getOllama().getUrl() != null) {
            return orchestrator.getOllama().getUrl();
        }
        return "http://localhost:11434";
    }

    private Path resolveDirectArtifactPath(AIProvider provider) {
        if (provider == null || provider.getUrl() == null || provider.getUrl().isBlank()) {
            return null;
        }
        if (provider.getUrl().startsWith("http://") || provider.getUrl().startsWith("https://")) {
            return null;
        }
        try {
            File f = new File(provider.getUrl());
            if (f.exists()) {
                return f.toPath().toAbsolutePath().normalize();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isValidOwnedArtifactPath(Path artifactPath, AIProvider provider) {
        if (artifactPath == null) return false;
        String pathStr = artifactPath.toString().toLowerCase();

        // Protect shared folders and root directories from accidental deletion
        if (pathStr.endsWith(".ollama") || pathStr.endsWith(".ollama/models") || pathStr.endsWith(".ollama\\models") ||
            pathStr.endsWith("source/models") || pathStr.endsWith("source\\models") ||
            pathStr.endsWith("lib/models") || pathStr.endsWith("lib\\models") ||
            pathStr.endsWith("forge-lab/forge-model/src/main/resources/model/demo")) {
            return false;
        }

        // Valid if it's a specific .evo file, .gguf file, or specific forge-output/dist directory
        if (pathStr.endsWith(".evo") || pathStr.endsWith(".gguf")) {
            return true;
        }

        if (Files.isDirectory(artifactPath) && (pathStr.contains("forge-output") || pathStr.contains("dist"))) {
            String dirName = artifactPath.getFileName().toString().toLowerCase();
            return dirName.startsWith("evo-") || dirName.startsWith("forging-") || (provider != null && dirName.equalsIgnoreCase(provider.getName()));
        }

        return false;
    }

    private boolean isModelPresentInOllama(OllamaService service, String modelName) {
        if (service == null || modelName == null) return false;
        List<OllamaModel> models = service.refreshModels();
        if (models == null) return false;
        for (OllamaModel m : models) {
            if (m.getName().equalsIgnoreCase(modelName)) {
                return true;
            }
        }
        return false;
    }
}
