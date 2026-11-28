package eu.kalafatic.evolution.view.editors.pages.properties;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
 * Immutable UI row representing one logical model. Multiple discovery or
 * registration representations of the same canonical model are collapsed into
 * a single row by {@link ProjectModelManager#getAllModels(Orchestrator)}.
 */
public final class ModelRow {

    private final AIProvider provider;
    private final ModelIdentity identity;
    private final String displayName;
    private final String description;
    private final List<String> tags;
    private final String registrationState;
    private final String status;
    private final String statusDescription;
    private final Path artifactPath;
    private final boolean artifactOwned;
    private final int ownedArtifactCount;
    private final long size;
    private final long modified;
    private final String providerLabel;
    private final String typeLabel;
    private final String endpoint;
    private final String location;

    private ModelRow(AIProvider provider, ModelIdentity identity, ModelStateStore.Metadata metadata,
                     String registrationState, Path artifactPath, boolean artifactOwned,
                     int ownedArtifactCount, long size, long modified) {
        this.provider = provider;
        this.identity = identity;
        this.displayName = metadata != null && metadata.getDisplayName() != null
                && !metadata.getDisplayName().isBlank()
                ? metadata.getDisplayName() : identity.getTechnicalId();
        this.description = metadata != null ? metadata.getDescription() : "";
        this.tags = metadata != null ? List.copyOf(metadata.getTags()) : List.of();
        this.registrationState = registrationState;
        this.status = resolveStatus(provider, registrationState);
        this.statusDescription = provider.getStateDescription() != null ? provider.getStateDescription() : "";
        this.artifactPath = artifactPath;
        this.artifactOwned = artifactOwned;
        this.ownedArtifactCount = ownedArtifactCount;
        this.size = size;
        this.modified = modified;
        this.providerLabel = resolveProviderLabel(identity, provider);
        this.typeLabel = resolveTypeLabel(identity, provider);
        this.endpoint = identity.getEndpoint();
        this.location = artifactPath != null ? artifactPath.toString()
                : (provider.getUrl() != null && !provider.getUrl().startsWith("http")
                        ? provider.getUrl() : "");
    }

    public static ModelRow from(Orchestrator orchestrator, AIProvider provider) {
        return from(orchestrator, provider, null, null);
    }

    public static ModelRow from(Orchestrator orchestrator, AIProvider provider,
                                java.util.Set<String> registeredCanonicalIds) {
        return from(orchestrator, provider, registeredCanonicalIds, null);
    }

    /**
     * @param registeredCanonicalIds pre-computed canonical ids of every EMF
     *                               registration, or {@code null} to resolve
     *                               them per row.
     * @param ollamaModels           pre-fetched Ollama model list for the
     *                               endpoint, or {@code null} to fetch lazily.
     */
    public static ModelRow from(Orchestrator orchestrator, AIProvider provider,
                                java.util.Set<String> registeredCanonicalIds,
                                List<OllamaModel> ollamaModels) {
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
        ModelIdentity identity = ModelIdentityResolver.resolve(orchestrator, provider);
        ModelStateStore store = ModelStateStore.getInstance();
        ModelStateStore.Metadata metadata = store.get(identity.getCanonicalId());
        String registrationState;
        if (registeredCanonicalIds != null) {
            if (registeredCanonicalIds.contains(identity.getCanonicalId())) {
                registrationState = "Registered";
            } else if (store.isSuppressed(identity.getCanonicalId())) {
                registrationState = "Unregistered";
            } else {
                registrationState = "Discovered";
            }
        } else {
            registrationState = ProjectModelManager.getInstance()
                    .getRegistrationState(orchestrator, provider);
        }

        java.util.List<Path> ownedArtifacts = (identity.isArtifactBacked() || identity.isOllama())
                ? eu.kalafatic.evolution.controller.manager.ModelArtifactResolver
                        .resolveOwnedArtifacts(identity)
                : java.util.List.of();
        Path artifactPath = identity.getArtifactPath();
        if (artifactPath == null && !ownedArtifacts.isEmpty()) {
            artifactPath = ownedArtifacts.get(0);
        }
        boolean artifactOwned = artifactPath != null && ModelIdentityResolver.isOwnedArtifact(artifactPath);
        int artifactCount = artifactOwned ? Math.max(1, ownedArtifacts.size()) : 0;
        long size = 0L;
        long modified = 0L;
        if (artifactPath != null && Files.exists(artifactPath)) {
            size = computeSize(artifactPath);
            try {
                modified = Files.getLastModifiedTime(artifactPath).toMillis();
            } catch (Exception ignored) {
                // Leave unknown
            }
        }
        if (identity.isOllama()) {
            List<OllamaModel> models = ollamaModels;
            if (models == null) {
                OllamaService service = OllamaManager.getInstance().getService(identity.getEndpoint());
                models = service.loadModels();
            }
            if (models != null) {
                for (OllamaModel model : models) {
                    if (ModelIdentityResolver.ollamaNamesEquivalent(model.getName(), identity.getRuntimeName())) {
                        if (size == 0L) {
                            size = model.getSize();
                        }
                        if (modified == 0L) {
                            modified = parseOllamaModified(model.getModifiedAt());
                        }
                        break;
                    }
                }
            }
        }
        return new ModelRow(provider, identity, metadata, registrationState,
                artifactPath, artifactOwned, artifactCount, size, modified);
    }

    public AIProvider getProvider() {
        return provider;
    }

    public ModelIdentity getIdentity() {
        return identity;
    }

    public String getKey() {
        return identity.getCanonicalId();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTechnicalId() {
        return identity.getTechnicalId();
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getRegistrationState() {
        return registrationState;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public Path getArtifactPath() {
        return artifactPath;
    }

    public boolean isArtifactOwned() {
        return artifactOwned;
    }

    public int getOwnedArtifactCount() {
        return ownedArtifactCount;
    }

    public boolean hasOwnedArtifacts() {
        return ownedArtifactCount > 0;
    }

    public boolean hasArtifact() {
        return artifactPath != null;
    }

    public long getSize() {
        return size;
    }

    public long getModified() {
        return modified;
    }

    public String getProviderLabel() {
        return providerLabel;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getLocation() {
        return location;
    }

    public String getFormattedSize() {
        return formatSize(size);
    }

    public String getFormattedModified() {
        if (modified <= 0L) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(modified));
    }

    public String getActionsLabel() {
        return "Edit · Rename · Delete";
    }

    /**
     * True when the row matches any of the free-text search terms across the
     * complete model dataset (name, technical id, provider, type, endpoint,
     * location, tags and description).
     */
    public boolean matchesSearch(String query) {
        return eu.kalafatic.evolution.controller.manager.ModelSearch.matchesAllTerms(query,
                displayName, identity.getTechnicalId(), identity.getCanonicalId(),
                providerLabel, typeLabel, status, registrationState, endpoint, location,
                description, String.join(" ", tags));
    }

    private static String resolveStatus(AIProvider provider, String registrationState) {
        String state = provider.getState();
        String status;
        if ("OK".equals(state)) {
            status = "Available";
        } else if ("NA".equals(state)) {
            status = "Missing / not registered";
        } else if ("ERR".equals(state)) {
            status = "Offline / error";
        } else if (state == null || state.isBlank()) {
            status = "Unknown";
        } else {
            status = state;
        }
        if ("Unregistered".equals(registrationState) && "Available".equals(status)) {
            return "Available (unregistered)";
        }
        return status;
    }

    private static String resolveProviderLabel(ModelIdentity identity, AIProvider provider) {
        switch (identity.getKind()) {
            case OLLAMA:
                return "Ollama";
            case EVO_NATIVE:
                return "EVO Native";
            case GGUF:
                return "GGUF";
            case DEMO:
                return "Demo";
            case REMOTE:
                String format = provider.getFormat();
                return format != null && !format.isBlank() ? format : "Remote";
            default:
                return "Unknown";
        }
    }

    private static String resolveTypeLabel(ModelIdentity identity, AIProvider provider) {
        String format = provider.getFormat();
        if (identity.getKind() == ModelIdentity.Kind.OLLAMA) {
            return "Local";
        }
        if (identity.getKind() == ModelIdentity.Kind.REMOTE) {
            return "Remote";
        }
        if (format != null && !format.isBlank() && !"ollama".equalsIgnoreCase(format)) {
            return identity.getKind().getLabel() + " (" + format + ")";
        }
        return identity.getKind().getLabel();
    }

    private static long computeSize(Path path) {
        if (path == null) {
            return 0L;
        }
        try {
            if (Files.isRegularFile(path)) {
                return Files.size(path);
            }
            if (Files.isDirectory(path)) {
                long total = 0L;
                File[] children = path.toFile().listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (child.isFile()) {
                            total += child.length();
                        }
                    }
                }
                return total;
            }
        } catch (Exception ignored) {
            // Unknown size
        }
        return 0L;
    }

    private static long parseOllamaModified(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            String cleaned = value;
            if (cleaned.contains(".")) {
                cleaned = cleaned.substring(0, cleaned.indexOf('.'));
            }
            if (cleaned.endsWith("Z")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            cleaned = cleaned.replace('T', ' ');
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                    cleaned.replace(' ', 'T'));
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) {
            return "";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
