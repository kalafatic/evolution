package eu.kalafatic.evolution.controller.manager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Resolves the canonical {@link ModelIdentity} for an {@link AIProvider} entry
 * produced by discovery or stored in the EMF configuration.
 *
 * <p>This class is the single place where EVO distinguishes:</p>
 * <ul>
 *   <li>the canonical model identity,</li>
 *   <li>the runtime (Ollama) identifier,</li>
 *   <li>the physical artifact path,</li>
 *   <li>the provider/registration identifier,</li>
 *   <li>historical aliases.</li>
 * </ul>
 *
 * <p>It does not register, persist or delete anything. The lifecycle services
 * consume the resolved identity.</p>
 */
public final class ModelIdentityResolver {

    public static final String DEFAULT_OLLAMA_ENDPOINT = "http://localhost:11434";

    private ModelIdentityResolver() {
    }

    public static ModelIdentity resolve(Orchestrator orchestrator, AIProvider provider) {
        if (provider == null) {
            return new ModelIdentity("unknown", "", "", ModelIdentity.Kind.UNKNOWN, "", null, "", false, Set.of());
        }

        String rawName = provider.getName() != null ? provider.getName().trim() : "";
        String format = provider.getFormat() != null ? provider.getFormat().trim().toLowerCase(Locale.ROOT) : "";
        boolean local = provider.isLocal();
        ModelIdentity.Kind kind = determineKind(provider, format, rawName);
        String endpoint = resolveEndpoint(orchestrator, provider, kind);
        Path artifactPath = resolveArtifactPath(provider, kind, rawName);

        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(rawName);
        if (!rawName.isEmpty()) {
            aliases.add(rawName.toLowerCase(Locale.ROOT));
        }

        String technicalId;
        String runtimeName;
        String canonicalId;
        String providerId = "unknown";

        switch (kind) {
            case OLLAMA: {
                String effectiveName = normalizeOllamaTechnicalId(rawName);
                technicalId = effectiveName;
                runtimeName = toOllamaRuntimeName(effectiveName);
                aliases.addAll(ollamaAliases(rawName));
                aliases.addAll(ollamaAliases(effectiveName));
                if (artifactPath != null) {
                    aliases.add(artifactPath.toString());
                    aliases.add(artifactPath.getFileName().toString());
                }
                canonicalId = canonicalOllamaId(endpoint, effectiveName);
                providerId = "ollama";
                break;
            }
            case EVO_NATIVE:
            case GGUF:
            case DEMO: {
                Path path = artifactPath;
                technicalId = rawName.isEmpty() && path != null ? path.getFileName().toString() : rawName;
                runtimeName = technicalId;
                if (path != null) {
                    String abs = path.toAbsolutePath().normalize().toString();
                    canonicalId = "artifact|" + abs;
                    aliases.add(abs);
                    aliases.add(path.getFileName().toString());
                    addWithoutExtension(aliases, path.getFileName().toString());
                    if (path.getParent() != null) {
                        aliases.add(path.getParent().getFileName().toString());
                    }
                } else {
                    canonicalId = "artifact-name|" + kind.name().toLowerCase(Locale.ROOT) + "|"
                            + technicalId.toLowerCase(Locale.ROOT);
                }
                providerId = kind == ModelIdentity.Kind.EVO_NATIVE ? "evo_native" : "local-file";
                break;
            }
            case REMOTE: {
                technicalId = rawName;
                runtimeName = rawName;
                canonicalId = "remote|" + rawName.toLowerCase(Locale.ROOT);
                providerId = rawName;
                break;
            }
            default: {
                technicalId = rawName;
                runtimeName = rawName;
                canonicalId = "unknown|" + rawName.toLowerCase(Locale.ROOT);
                break;
            }
        }

        aliases.remove("");
        return new ModelIdentity(canonicalId, technicalId, runtimeName, kind, endpoint, artifactPath,
                providerId, local, aliases);
    }

    public static ModelIdentity.Kind determineKind(AIProvider provider, String format, String rawName) {
        if (provider == null) {
            return ModelIdentity.Kind.UNKNOWN;
        }
        if (!provider.isLocal()) {
            return ModelIdentity.Kind.REMOTE;
        }
        String lower = rawName != null ? rawName.toLowerCase(Locale.ROOT) : "";
        if ("evo_native".equals(format) || lower.endsWith(".evo")) {
            return ModelIdentity.Kind.EVO_NATIVE;
        }
        if (lower.startsWith("demo/")) {
            return ModelIdentity.Kind.DEMO;
        }
        if (lower.endsWith(".gguf")) {
            return ModelIdentity.Kind.GGUF;
        }
        return ModelIdentity.Kind.OLLAMA;
    }

    public static String resolveEndpoint(Orchestrator orchestrator, AIProvider provider, ModelIdentity.Kind kind) {
        if (kind == ModelIdentity.Kind.REMOTE || kind == ModelIdentity.Kind.UNKNOWN) {
            return provider != null && provider.getUrl() != null ? provider.getUrl() : "";
        }
        if (provider != null && provider.getUrl() != null) {
            String url = provider.getUrl().trim();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return normalizeEndpoint(url);
            }
        }
        if (orchestrator != null && orchestrator.getOllama() != null
                && orchestrator.getOllama().getUrl() != null
                && !orchestrator.getOllama().getUrl().isBlank()) {
            return normalizeEndpoint(orchestrator.getOllama().getUrl());
        }
        return DEFAULT_OLLAMA_ENDPOINT;
    }

    public static String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return DEFAULT_OLLAMA_ENDPOINT;
        }
        String e = endpoint.trim();
        while (e.endsWith("/")) {
            e = e.substring(0, e.length() - 1);
        }
        return e;
    }

    /**
     * Removes the EVO provider prefix {@code ollama:} that is persisted in some
     * EMF registrations (for example {@code ollama:deepseek-coder...}). The
     * Ollama runtime identifier is the remainder. A model literally named
     * {@code ollama:...} is not a supported EVO naming convention.
     */
    public static String normalizeOllamaTechnicalId(String name) {
        if (name == null) {
            return "";
        }
        String n = name.trim();
        if (n.regionMatches(true, 0, "ollama:", 0, "ollama:".length())) {
            String remainder = n.substring("ollama:".length()).trim();
            if (!remainder.isEmpty()) {
                return remainder;
            }
        }
        return n;
    }

    /**
     * Expands an implicit Ollama tag to the explicit runtime identifier.
     * This is the value that must be passed to unload/delete operations.
     */
    public static String toOllamaRuntimeName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String n = name.trim();
        int slash = n.lastIndexOf('/');
        int colon = n.lastIndexOf(':');
        boolean hasTag = colon > slash;
        return hasTag ? n : n + ":latest";
    }

    /**
     * Identity key for an Ollama model. An untagged name and its {@code :latest}
     * form share the same key; other explicit tags stay distinct.
     */
    public static String canonicalOllamaId(String endpoint, String name) {
        String runtime = toOllamaRuntimeName(name);
        return "ollama|" + normalizeEndpoint(endpoint).toLowerCase(Locale.ROOT) + "|" + runtime.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the alias forms of an Ollama reference. This intentionally does not
     * collapse legitimate non-latest tags.
     */
    public static Set<String> ollamaAliases(String name) {
        Set<String> aliases = new LinkedHashSet<>();
        if (name == null || name.isBlank()) {
            return aliases;
        }
        String n = name.trim();
        aliases.add(n);
        String runtime = toOllamaRuntimeName(n);
        aliases.add(runtime);
        if (runtime.toLowerCase(Locale.ROOT).endsWith(":latest")) {
            aliases.add(runtime.substring(0, runtime.length() - ":latest".length()));
        }
        return aliases;
    }

    /**
     * True when two Ollama references denote the same runtime model
     * (implicit {@code :latest} expansion, case-insensitive).
     */
    public static boolean ollamaNamesEquivalent(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String ra = toOllamaRuntimeName(a).toLowerCase(Locale.ROOT);
        String rb = toOllamaRuntimeName(b).toLowerCase(Locale.ROOT);
        return !ra.isEmpty() && ra.equals(rb);
    }

    /**
     * True when the provider reference matches one of the identity aliases.
     * Artifact-backed identities are only matched by artifact aliases, never by
     * a bare Ollama-looking name, so an {@code .evo} artifact is not silently
     * treated as an Ollama model with a similar name.
     */
    public static boolean matchesAlias(ModelIdentity identity, String candidate) {
        if (identity == null || candidate == null || candidate.isBlank()) {
            return false;
        }
        String c = candidate.trim();
        if (identity.getAliases().contains(c)
                || identity.getAliases().contains(c.toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (identity.isOllama() && ollamaNamesEquivalent(identity.getTechnicalId(), c)) {
            return true;
        }
        if (identity.getArtifactPath() != null) {
            try {
                Path p = Paths.get(c).toAbsolutePath().normalize();
                if (p.equals(identity.getArtifactPath().toAbsolutePath().normalize())) {
                    return true;
                }
            } catch (Exception ignored) {
                // Not a path
            }
        }
        return false;
    }

    private static void addWithoutExtension(Set<String> aliases, String fileName) {
        if (fileName == null) {
            return;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            aliases.add(fileName.substring(0, dot));
        }
    }

    /**
     * Resolves the artifact that backs a model, if one can be identified.
     * For pure Ollama registrations this returns {@code null} because Ollama
     * owns the storage layout; deleting blob files directly is not supported.
     */
    public static Path resolveArtifactPath(AIProvider provider, ModelIdentity.Kind kind, String rawName) {
        if (provider == null) {
            return null;
        }
        if (kind == ModelIdentity.Kind.REMOTE || kind == ModelIdentity.Kind.UNKNOWN) {
            return null;
        }

        Path direct = existingPath(provider.getUrl());
        if (direct != null && (Files.isRegularFile(direct) || Files.isDirectory(direct))) {
            if (kind != ModelIdentity.Kind.OLLAMA || isDirectArtifactReference(provider, direct)) {
                return direct;
            }
        }

        String name = rawName != null ? rawName.trim() : "";
        if (name.isEmpty()) {
            return null;
        }

        if (kind == ModelIdentity.Kind.EVO_NATIVE) {
            File resolved = OllamaProvider.resolveEvoArtifactPath(name);
            if (resolved != null) {
                return resolved.toPath().toAbsolutePath().normalize();
            }
            return findByFileName(name, true);
        }
        if (kind == ModelIdentity.Kind.GGUF) {
            return findByFileName(name, false);
        }
        if (kind == ModelIdentity.Kind.DEMO) {
            String demoName = name.startsWith("demo/") ? name.substring("demo/".length()) : name;
            return findByFileName(demoName + ".gguf", false);
        }

        // Ollama: only report a loose local GGUF artifact; blobs are runtime-owned.
        Path loose = findByFileName(name + ".gguf", false);
        if (loose != null && isUnderOllamaHome(loose)) {
            return null;
        }
        return loose;
    }

    private static boolean isDirectArtifactReference(AIProvider provider, Path path) {
        String url = provider.getUrl();
        return url != null && !(url.startsWith("http://") || url.startsWith("https://"))
                && (url.toLowerCase(Locale.ROOT).endsWith(".evo")
                        || url.toLowerCase(Locale.ROOT).endsWith(".gguf"));
    }

    private static Path existingPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return null;
        }
        try {
            Path p = Paths.get(v);
            return p.isAbsolute() ? p.normalize() : p.toAbsolutePath().normalize();
        } catch (Exception e) {
            return null;
        }
    }

    private static Path findByFileName(String fileName, boolean evoOnly) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        for (Path dir : modelSearchDirectories()) {
            if (dir == null || !Files.isDirectory(dir)) {
                continue;
            }
            Path direct = dir.resolve(fileName);
            if (Files.exists(direct)) {
                return direct.toAbsolutePath().normalize();
            }
            if (!evoOnly) {
                Path gguf = dir.resolve(fileName.endsWith(".gguf") ? fileName : fileName + ".gguf");
                if (Files.exists(gguf)) {
                    return gguf.toAbsolutePath().normalize();
                }
            }
            Path evo = dir.resolve(fileName.endsWith(".evo") ? fileName : fileName + ".evo");
            if (Files.exists(evo)) {
                return evo.toAbsolutePath().normalize();
            }
            Path directoryArtifact = dir.resolve(fileName);
            if (Files.isDirectory(directoryArtifact)) {
                return directoryArtifact.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    /**
     * Directories that may contain EVO-owned artifacts. This intentionally
     * excludes Ollama blob storage.
     */
    public static List<Path> modelSearchDirectories() {
        List<Path> dirs = new ArrayList<>();
        addIfPresent(dirs, LlamaService.resolveControllerModelsDir());
        addIfPresent(dirs, LlamaService.resolveLlamaCppLibDir());
        for (File f : LlamaService.resolveAllTargetModelDirs()) {
            addIfPresent(dirs, f);
        }
        try {
            Path workspace = ResourceManager.getInstance().getPath(EvoPath.WORKSPACE);
            if (workspace != null) {
                dirs.add(workspace.resolve("forge-output"));
                dirs.add(workspace.resolve("forged").resolve("output"));
                dirs.add(workspace.resolve("source").resolve("models"));
                dirs.add(workspace.resolve("models"));
            }
        } catch (Exception ignored) {
            // ResourceManager may not be initialised in headless tests
        }
        try {
            String codebase = ProjectModelManager.getCodebasePath();
            if (codebase != null && !codebase.isBlank()) {
                Path base = Paths.get(codebase);
                dirs.add(base.resolve("forge-output"));
                dirs.add(base.resolve("source").resolve("models"));
                dirs.add(base.resolve("dist"));
            }
        } catch (Exception ignored) {
            // Optional
        }
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            dirs.add(Paths.get(userDir, "forge-output"));
        }
        String extra = System.getProperty("evo.model.ownership.roots");
        if (extra != null && !extra.isBlank()) {
            for (String part : extra.split(File.pathSeparator)) {
                if (!part.isBlank()) {
                    dirs.add(Paths.get(part.trim()));
                }
            }
        }
        return dirs;
    }

    private static void addIfPresent(List<Path> dirs, File file) {
        if (file != null) {
            dirs.add(file.toPath().toAbsolutePath().normalize());
        }
    }

    /**
     * Ownership check: an artifact may only be deleted when it lives in an
     * EVO-owned model directory. Ollama blob storage is never owned by EVO.
     */
    public static boolean isOwnedArtifact(Path artifactPath) {
        if (artifactPath == null) {
            return false;
        }
        Path normalized = artifactPath.toAbsolutePath().normalize();
        if (isUnderOllamaHome(normalized)) {
            return false;
        }
        String lower = normalized.toString().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".ollama") || lower.endsWith(".ollama/models")) {
            return false;
        }
        for (Path root : modelSearchDirectories()) {
            if (root == null) {
                continue;
            }
            Path r = root.toAbsolutePath().normalize();
            if (normalized.startsWith(r) && !normalized.equals(r)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUnderOllamaHome(Path path) {
        if (path == null) {
            return false;
        }
        Path ollamaHome = Paths.get(System.getProperty("user.home", ""), ".ollama").toAbsolutePath().normalize();
        return path.toAbsolutePath().normalize().startsWith(ollamaHome);
    }
}
