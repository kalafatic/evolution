package eu.kalafatic.evolution.controller.manager;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Canonical identity of a model as understood by the EVO model lifecycle.
 *
 * <p>EVO models can be represented by several identifiers at the same time
 * (display name, Ollama tag, EVO Native artifact filename, absolute artifact
 * path, static provider name). This class captures the resolved relationship
 * between those representations without turning them into unrelated models.</p>
 *
 * <p>The canonical identifier is deliberately provider-aware:</p>
 * <ul>
 *   <li>Ollama models are keyed by endpoint + tag-explicit runtime name, so
 *       {@code llama3} and {@code llama3:latest} resolve to the same identity
 *       while {@code llama3:7b} remains a distinct model.</li>
 *   <li>Artifact-backed models (EVO Native / GGUF / demo) are keyed by the
 *       normalized absolute artifact path.</li>
 *   <li>Remote providers are keyed by provider identifier.</li>
 * </ul>
 *
 * <p>This is intentionally <b>not</b> a model registry. It is a value object
 * produced by {@link ModelIdentityResolver} from the existing EMF provider
 * entries and discovery results.</p>
 */
public final class ModelIdentity {

    public enum Kind {
        OLLAMA("Ollama"),
        EVO_NATIVE("EVO Native"),
        GGUF("GGUF"),
        DEMO("Demo"),
        REMOTE("Remote"),
        UNKNOWN("Unknown");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String canonicalId;
    private final String technicalId;
    private final String runtimeName;
    private final Kind kind;
    private final String endpoint;
    private final Path artifactPath;
    private final String providerId;
    private final boolean local;
    private final Set<String> aliases;

    public ModelIdentity(String canonicalId, String technicalId, String runtimeName, Kind kind,
                         String endpoint, Path artifactPath, String providerId, boolean local,
                         Set<String> aliases) {
        this.canonicalId = canonicalId != null ? canonicalId : "";
        this.technicalId = technicalId != null ? technicalId : "";
        this.runtimeName = runtimeName != null ? runtimeName : this.technicalId;
        this.kind = kind != null ? kind : Kind.UNKNOWN;
        this.endpoint = endpoint != null ? endpoint : "";
        this.artifactPath = artifactPath;
        this.providerId = providerId != null ? providerId : "";
        this.local = local;
        this.aliases = aliases != null
                ? Collections.unmodifiableSet(new LinkedHashSet<>(aliases))
                : Collections.emptySet();
    }

    /** Stable key used for persistence, suppression and de-duplication. */
    public String getCanonicalId() {
        return canonicalId;
    }

    /** The technical/provider identifier (Ollama tag, artifact filename, provider name). */
    public String getTechnicalId() {
        return technicalId;
    }

    /** The identifier to pass to the runtime (Ollama tag with explicit {@code :latest}). */
    public String getRuntimeName() {
        return runtimeName;
    }

    public Kind getKind() {
        return kind;
    }

    public String getEndpoint() {
        return endpoint;
    }

    /** The owned or referenced artifact path, or {@code null} when not artifact-backed. */
    public Path getArtifactPath() {
        return artifactPath;
    }

    public String getProviderId() {
        return providerId;
    }

    public boolean isLocal() {
        return local;
    }

    /** All alternative identifiers that must be considered during removal. */
    public Set<String> getAliases() {
        return aliases;
    }

    public boolean isArtifactBacked() {
        return kind == Kind.EVO_NATIVE || kind == Kind.GGUF || kind == Kind.DEMO;
    }

    public boolean isOllama() {
        return kind == Kind.OLLAMA;
    }

    @Override
    public String toString() {
        return canonicalId;
    }
}
