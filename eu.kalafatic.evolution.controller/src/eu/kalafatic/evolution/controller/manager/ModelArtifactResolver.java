package eu.kalafatic.evolution.controller.manager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Resolves every physical artifact that belongs to one canonical model.
 *
 * <p>A forged EVO model is commonly represented by several files and
 * directories with the same stem:</p>
 * <pre>
 *   evo-x.gguf
 *   evo-x.gguf.ai.json
 *   evo-x.evo
 *   evo-x.evo/                (directory)
 *   evo-x.evo.ai.json
 *   evo-x_vocab.json
 *   dist/evo-x/               (exported bundle)
 *   forge-output/evo-x/
 * </pre>
 *
 * <p>Deleting only the first resolved path leaves the remaining
 * representations to be discovered again. This resolver deliberately matches
 * by exact stem (never by prefix), only inside EVO-owned model roots, and
 * never descends into Ollama blob storage.</p>
 */
public final class ModelArtifactResolver {

    private static final String[] KNOWN_SUFFIXES = {
            ".gguf.ai.json",
            ".evo.ai.json",
            ".gguf",
            ".evo",
            ".bin",
            "_vocab.json",
            "-vocab.json",
            ".json"
    };

    private ModelArtifactResolver() {
    }

    /**
     * Returns all owned artifacts for the identity, deepest entries removed when
     * a parent directory is already included.
     */
    public static List<Path> resolveOwnedArtifacts(ModelIdentity identity) {
        if (identity == null
                || identity.getKind() == ModelIdentity.Kind.REMOTE
                || identity.getKind() == ModelIdentity.Kind.UNKNOWN) {
            return List.of();
        }
        Set<String> stems = candidateStems(identity);
        if (stems.isEmpty()) {
            return List.of();
        }
        Set<Path> matches = new LinkedHashSet<>();
        for (Path root : ModelIdentityResolver.modelSearchDirectories()) {
            if (root == null || !Files.isDirectory(root)) {
                continue;
            }
            File[] children = root.toFile().listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                String name = child.getName();
                if (name.startsWith(".")) {
                    // Hidden metadata directories such as workspace/models/.evo
                    continue;
                }
                String stem = stem(name);
                if (stems.contains(stem.toLowerCase(Locale.ROOT))) {
                    matches.add(child.toPath().toAbsolutePath().normalize());
                }
            }
        }
        // Include an explicitly resolved path even when its parent is not one of
        // the enumerated roots (it is still ownership-checked below).
        if (identity.getArtifactPath() != null) {
            Path direct = identity.getArtifactPath().toAbsolutePath().normalize();
            if (ModelIdentityResolver.isOwnedArtifact(direct)) {
                matches.add(direct);
            }
        }
        return pruneNested(matches);
    }

    /**
     * True when at least one physical artifact belongs to the identity.
     */
    public static boolean hasOwnedArtifacts(ModelIdentity identity) {
        return !resolveOwnedArtifacts(identity).isEmpty();
    }

    public static boolean isSharedGenericArtifact(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return lower.equals("evo.gguf") || lower.equals("evo.evo");
    }

    /**
     * Strips known model-artifact suffixes to obtain the logical stem.
     */
    public static String stem(String fileName) {
        if (fileName == null) {
            return "";
        }
        String value = fileName;
        String lower = value.toLowerCase(Locale.ROOT);
        for (String suffix : KNOWN_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                value = value.substring(0, value.length() - suffix.length());
                lower = value.toLowerCase(Locale.ROOT);
                break;
            }
        }
        return value;
    }

    private static Set<String> candidateStems(ModelIdentity identity) {
        Set<String> stems = new TreeSet<>();
        addStem(stems, identity.getTechnicalId());
        addStem(stems, identity.getRuntimeName());
        if (identity.getArtifactPath() != null && identity.getArtifactPath().getFileName() != null) {
            addStem(stems, identity.getArtifactPath().getFileName().toString());
        }
        // Anything after an explicit Ollama tag is not part of the artifact stem.
        return stems;
    }

    private static void addStem(Set<String> stems, String value) {
        if (value == null) {
            return;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return;
        }
        // Only the implicit ":latest" tag is an alias of the same artifact. Other
        // tags may legitimately share a base GGUF and must not cause it to be
        // deleted when a single tagged runtime model is removed.
        int slash = normalized.lastIndexOf('/');
        int colon = normalized.lastIndexOf(':');
        if (colon > slash && normalized.toLowerCase(Locale.ROOT).endsWith(":latest")) {
            normalized = normalized.substring(0, colon);
        }
        String stem = stem(normalized);
        if (!stem.isEmpty()) {
            stems.add(stem.toLowerCase(Locale.ROOT));
        }
    }

    private static List<Path> pruneNested(Set<Path> paths) {
        List<Path> sorted = new ArrayList<>(paths);
        sorted.sort(Comparator.comparingInt(Path::getNameCount));
        List<Path> result = new ArrayList<>();
        for (Path candidate : sorted) {
            boolean covered = false;
            for (Path existing : result) {
                if (candidate.startsWith(existing) && !candidate.equals(existing)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                result.add(candidate);
            }
        }
        result.sort(Comparator.comparing(Path::toString));
        return result;
    }
}
