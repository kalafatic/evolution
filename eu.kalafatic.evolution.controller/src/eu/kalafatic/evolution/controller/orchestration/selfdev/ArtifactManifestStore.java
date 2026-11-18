package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;

/**
 * Authoritative per-run artifact provenance store (WP4).
 *
 * One store per run, persisted at &lt;run&gt;/export/artifact-manifest.json.
 * Build/export/deploy/promote/rollback MUST resolve artifacts through this store
 * instead of first-match filesystem scans. Every recorded artifact carries:
 * runId, buildId, revision, path, size, SHA-256, timestamp, productId, OS,
 * architecture and build/test/export result markers.
 */
public class ArtifactManifestStore {

    public static final String MANIFEST_FILE_NAME = "artifact-manifest.json";

    private final File manifestFile;
    private final String runId;
    private final AtomicLong buildSequence = new AtomicLong(0);
    private final Map<ArtifactType, BuildArtifact> entries = new ConcurrentHashMap<>();

    public ArtifactManifestStore(SelfDevContext context) {
        this.runId = context != null ? context.getRunId() : "unknown-run";
        File exportDir = context != null ? context.getExportDirectory() : null;
        this.manifestFile = exportDir != null ? new File(exportDir, MANIFEST_FILE_NAME) : null;
        loadFromDisk();
    }

    public File getManifestFile() {
        return manifestFile;
    }

    public synchronized String nextBuildId() {
        long seq = buildSequence.incrementAndGet();
        return runId + "-build-" + seq;
    }

    /**
     * Records (or re-records) the authoritative identity of an artifact.
     * Computes size and SHA-256 from the artifact file; persists the manifest.
     */
    public synchronized BuildArtifact record(ArtifactType type, File path, String sourceRevision,
                                             String buildId, String buildResult, String testResult,
                                             String exportResult, Map<String, String> extraMetadata) {
        if (type == null || path == null || !path.exists()) {
            throw new IllegalArgumentException("Artifact manifest requires an existing artifact path for type " + type);
        }
        String sha = computeSha256(path);
        long size = path.isDirectory() ? directorySize(path) : path.length();

        Map<String, String> metadata = new LinkedHashMap<>();
        if (extraMetadata != null) {
            metadata.putAll(extraMetadata);
        }
        BuildArtifact artifact = new BuildArtifact(type, path, sourceRevision,
                metadata.get("platform"), metadata, runId, buildId, size, sha,
                buildResult, testResult, exportResult);
        entries.put(type, artifact);
        persist();
        return artifact;
    }

    /**
     * Resolves the recorded artifact for the given type and verifies that the
     * file still exists and matches the recorded checksum. Returns null when no
     * manifest entry exists for the type (callers must treat this as missing).
     *
     * @throws IOException when the recorded artifact is stale or its checksum mismatches.
     */
    public synchronized BuildArtifact resolve(ArtifactType type) throws IOException {
        BuildArtifact recorded = entries.get(type);
        if (recorded == null) {
            return null;
        }
        File path = recorded.getPath();
        if (path == null || !path.exists()) {
            throw new IOException("Recorded artifact for " + type + " is missing on disk: "
                    + (path != null ? path.getAbsolutePath() : "null") + " (stale manifest entry)");
        }
        String currentSha = computeSha256(path);
        if (currentSha == null || !currentSha.equalsIgnoreCase(recorded.getSha256())) {
            throw new IOException("Checksum mismatch for recorded artifact " + type + " at " + path.getAbsolutePath()
                    + ": manifest=" + recorded.getSha256() + " actual=" + currentSha);
        }
        return recorded;
    }

    /**
     * Verifies integrity of an artifact (used for promoted/restored artifacts
     * that carry their own manifest entry).
     */
    public synchronized boolean verifyIntegrity(BuildArtifact artifact) {
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return false;
        }
        try {
            String sha = computeSha256(artifact.getPath());
            return sha != null && sha.equalsIgnoreCase(artifact.getSha256());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Stores an already-provenanced artifact (e.g. a promoted copy that must
     * preserve its original identity) under a new path.
     */
    public synchronized BuildArtifact rehome(BuildArtifact source, File newPath, String exportResult) {
        if (source == null || newPath == null || !newPath.exists()) {
            return null;
        }
        String sha = computeSha256(newPath);
        BuildArtifact rehomed = new BuildArtifact(source.getType(), newPath, source.getSourceRevision(),
                source.getPlatform(), source.getMetadata(), source.getRunId(), source.getBuildId(),
                newPath.isDirectory() ? directorySize(newPath) : newPath.length(), sha,
                source.getBuildResult(), source.getTestResult(), exportResult);
        entries.put(source.getType(), rehomed);
        persist();
        return rehomed;
    }

    private synchronized void loadFromDisk() {
        if (manifestFile == null || !manifestFile.exists()) {
            return;
        }
        try {
            String json = Files.readString(manifestFile.toPath(), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            for (ArtifactType type : ArtifactType.values()) {
                JSONObject entry = root.optJSONObject(type.name());
                if (entry == null) {
                    continue;
                }
                entries.put(type, fromJson(type, entry));
            }
        } catch (Exception e) {
            System.err.println("[ArtifactManifestStore] Failed to load manifest " + manifestFile + ": " + e.getMessage());
        }
    }

    private BuildArtifact fromJson(ArtifactType type, JSONObject entry) {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (entry.has("metadata")) {
            JSONObject md = entry.getJSONObject("metadata");
            for (Object keyObj : md.keySet()) {
                String key = String.valueOf(keyObj);
                metadata.put(key, md.optString(key));
            }
        }
        String pathStr = entry.optString("path", null);
        File path = pathStr != null && !pathStr.isEmpty() ? new File(pathStr) : null;
        return new BuildArtifact(
                type,
                path,
                entry.optString("sourceRevision", null),
                entry.optString("platform", null),
                metadata,
                entry.optString("runId", null),
                entry.optString("buildId", null),
                entry.optLong("size", -1),
                entry.optString("sha256", null),
                entry.optString("buildResult", null),
                entry.optString("testResult", null),
                entry.optString("exportResult", null));
    }

    private synchronized void persist() {
        if (manifestFile == null) {
            return;
        }
        try {
            File parent = manifestFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JSONObject root = new JSONObject();
            root.put("runId", runId);
            for (Map.Entry<ArtifactType, BuildArtifact> e : entries.entrySet()) {
                BuildArtifact a = e.getValue();
                JSONObject entry = new JSONObject();
                entry.put("type", e.getKey().name());
                entry.put("path", a.getPath() != null ? a.getPath().getAbsolutePath() : JSONObject.NULL);
                entry.put("sourceRevision", a.getSourceRevision());
                entry.put("platform", a.getPlatform());
                entry.put("runId", a.getRunId());
                entry.put("buildId", a.getBuildId());
                entry.put("size", a.getSize());
                entry.put("sha256", a.getSha256());
                entry.put("createdAt", a.getCreatedAt());
                entry.put("buildResult", a.getBuildResult());
                entry.put("testResult", a.getTestResult());
                entry.put("exportResult", a.getExportResult());
                entry.put("metadata", new JSONObject(a.getMetadata()));
                root.put(e.getKey().name(), entry);
            }
            Files.writeString(manifestFile.toPath(), root.toString(2), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[ArtifactManifestStore] Failed to persist manifest " + manifestFile + ": " + e.getMessage());
        }
    }

    public static String computeSha256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (file.isDirectory()) {
                try (java.util.stream.Stream<Path> walk = Files.walk(file.toPath())) {
                    walk.filter(Files::isRegularFile)
                        .sorted()
                        .forEach(f -> {
                            try (InputStream in = Files.newInputStream(f)) {
                                byte[] buf = new byte[65536];
                                int n;
                                while ((n = in.read(buf)) > 0) {
                                    digest.update(buf, 0, n);
                                }
                            } catch (IOException ignored) {}
                        });
                }
            } else {
                try (InputStream in = Files.newInputStream(file.toPath())) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        digest.update(buf, 0, n);
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static long directorySize(File dir) {
        try (java.util.stream.Stream<Path> walk = Files.walk(dir.toPath())) {
            return walk.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
        } catch (Exception e) {
            return -1;
        }
    }
}
