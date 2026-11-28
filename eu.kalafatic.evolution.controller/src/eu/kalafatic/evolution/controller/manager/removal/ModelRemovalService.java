package eu.kalafatic.evolution.controller.manager.removal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.manager.ModelArtifactResolver;
import eu.kalafatic.evolution.controller.manager.ModelIdentity;
import eu.kalafatic.evolution.controller.manager.ModelIdentityResolver;
import eu.kalafatic.evolution.controller.manager.ModelStateStore;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Enforces the model removal lifecycle.
 *
 * <p>Removal resolves the canonical model identity first and then cleans every
 * representation it owns: Ollama runtime registration (when the runtime is
 * reachable), the EVO EMF provider registration, default model references,
 * owned artifacts (only when explicitly requested) and discovery caches.
 * Verification is based on the authoritative sources, not on the fact that a
 * local cache was refreshed.</p>
 */
public class ModelRemovalService {

    private static final ModelRemovalService INSTANCE = new ModelRemovalService();

    private ModelRemovalService() {
    }

    public static ModelRemovalService getInstance() {
        return INSTANCE;
    }

    /**
     * Removal options. Artifact deletion is opt-in and never deletes shared or
     * externally owned files.
     */
    public static final class RemovalOptions {
        private boolean deleteOwnedArtifacts;
        private boolean deleteSharedArtifacts;
        private boolean unloadRuntime = true;
        private boolean deregisterRuntime = true;
        private boolean suppressAfterRemoval = true;

        public static RemovalOptions defaults() {
            return new RemovalOptions();
        }

        public RemovalOptions deleteOwnedArtifacts(boolean value) {
            this.deleteOwnedArtifacts = value;
            return this;
        }

        public RemovalOptions deleteSharedArtifacts(boolean value) {
            this.deleteSharedArtifacts = value;
            return this;
        }

        public RemovalOptions unloadRuntime(boolean value) {
            this.unloadRuntime = value;
            return this;
        }

        public RemovalOptions deregisterRuntime(boolean value) {
            this.deregisterRuntime = value;
            return this;
        }

        public RemovalOptions suppressAfterRemoval(boolean value) {
            this.suppressAfterRemoval = value;
            return this;
        }

        public boolean isDeleteOwnedArtifacts() {
            return deleteOwnedArtifacts;
        }

        public boolean isDeleteSharedArtifacts() {
            return deleteSharedArtifacts;
        }

        public boolean isUnloadRuntime() {
            return unloadRuntime;
        }

        public boolean isDeregisterRuntime() {
            return deregisterRuntime;
        }

        public boolean isSuppressAfterRemoval() {
            return suppressAfterRemoval;
        }
    }

    public List<ModelRemovalResult> removeModels(Orchestrator orchestrator, List<AIProvider> selectedModels) {
        return removeModels(orchestrator, selectedModels, RemovalOptions.defaults());
    }

    /**
     * Executes batch removal. Duplicate UI rows referring to the same canonical
     * model are collapsed so the runtime is not deregistered twice.
     */
    public List<ModelRemovalResult> removeModels(Orchestrator orchestrator, List<AIProvider> selectedModels,
                                                 RemovalOptions options) {
        List<ModelRemovalResult> results = new ArrayList<>();
        if (selectedModels == null || selectedModels.isEmpty()) {
            return results;
        }
        RemovalOptions effectiveOptions = options != null ? options : RemovalOptions.defaults();
        Map<String, AIProvider> unique = new LinkedHashMap<>();
        for (AIProvider provider : selectedModels) {
            if (provider == null) {
                continue;
            }
            ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);
            unique.putIfAbsent(identity.getCanonicalId(), provider);
        }
        for (AIProvider provider : unique.values()) {
            String removalId = UUID.randomUUID().toString().substring(0, 8);
            results.add(removeSingleModel(removalId, orchestrator, provider, effectiveOptions));
        }
        return results;
    }

    public ModelRemovalResult removeSingleModel(String removalId, Orchestrator orchestrator, AIProvider provider) {
        return removeSingleModel(removalId, orchestrator, provider, RemovalOptions.defaults());
    }

    /**
     * Executes the removal lifecycle for a single model.
     */
    public ModelRemovalResult removeSingleModel(String removalId, Orchestrator orchestrator, AIProvider provider,
                                                RemovalOptions options) {
        RemovalOptions effectiveOptions = options != null ? options : RemovalOptions.defaults();
        String rawName = provider != null && provider.getName() != null ? provider.getName() : "unknown";
        ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);
        String modelType = identity.getKind().name();
        String endpointUrl = identity.getEndpoint();

        ModelRemovalResult result = new ModelRemovalResult(removalId, rawName, modelType, endpointUrl);
        result.setRequestedIdentity(rawName);
        result.setCanonicalIdentity(identity.getCanonicalId());
        result.setProvider(identity.getProviderId());
        result.setResolvedAliases(identity.getAliases());
        result.setArtifactLocation(identity.getArtifactPath() != null
                ? identity.getArtifactPath().toString() : "");
        result.log("Starting removal lifecycle for model '" + rawName + "' (Type: " + modelType
                + ", Endpoint: " + endpointUrl + ", Canonical: " + identity.getCanonicalId() + ")");

        // Stage 1: Resolve identity and ownership
        logStageStart(result, "STAGE_1_RESOLVE_IDENTITY");
        if (rawName.isBlank() || identity.getCanonicalId().isBlank()) {
            result.addStepResult(ModelRemovalStepResult.failed("STAGE_1_RESOLVE_IDENTITY",
                    "Model name/canonical identity is empty", null));
            result.setStatus(ModelRemovalStatus.FAILED);
            result.log(result.getFinalSummaryLine());
            return result;
        }

        Path artifactPath = identity.getArtifactPath();
        boolean artifactOwned = artifactPath != null && ModelIdentityResolver.isOwnedArtifact(artifactPath);
        List<Path> ownedArtifacts = eu.kalafatic.evolution.controller.manager.ModelArtifactResolver
                .resolveOwnedArtifacts(identity);
        if (artifactPath == null && !ownedArtifacts.isEmpty()) {
            artifactPath = ownedArtifacts.get(0);
        }
        if (!ownedArtifacts.isEmpty()) {
            artifactOwned = true;
        }
        result.setOwnership(artifactPath == null ? "No physical artifact"
                : artifactOwned ? "EVO-owned artifact(s): " + ownedArtifacts.size()
                : "External/shared artifact (retained)");
        result.addStepResult(ModelRemovalStepResult.success("STAGE_1_RESOLVE_IDENTITY",
                "Requested '" + rawName + "' resolved to canonical '" + identity.getCanonicalId()
                        + "'; aliases=" + identity.getAliases()
                        + "; artifact=" + (artifactPath != null ? artifactPath : "none")
                        + "; ownership=" + result.getOwnership()));

        ProjectModelManager modelManager = ProjectModelManager.getInstance();

        // Stage 2: Resolve all registrations
        logStageStart(result, "STAGE_2_RESOLVE_REGISTRATIONS");
        List<AIProvider> registeredProviders = modelManager.findRegisteredProviders(orchestrator, identity);
        result.addStepResult(ModelRemovalStepResult.success("STAGE_2_RESOLVE_REGISTRATIONS",
                "EVO EMF registrations=" + registeredProviders.size()
                        + "; runtime identity=" + identity.getRuntimeName()
                        + "; provider=" + identity.getProviderId()));

        // Stage 3: Unload from runtime (when supported)
        logStageStart(result, "STAGE_3_UNLOAD");
        boolean runtimeReachable = true;
        if (!identity.isOllama()) {
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_3_UNLOAD",
                    "Unload is not applicable for " + modelType));
        } else if (!effectiveOptions.isUnloadRuntime()) {
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_3_UNLOAD",
                    "Unload skipped by request"));
        } else {
            OllamaService service = OllamaManager.getInstance().getService(endpointUrl);
            OllamaService.OllamaOperationResult unload = service.unloadModelEx(identity.getRuntimeName());
            if (unload.success()) {
                result.addStepResult(ModelRemovalStepResult.success("STAGE_3_UNLOAD",
                        "Unloaded '" + identity.getRuntimeName() + "' from Ollama memory/VRAM"));
            } else if (unload.statusCode() == 404) {
                result.addStepResult(ModelRemovalStepResult.skipped("STAGE_3_UNLOAD",
                        "Model was already absent from the runtime (HTTP 404)"));
            } else if (isRuntimeUnavailable(unload)) {
                runtimeReachable = false;
                result.addStepResult(ModelRemovalStepResult.skipped("STAGE_3_UNLOAD",
                        "Runtime unavailable: " + unload.message()));
            } else {
                result.addStepResult(ModelRemovalStepResult.failed("STAGE_3_UNLOAD",
                        "Failed to unload model: " + unload.message(), unload.exception()));
            }
        }

        // Stage 4: Remove runtime registration
        logStageStart(result, "STAGE_4_REMOVE_RUNTIME_REGISTRATION");
        boolean runtimeRegistrationRemoved = !identity.isOllama();
        if (!identity.isOllama()) {
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_4_REMOVE_RUNTIME_REGISTRATION",
                    "Runtime registration removal is not applicable for " + modelType));
            result.setRuntimeOutcome("Not applicable");
        } else if (!effectiveOptions.isDeregisterRuntime()) {
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_4_REMOVE_RUNTIME_REGISTRATION",
                    "Runtime deregistration skipped by request"));
            result.setRuntimeOutcome("Retained (by request)");
        } else if (!runtimeReachable) {
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_4_REMOVE_RUNTIME_REGISTRATION",
                    "Runtime is unreachable; runtime registration could not be removed or verified"));
            result.setRuntimeOutcome("Unverified - runtime unavailable");
        } else {
            OllamaService service = OllamaManager.getInstance().getService(endpointUrl);
            OllamaService.OllamaOperationResult delete = service.deleteModelEx(identity.getRuntimeName());
            if (delete.success()) {
                runtimeRegistrationRemoved = true;
                result.addStepResult(ModelRemovalStepResult.success("STAGE_4_REMOVE_RUNTIME_REGISTRATION",
                        "Removed '" + identity.getRuntimeName() + "' from the Ollama registry"));
                result.setRuntimeOutcome("Removed");
            } else if (delete.statusCode() == 404) {
                runtimeRegistrationRemoved = true;
                result.addStepResult(ModelRemovalStepResult.skipped("STAGE_4_REMOVE_RUNTIME_REGISTRATION",
                        "Model was already absent from the Ollama registry (HTTP 404 treated as idempotent success)"));
                result.setRuntimeOutcome("Already absent");
            } else {
                OllamaService.OllamaModelListResult listing = service.listModelsEx();
                if (!listing.success()) {
                    runtimeReachable = false;
                    result.addStepResult(ModelRemovalStepResult.failed("STAGE_4_REMOVE_RUNTIME_REGISTRATION",
                            "Could not verify runtime removal because the runtime is unavailable: " + listing.message(),
                            listing.exception()));
                    result.setRuntimeOutcome("Unverified - runtime unavailable");
                } else if (!containsModel(listing.models(), identity.getRuntimeName())) {
                    runtimeRegistrationRemoved = true;
                    result.addStepResult(ModelRemovalStepResult.success("STAGE_4_REMOVE_RUNTIME_REGISTRATION",
                            "Delete reported an error but the model is absent from the runtime (idempotent success)"));
                    result.setRuntimeOutcome("Removed");
                } else {
                    result.addStepResult(ModelRemovalStepResult.failed("STAGE_4_REMOVE_RUNTIME_REGISTRATION",
                            "Model is still registered in the runtime: " + delete.message(), delete.exception()));
                    result.setRuntimeOutcome("Failed");
                }
            }
        }

        // Stage 5: Remove the complete owned artifact set (opt-in)
        logStageStart(result, "STAGE_5_REMOVE_OWNED_ARTIFACTS");
        List<Path> deletionAttempts = new ArrayList<>();
        String artifactOutcome;
        List<Path> artifactSet = new ArrayList<>(ownedArtifacts);
        Path directArtifact = artifactPath;
        if (directArtifact != null && ModelIdentityResolver.isOwnedArtifact(directArtifact)
                && artifactSet.stream().noneMatch(p -> p.equals(directArtifact.toAbsolutePath().normalize()))) {
            artifactSet.add(directArtifact.toAbsolutePath().normalize());
        }

        if (artifactSet.isEmpty()) {
            artifactOutcome = "No artifact";
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_5_REMOVE_OWNED_ARTIFACTS",
                    "No physical artifact was resolved for this model"));
        } else if (!artifactOwned) {
            artifactOutcome = "Retained (not owned by EVO)";
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_5_REMOVE_OWNED_ARTIFACTS", artifactPath,
                    "Artifacts are external/shared and are never deleted"));
        } else if (!effectiveOptions.isDeleteOwnedArtifacts()) {
            artifactOutcome = "Retained (deletion not requested)";
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_5_REMOVE_OWNED_ARTIFACTS", artifactPath,
                    artifactSet.size() + " owned artifact(s) retained because deletion was not requested"));
        } else {
            List<Path> protectedShared = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            int alreadyAbsent = 0;
            for (Path candidate : artifactSet) {
                if (ModelArtifactResolver.isSharedGenericArtifact(candidate)
                        && !effectiveOptions.isDeleteSharedArtifacts()) {
                    protectedShared.add(candidate);
                    continue;
                }
                if (!Files.exists(candidate)) {
                    alreadyAbsent++;
                    continue;
                }
                deletionAttempts.add(candidate);
                boolean deleted;
                if (Files.isDirectory(candidate)) {
                    FileDeletionUtil.DirectoryDeletionResult dirResult =
                            FileDeletionUtil.deleteDirectoryRecursive(candidate);
                    deleted = dirResult.success() && !Files.exists(candidate);
                } else {
                    FileDeletionUtil.DeletionStepResult fileResult = FileDeletionUtil.deleteFile(candidate);
                    deleted = fileResult.deleted() && !Files.exists(candidate);
                }
                if (!deleted) {
                    failures.add(candidate.toString());
                }
            }

            StringBuilder summary = new StringBuilder();
            summary.append("Deleted ").append(deletionAttempts.size() - failures.size())
                    .append(" of ").append(deletionAttempts.size()).append(" owned artifact(s)");
            if (alreadyAbsent > 0) {
                summary.append("; ").append(alreadyAbsent).append(" already absent");
            }
            if (!protectedShared.isEmpty()) {
                summary.append("; retained shared generic artifact(s): ").append(protectedShared);
            }
            if (failures.isEmpty()) {
                artifactOutcome = protectedShared.isEmpty()
                        ? "Deleted (" + (deletionAttempts.size() - failures.size()) + ")"
                        : "Partially retained (shared): " + protectedShared.size();
                result.addStepResult(ModelRemovalStepResult.success("STAGE_5_REMOVE_OWNED_ARTIFACTS",
                        artifactPath, summary.toString()));
            } else {
                artifactOutcome = "Delete failed (" + failures.size() + ")";
                result.addStepResult(ModelRemovalStepResult.failed("STAGE_5_REMOVE_OWNED_ARTIFACTS",
                        artifactPath, summary + "; failed: " + failures, null));
            }
        }
        result.setArtifactOutcome(artifactOutcome);

        // Stage 6: Remove EVO registry (EMF provider) entries
        logStageStart(result, "STAGE_6_REMOVE_EVO_REGISTRATION");
        List<String> removedProviderNames = modelManager.removeRegisteredProviders(orchestrator, identity);
        if (removedProviderNames.isEmpty()) {
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_6_REMOVE_EVO_REGISTRATION",
                    registeredProviders.isEmpty()
                            ? "No matching EVO registration existed (already absent)"
                            : "Registration entries disappeared during removal"));
            result.setRegistryOutcome(registeredProviders.isEmpty()
                    ? "Already absent" : "Removed");
        } else {
            result.addStepResult(ModelRemovalStepResult.success("STAGE_6_REMOVE_EVO_REGISTRATION",
                    "Removed EVO provider registration(s): " + removedProviderNames));
            result.setRegistryOutcome("Removed (" + removedProviderNames.size() + ")");
        }

        // Stage 7: Clear default model references
        logStageStart(result, "STAGE_7_CLEAR_CONFIGURATION");
        boolean referencesCleared = modelManager.clearModelDefaultReferences(orchestrator, identity);
        if (referencesCleared) {
            result.addStepResult(ModelRemovalStepResult.success("STAGE_7_CLEAR_CONFIGURATION",
                    "Cleared default model references pointing at the removed model"));
            result.setConfigurationOutcome("Cleared");
        } else {
            result.addStepResult(ModelRemovalStepResult.skipped("STAGE_7_CLEAR_CONFIGURATION",
                    "No default model reference pointed at the removed model"));
            result.setConfigurationOutcome("No references");
        }

        // Stage 8: Invalidate caches and refresh discovery
        logStageStart(result, "STAGE_8_REFRESH");
        if (identity.isOllama()) {
            OllamaService service = OllamaManager.getInstance().getService(endpointUrl);
            service.clearCache();
            if (runtimeReachable) {
                service.refreshModels();
            }
        }
        modelManager.refreshModelDiscovery(orchestrator);
        result.addStepResult(ModelRemovalStepResult.success("STAGE_8_REFRESH",
                "Invalidated model discovery caches and notified consumers"));

        if (effectiveOptions.isSuppressAfterRemoval()) {
            ModelStateStore store = modelManager.getModelStateStore();
            String reason = "Removed via Properties > Models on " + java.time.Instant.now()
                    + " (requested='" + rawName + "')";
            try {
                boolean persisted = store.suppress(identity.getCanonicalId(), reason);
                if (persisted) {
                    result.addStepResult(ModelRemovalStepResult.success("STAGE_8B_SUPPRESS",
                            "Marked canonical identity as explicitly unregistered: "
                                    + identity.getCanonicalId()));
                } else {
                    result.addStepResult(ModelRemovalStepResult.failed("STAGE_8B_SUPPRESS",
                            "Could not persist the unregistration marker at "
                                    + store.getStateFile() + "; a later discovery may re-list the model.",
                            null));
                }
            } catch (Throwable t) {
                // The marker is auxiliary; it must never abort registry/runtime cleanup.
                result.addStepResult(ModelRemovalStepResult.failed("STAGE_8B_SUPPRESS",
                        "Could not persist the unregistration marker: " + t.getMessage(), null));
            }
        }

        // Stage 9: Verify absence from authoritative sources
        logStageStart(result, "STAGE_9_VERIFY");
        List<AIProvider> remainingRegistrations = modelManager.findRegisteredProviders(orchestrator, identity);
        boolean registryAbsent = remainingRegistrations.isEmpty();
        boolean defaultsCleared = !modelManager.referencesModel(identity, orchestrator.getLocalModel())
                && !modelManager.referencesModel(identity, orchestrator.getRemoteModel())
                && !modelManager.referencesModel(identity, orchestrator.getHybridModel())
                && !modelManager.referencesModel(identity, orchestrator.getOpenAiModel())
                && (orchestrator.getOllama() == null
                        || !modelManager.referencesModel(identity, orchestrator.getOllama().getModel()))
                && (orchestrator.getLlm() == null
                        || !modelManager.referencesModel(identity, orchestrator.getLlm().getModel()))
                && (orchestrator.getNeuronAI() == null
                        || !modelManager.referencesModel(identity, orchestrator.getNeuronAI().getModel()));

        boolean deletionAttempted = !deletionAttempts.isEmpty();
        boolean artifactVerified = true;
        if (deletionAttempted) {
            List<Path> remainingArtifacts = new ArrayList<>();
            for (Path attempted : deletionAttempts) {
                if (Files.exists(attempted)) {
                    remainingArtifacts.add(attempted);
                }
            }
            artifactVerified = remainingArtifacts.isEmpty();
            if (!artifactVerified) {
                result.setArtifactOutcome("Still present after deletion: " + remainingArtifacts);
            }
        }

        boolean runtimeVerified = true;
        if (identity.isOllama() && effectiveOptions.isDeregisterRuntime()) {
            if (!runtimeReachable) {
                runtimeVerified = false;
            } else {
                OllamaService service = OllamaManager.getInstance().getService(endpointUrl);
                OllamaService.OllamaModelListResult listing = service.listModelsEx();
                if (!listing.success()) {
                    runtimeVerified = false;
                } else {
                    runtimeVerified = !containsModel(listing.models(), identity.getRuntimeName());
                }
            }
        }

        boolean verified = registryAbsent && defaultsCleared && artifactVerified && runtimeVerified;
        result.addStepResult(verified
                ? ModelRemovalStepResult.success("STAGE_9_VERIFY",
                        "Verified absence - registry: absent, defaults: clear, artifact: "
                                + (artifactVerified ? "absent/retained" : "present")
                                + ", runtime: "
                                + (runtimeVerified ? "absent/not applicable" : "unverified"))
                : ModelRemovalStepResult.failed("STAGE_9_VERIFY",
                        "Verification incomplete - registryAbsent=" + registryAbsent
                                + ", defaultsCleared=" + defaultsCleared
                                + ", artifactVerified=" + artifactVerified
                                + ", runtimeVerified=" + runtimeVerified, null));
        result.setVerificationOutcome(verified ? "Verified absent" : "Incomplete");

        // Stage 10: Truthful final classification
        logStageStart(result, "STAGE_10_REPORT");
        boolean runtimeRetainedByRequest = identity.isOllama()
                && !effectiveOptions.isDeregisterRuntime()
                && !runtimeRegistrationRemoved;
        if (!registryAbsent) {
            result.setStatus(ModelRemovalStatus.FAILED);
            result.setConfigurationOutcome("Registration still present - removal failed");
        } else if (verified && !result.hasFailedStep() && !runtimeRetainedByRequest) {
            result.setStatus(ModelRemovalStatus.SUCCESS);
        } else if (registryAbsent) {
            result.setStatus(ModelRemovalStatus.PARTIAL_SUCCESS);
        } else {
            result.setStatus(ModelRemovalStatus.FAILED);
        }

        // Surface retained runtime registration explicitly
        if (identity.isOllama() && !runtimeRegistrationRemoved) {
            result.setRuntimeOutcome("Retained / unverified");
        }

        result.log("Removal lifecycle completed: " + result.getSummaryTableRow());
        result.log(result.getFinalSummaryLine());
        Log.log("[REMOVAL_LOG]" + result.getFinalSummaryLine());
        return result;
    }

    private void logStageStart(ModelRemovalResult result, String stage) {
        result.log("[" + result.getRemovalId() + "][" + stage + "] STARTED");
    }

    private boolean containsModel(List<OllamaModel> models, String runtimeName) {
        if (models == null) {
            return false;
        }
        for (OllamaModel model : models) {
            if (model != null && ModelIdentityResolver.ollamaNamesEquivalent(model.getName(), runtimeName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRuntimeUnavailable(OllamaService.OllamaOperationResult result) {
        return result != null && result.statusCode() <= 0 && result.exception() != null;
    }

    /**
     * Exposed for diagnostics/tests: resolves the artifact that would be
     * considered for deletion.
     */
    public Path resolveArtifactPath(Orchestrator orchestrator, AIProvider provider) {
        return ModelIdentityResolver.resolve(orchestrator, provider).getArtifactPath();
    }

    /**
     * Exposed for diagnostics/tests: canonical identity of a provider entry.
     */
    public String resolveCanonicalId(Orchestrator orchestrator, AIProvider provider) {
        return ModelIdentityResolver.resolve(orchestrator, provider).getCanonicalId();
    }
}
