package eu.kalafatic.evolution.controller.manager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Metadata editing and safe renaming for models.
 *
 * <p>Display metadata is persisted in {@link ModelStateStore} and never changes
 * the canonical/runtime identity. Physical artifact renaming is only performed
 * for EVO-owned EVO Native/GGUF artifacts and updates every reference that
 * belongs to the renamed artifact atomically before reporting success.</p>
 */
public class ModelEditingService {

    private static final ModelEditingService INSTANCE = new ModelEditingService();

    private ModelEditingService() {
    }

    public static ModelEditingService getInstance() {
        return INSTANCE;
    }

    public static final class MetadataUpdate {
        private String displayName = "";
        private String description = "";
        private List<String> tags = new ArrayList<>();
        private List<String> aliases = new ArrayList<>();

        public MetadataUpdate displayName(String value) {
            this.displayName = value != null ? value.trim() : "";
            return this;
        }

        public MetadataUpdate description(String value) {
            this.description = value != null ? value : "";
            return this;
        }

        public MetadataUpdate tags(List<String> value) {
            this.tags = value != null ? new ArrayList<>(value) : new ArrayList<>();
            return this;
        }

        public MetadataUpdate aliases(List<String> value) {
            this.aliases = value != null ? new ArrayList<>(value) : new ArrayList<>();
            return this;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getTags() {
            return tags;
        }

        public List<String> getAliases() {
            return aliases;
        }
    }

    public static final class RenameResult {
        private final boolean success;
        private final boolean artifactRenamed;
        private final String message;
        private final Path oldPath;
        private final Path newPath;

        public RenameResult(boolean success, boolean artifactRenamed, String message, Path oldPath, Path newPath) {
            this.success = success;
            this.artifactRenamed = artifactRenamed;
            this.message = message != null ? message : "";
            this.oldPath = oldPath;
            this.newPath = newPath;
        }

        public static RenameResult failure(String message) {
            return new RenameResult(false, false, message, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isArtifactRenamed() {
            return artifactRenamed;
        }

        public String getMessage() {
            return message;
        }

        public Path getOldPath() {
            return oldPath;
        }

        public Path getNewPath() {
            return newPath;
        }
    }

    /**
     * Saves user metadata. Display-name conflicts are rejected.
     */
    public RenameResult saveMetadata(Orchestrator orchestrator, AIProvider provider, MetadataUpdate update) {
        if (provider == null || update == null) {
            return RenameResult.failure("No model or metadata supplied.");
        }
        ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);
        String displayName = update.getDisplayName();
        if (displayName.isBlank()) {
            displayName = identity.getTechnicalId();
        }
        if (conflicts(orchestrator, identity, displayName)) {
            return RenameResult.failure("Another model already uses the name '" + displayName + "'.");
        }

        ModelStateStore store = ModelStateStore.getInstance();
        ModelStateStore.Metadata metadata = store.get(identity.getCanonicalId());
        if (metadata == null) {
            metadata = new ModelStateStore.Metadata();
        }
        metadata.setDisplayName(displayName);
        metadata.setDescription(update.getDescription());
        metadata.getTags().clear();
        metadata.getTags().addAll(clean(update.getTags()));
        metadata.getAliases().clear();
        metadata.getAliases().addAll(clean(update.getAliases()));
        store.put(identity.getCanonicalId(), metadata);

        ProjectModelManager.getInstance().notifyModelsChanged();
        return new RenameResult(true, false, "Metadata saved. Technical identity remains '"
                + identity.getTechnicalId() + "'.", null, null);
    }

    /**
     * Changes only the EVO display name. Never touches the runtime model.
     */
    public RenameResult renameDisplayName(Orchestrator orchestrator, AIProvider provider, String newDisplayName) {
        if (newDisplayName == null || newDisplayName.isBlank()) {
            return RenameResult.failure("Display name must not be empty.");
        }
        return saveMetadata(orchestrator, provider, new MetadataUpdate().displayName(newDisplayName));
    }

    /**
     * Renames the physical artifact. Only EVO-owned artifact-backed models are
     * supported; Ollama runtime renaming is not supported by the Ollama API.
     */
    public RenameResult renameArtifact(Orchestrator orchestrator, AIProvider provider, String newName) {
        if (provider == null) {
            return RenameResult.failure("No model selected.");
        }
        ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);
        if (identity.isOllama()) {
            return RenameResult.failure("Renaming an Ollama runtime model is not supported by the Ollama API. "
                    + "Only the EVO display name can be changed safely.");
        }
        if (!identity.isArtifactBacked()) {
            return RenameResult.failure("This model is not backed by a local artifact and cannot be renamed physically.");
        }
        Path artifact = identity.getArtifactPath();
        if (artifact == null) {
            return RenameResult.failure("The physical artifact could not be resolved.");
        }
        if (!ModelIdentityResolver.isOwnedArtifact(artifact)) {
            return RenameResult.failure("The artifact is not owned by EVO and must not be renamed: " + artifact);
        }
        String simpleName = sanitizeArtifactName(newName, artifact.getFileName().toString(),
                Files.isDirectory(artifact));
        if (simpleName == null) {
            return RenameResult.failure("Invalid artifact name. Use a simple file name without path separators.");
        }
        Path parent = artifact.getParent();
        if (parent == null) {
            return RenameResult.failure("Artifact has no parent directory.");
        }
        Path target = parent.resolve(simpleName);
        if (target.equals(artifact)) {
            return RenameResult.failure("The artifact already has that name.");
        }
        if (Files.exists(target)) {
            return RenameResult.failure("A file or directory named '" + simpleName + "' already exists in "
                    + parent + ".");
        }

        List<AIProvider> registrations = ProjectModelManager.getInstance()
                .findRegisteredProviders(orchestrator, identity);
        try {
            Files.move(artifact, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicFailure) {
            try {
                Files.move(artifact, target);
            } catch (Exception moveFailure) {
                return RenameResult.failure("Failed to rename artifact: " + moveFailure.getMessage());
            }
        }

        String oldFileName = artifact.getFileName().toString();
        String newFileName = target.getFileName().toString();
        String oldPathString = artifact.toAbsolutePath().normalize().toString();
        String newPathString = target.toAbsolutePath().normalize().toString();

        for (AIProvider registration : registrations) {
            String url = registration.getUrl();
            if (url != null && url.equals(oldPathString)) {
                registration.setUrl(newPathString);
            }
            if (registration.getName() != null
                    && (registration.getName().equalsIgnoreCase(oldFileName)
                            || registration.getName().equalsIgnoreCase(stripExtension(oldFileName)))) {
                registration.setName(newFileName);
            }
        }

        // Update default references that used the old artifact name.
        if (registrationReferences(orchestrator, identity)) {
            updateDefaultReference(orchestrator, identity, newFileName);
        }

        // Move metadata to the new canonical identity.
        String newCanonical = "artifact|" + newPathString;
        ModelStateStore.getInstance().move(identity.getCanonicalId(), newCanonical);

        ProjectModelManager.getInstance().refreshModelDiscovery(orchestrator);
        Log.log("[MODEL-RENAME] Renamed artifact '" + artifact + "' to '" + target + "' ("
                + registrations.size() + " registration reference(s) updated)");
        return new RenameResult(true, true, "Artifact renamed to '" + newFileName + "'. "
                + registrations.size() + " registration reference(s) updated.", artifact, target);
    }

    private boolean registrationReferences(Orchestrator orchestrator, ModelIdentity identity) {
        return ProjectModelManager.getInstance().referencesModel(identity, orchestrator.getLocalModel())
                || ProjectModelManager.getInstance().referencesModel(identity, orchestrator.getRemoteModel())
                || ProjectModelManager.getInstance().referencesModel(identity, orchestrator.getHybridModel());
    }

    private void updateDefaultReference(Orchestrator orchestrator, ModelIdentity identity, String newName) {
        ProjectModelManager manager = ProjectModelManager.getInstance();
        if (manager.referencesModel(identity, orchestrator.getLocalModel())) {
            manager.updateLocalModel(orchestrator, newName);
        }
        if (manager.referencesModel(identity, orchestrator.getRemoteModel())) {
            manager.updateRemoteModel(orchestrator, newName);
        }
        if (manager.referencesModel(identity, orchestrator.getHybridModel())) {
            manager.updateHybridModel(orchestrator, newName);
        }
    }

    /**
     * Rejects display/runtime name conflicts with other models.
     */
    public boolean conflicts(Orchestrator orchestrator, ModelIdentity identity, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        ModelStateStore store = ModelStateStore.getInstance();
        for (var entry : store.snapshot().entrySet()) {
            if (entry.getKey().equals(identity.getCanonicalId())) {
                continue;
            }
            String display = entry.getValue().getDisplayName();
            if (display != null && !display.isBlank()
                    && display.toLowerCase(Locale.ROOT).equals(normalized)) {
                return true;
            }
        }
        if (orchestrator != null) {
            for (AIProvider provider : orchestrator.getAiProviders()) {
                ModelIdentity other = ModelIdentityResolver.resolve(orchestrator, provider);
                if (other.getCanonicalId().equals(identity.getCanonicalId())) {
                    continue;
                }
                if (ModelIdentityResolver.matchesAlias(other, candidate)
                        || candidate.equalsIgnoreCase(other.getTechnicalId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String sanitizeArtifactName(String name, String originalFileName, boolean directory) {
        if (name == null) {
            return null;
        }
        String value = name.trim();
        if (value.isEmpty() || value.contains("/") || value.contains("\\")
                || value.contains("..") || value.indexOf('\0') >= 0) {
            return null;
        }
        if (!directory) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".evo") && !lower.endsWith(".gguf")) {
                String originalLower = originalFileName != null
                        ? originalFileName.toLowerCase(Locale.ROOT) : "";
                value = value + (originalLower.endsWith(".gguf") ? ".gguf" : ".evo");
            }
        }
        return value;
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static List<String> clean(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }
}
