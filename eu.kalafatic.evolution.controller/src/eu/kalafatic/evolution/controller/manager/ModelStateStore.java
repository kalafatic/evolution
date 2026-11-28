package eu.kalafatic.evolution.controller.manager;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * Persistent user metadata and explicit unregistration state for models.
 *
 * <p>This store deliberately does <b>not</b> register models and is not a model
 * registry. It records only data that has no other authoritative home in the
 * existing EMF configuration:</p>
 * <ul>
 *   <li>user-defined display name, description, tags and aliases,</li>
 *   <li>explicit "unregistered" markers so discovery cannot silently resurrect
 *       a model that EVO removed.</li>
 * </ul>
 *
 * <p>The file format is {@link Properties}-based on purpose: the controller
 * bundle has no OSGi import for an external JSON library, so relying on one
 * here would make unregistration markers silently unavailable in the running
 * product.</p>
 */
public final class ModelStateStore {

    private static final String STATE_FILE_PROPERTY = "evo.model.state.file";
    private static final String DEFAULT_FILE_NAME = "model-state.properties";
    private static final String KEY_PREFIX = "model.";
    private static final String FIELD_SEPARATOR = "\u001F";
    private static final String LIST_SEPARATOR = "\u001E";
    private static final String PARAM_SEPARATOR = "\u001D";

    private static volatile Path stateFileOverride;

    private static final ModelStateStore INSTANCE = new ModelStateStore();

    private final Map<String, Metadata> models = new LinkedHashMap<>();
    private Path file;
    private boolean loaded;

    private ModelStateStore() {
    }

    public static ModelStateStore getInstance() {
        return INSTANCE;
    }

    /**
     * Test seam: point the store at a temporary file and clear in-memory state.
     */
    public static synchronized void setStateFileForTesting(Path path) {
        stateFileOverride = path;
        INSTANCE.reset();
    }

    public static synchronized void resetStateFile() {
        stateFileOverride = null;
        INSTANCE.reset();
    }

    private synchronized void reset() {
        models.clear();
        loaded = false;
        file = resolveFile();
    }

    public synchronized Path getStateFile() {
        if (file == null) {
            file = resolveFile();
        }
        return file;
    }

    private static Path resolveFile() {
        if (stateFileOverride != null) {
            return stateFileOverride.toAbsolutePath().normalize();
        }
        String override = System.getProperty(STATE_FILE_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Paths.get(override.trim()).toAbsolutePath().normalize();
        }
        try {
            Path modelsDir = ResourceManager.getInstance().getPath(EvoPath.MODELS);
            if (modelsDir != null) {
                return modelsDir.resolve(DEFAULT_FILE_NAME).toAbsolutePath().normalize();
            }
        } catch (Exception e) {
            Log.log("[MODEL-STATE] Could not resolve the models directory: " + e.getMessage());
        }
        String userHome = System.getProperty("user.home", ".");
        return Paths.get(userHome, ".evo-model-state.json").toAbsolutePath().normalize();
    }

    public synchronized Metadata get(String canonicalId) {
        ensureLoaded();
        Metadata metadata = models.get(key(canonicalId));
        return metadata != null ? metadata.copy() : null;
    }

    public synchronized boolean isSuppressed(String canonicalId) {
        Metadata metadata = get(canonicalId);
        return metadata != null && metadata.isSuppressed();
    }

    /**
     * @return true when the metadata was persisted successfully.
     */
    public synchronized boolean put(String canonicalId, Metadata metadata) {
        ensureLoaded();
        if (canonicalId == null || canonicalId.isBlank() || metadata == null) {
            return false;
        }
        models.put(key(canonicalId), metadata.copy());
        return save();
    }

    /**
     * @return true when the unregistration marker was persisted successfully.
     */
    public synchronized boolean suppress(String canonicalId, String reason) {
        ensureLoaded();
        if (canonicalId == null || canonicalId.isBlank()) {
            return false;
        }
        Metadata metadata = models.computeIfAbsent(key(canonicalId), k -> new Metadata());
        metadata.setSuppressed(true);
        metadata.setSuppressionReason(reason != null ? reason : "");
        metadata.setSuppressedAt(System.currentTimeMillis());
        return save();
    }

    public synchronized void unsuppress(String canonicalId) {
        ensureLoaded();
        Metadata metadata = models.get(key(canonicalId));
        if (metadata != null) {
            metadata.setSuppressed(false);
            metadata.setSuppressionReason("");
            metadata.setSuppressedAt(0L);
            save();
        }
    }

    public synchronized void remove(String canonicalId) {
        ensureLoaded();
        if (models.remove(key(canonicalId)) != null) {
            save();
        }
    }

    /**
     * Moves all metadata from one identity to another. Used by artifact rename.
     */
    public synchronized void move(String oldCanonicalId, String newCanonicalId) {
        ensureLoaded();
        Metadata metadata = models.remove(key(oldCanonicalId));
        if (metadata != null && newCanonicalId != null && !newCanonicalId.isBlank()) {
            models.put(key(newCanonicalId), metadata);
            save();
        }
    }

    public synchronized Map<String, Metadata> snapshot() {
        ensureLoaded();
        Map<String, Metadata> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Metadata> entry : models.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return Collections.unmodifiableMap(copy);
    }

    public synchronized void reload() {
        models.clear();
        loaded = false;
        ensureLoaded();
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        Path f = getStateFile();
        loaded = true;
        if (f == null || !Files.isRegularFile(f)) {
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (Exception e) {
            Log.log("[MODEL-STATE] Failed to load model state from " + f + ": " + e.getMessage());
            return;
        }
        for (String name : properties.stringPropertyNames()) {
            if (!name.startsWith(KEY_PREFIX)) {
                continue;
            }
            String canonical = name.substring(KEY_PREFIX.length());
            try {
                models.put(canonical, Metadata.decode(properties.getProperty(name, "")));
            } catch (Exception e) {
                Log.log("[MODEL-STATE] Ignoring malformed entry '" + canonical + "': " + e.getMessage());
            }
        }
    }

    private boolean save() {
        Path f = getStateFile();
        if (f == null) {
            return false;
        }
        try {
            Properties properties = new Properties();
            properties.setProperty("version", "2");
            for (Map.Entry<String, Metadata> entry : models.entrySet()) {
                properties.setProperty(KEY_PREFIX + entry.getKey(), entry.getValue().encode());
            }
            Path parent = f.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = f.resolveSibling(f.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                properties.store(writer, "EVO model state (metadata + unregistration markers)");
            }
            try {
                Files.move(temp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                Files.move(temp, f, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            Log.log("[MODEL-STATE] Failed to persist model state to " + f + ": " + e.getMessage());
            return false;
        }
    }

    private static String key(String canonicalId) {
        return canonicalId != null ? canonicalId.trim() : "";
    }

    /**
     * User-editable metadata for a canonical model identity.
     */
    public static final class Metadata {
        private String displayName = "";
        private String description = "";
        private final List<String> tags = new ArrayList<>();
        private final List<String> aliases = new ArrayList<>();
        private final Map<String, String> parameters = new LinkedHashMap<>();
        private boolean suppressed;
        private String suppressionReason = "";
        private long suppressedAt;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName != null ? displayName.trim() : "";
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description != null ? description : "";
        }

        public List<String> getTags() {
            return tags;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public boolean isSuppressed() {
            return suppressed;
        }

        public void setSuppressed(boolean suppressed) {
            this.suppressed = suppressed;
        }

        public String getSuppressionReason() {
            return suppressionReason;
        }

        public void setSuppressionReason(String suppressionReason) {
            this.suppressionReason = suppressionReason != null ? suppressionReason : "";
        }

        public long getSuppressedAt() {
            return suppressedAt;
        }

        public void setSuppressedAt(long suppressedAt) {
            this.suppressedAt = suppressedAt;
        }

        public Metadata copy() {
            Metadata copy = new Metadata();
            copy.displayName = displayName;
            copy.description = description;
            copy.tags.addAll(tags);
            copy.aliases.addAll(aliases);
            copy.parameters.putAll(parameters);
            copy.suppressed = suppressed;
            copy.suppressionReason = suppressionReason;
            copy.suppressedAt = suppressedAt;
            return copy;
        }

        private String encode() {
            StringBuilder builder = new StringBuilder();
            builder.append(escape(displayName)).append(FIELD_SEPARATOR)
                    .append(escape(description)).append(FIELD_SEPARATOR)
                    .append(suppressed ? "1" : "0").append(FIELD_SEPARATOR)
                    .append(escape(suppressionReason)).append(FIELD_SEPARATOR)
                    .append(suppressedAt).append(FIELD_SEPARATOR)
                    .append(escapeList(tags)).append(FIELD_SEPARATOR)
                    .append(escapeList(aliases)).append(FIELD_SEPARATOR)
                    .append(escapeParameters(parameters));
            return builder.toString();
        }

        private static Metadata decode(String value) {
            Metadata metadata = new Metadata();
            String[] fields = value.split(FIELD_SEPARATOR, -1);
            if (fields.length > 0) {
                metadata.displayName = unescape(fields[0]);
            }
            if (fields.length > 1) {
                metadata.description = unescape(fields[1]);
            }
            if (fields.length > 2) {
                metadata.suppressed = "1".equals(fields[2]);
            }
            if (fields.length > 3) {
                metadata.suppressionReason = unescape(fields[3]);
            }
            if (fields.length > 4) {
                try {
                    metadata.suppressedAt = Long.parseLong(fields[4]);
                } catch (NumberFormatException ignored) {
                    metadata.suppressedAt = 0L;
                }
            }
            if (fields.length > 5) {
                metadata.tags.addAll(unescapeList(fields[5]));
            }
            if (fields.length > 6) {
                metadata.aliases.addAll(unescapeList(fields[6]));
            }
            if (fields.length > 7) {
                metadata.parameters.putAll(unescapeParameters(fields[7]));
            }
            return metadata;
        }

        private static String escape(String value) {
            return value != null ? value : "";
        }

        private static String unescape(String value) {
            return value != null ? value : "";
        }

        private static String escapeList(List<String> values) {
            StringBuilder builder = new StringBuilder();
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(LIST_SEPARATOR);
                }
                builder.append(escape(value));
            }
            return builder.toString();
        }

        private static List<String> unescapeList(String value) {
            List<String> result = new ArrayList<>();
            if (value == null || value.isEmpty()) {
                return result;
            }
            for (String part : value.split(LIST_SEPARATOR, -1)) {
                if (!part.isEmpty()) {
                    result.add(part);
                }
            }
            return result;
        }

        private static String escapeParameters(Map<String, String> parameters) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(LIST_SEPARATOR);
                }
                builder.append(escape(entry.getKey())).append(PARAM_SEPARATOR)
                        .append(escape(entry.getValue()));
            }
            return builder.toString();
        }

        private static Map<String, String> unescapeParameters(String value) {
            Map<String, String> result = new LinkedHashMap<>();
            if (value == null || value.isEmpty()) {
                return result;
            }
            for (String part : value.split(LIST_SEPARATOR, -1)) {
                int index = part.indexOf(PARAM_SEPARATOR);
                if (index >= 0) {
                    result.put(part.substring(0, index), part.substring(index + 1));
                }
            }
            return result;
        }
    }
}
