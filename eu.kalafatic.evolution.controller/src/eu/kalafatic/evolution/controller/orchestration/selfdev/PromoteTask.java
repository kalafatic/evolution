package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import eu.kalafatic.evolution.controller.log.Log;

public class PromoteTask extends AbstractSelfDevTask {

    public PromoteTask(String id) {
        super(id, "Promote Candidate Artifact (" + id + ")");
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "PromoteTask pre-validation failed: SelfDevContext is null", null);
        }
        BuildArtifact candidate = context.getArtifact(ArtifactType.EVO_RCP);
        if (candidate == null || candidate.getPath() == null || !candidate.getPath().exists()) {
            return TaskResult.failure(id, "PromoteTask pre-validation failed: Candidate EVO RCP artifact missing", null);
        }

        // Verify that critical previous verification and inference stages succeeded
        TaskResult startRes = context.getTaskResult("START_EVO");
        if (startRes == null || !startRes.isSuccess()) {
            return TaskResult.failure(id, "PromoteTask pre-validation failed: Cannot promote because START_EVO did not succeed", null);
        }

        TaskResult inferRes = context.getTaskResult("CREATE_INFERENCE_TASK");
        if (inferRes != null && !inferRes.isSuccess()) {
            return TaskResult.failure(id, "PromoteTask pre-validation failed: Cannot promote because CREATE_INFERENCE_TASK failed", null);
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Candidate artifact ready for promotion: " + candidate.getPath().getAbsolutePath())
                .workingDirectory(candidate.getPath())
                .build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        BuildArtifact candidate = context.getArtifact(ArtifactType.EVO_RCP);
        File candidateFile = candidate.getPath();

        // Promotion targets: mark and archive candidate into promoted directory in run and export
        File promotedDir = new File(context.getExportDirectory(), "promoted");
        if (!promotedDir.exists()) {
            promotedDir.mkdirs();
        }

        File targetInPromoted = new File(promotedDir, candidateFile.getName());
        try {
            if (candidateFile.isDirectory()) {
                copyDirectory(candidateFile, targetInPromoted);
            } else {
                Files.copy(candidateFile.toPath(), targetInPromoted.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // WP4 (D-12/D-43): promotion preserves artifact identity (build ID,
            // revision, metadata) and re-verifies the copy via checksum.
            ArtifactManifestStore store = context.getArtifactManifestStore();
            BuildArtifact promotedArtifact = store.rehome(candidate, targetInPromoted, "SUCCESS");
            if (promotedArtifact == null || !store.verifyIntegrity(promotedArtifact)) {
                return TaskResult.failure(id, "Promotion failed: promoted copy integrity verification failed at " + targetInPromoted.getAbsolutePath(), null);
            }
            context.recordArtifact(promotedArtifact);

            // WP10 (D-43): deterministic known-good index for rollback.
            appendPromotedIndex(promotedDir, promotedArtifact);

            Log.log("[SELF-DEV][PROMOTE] Candidate artifact successfully promoted to: " + targetInPromoted.getAbsolutePath());
            System.out.println("[SELF-DEV][PROMOTE] Candidate artifact successfully promoted to: " + targetInPromoted.getAbsolutePath());

            long duration = System.currentTimeMillis() - startTime;
            return new TaskResult.Builder(id)
                    .status(TaskStatus.SUCCESS)
                    .message("Candidate artifact successfully promoted to: " + targetInPromoted.getAbsolutePath())
                    .artifact(promotedArtifact)
                    .duration(duration)
                    .workingDirectory(targetInPromoted)
                    .diagnostic("promotedLocation", targetInPromoted.getAbsolutePath())
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure(id, "Failed to promote candidate artifact: " + e.getMessage(), e);
        }
    }

    private void copyDirectory(File src, File dest) throws IOException {
        Path srcPath = src.toPath();
        Path destPath = dest.toPath();
        Files.walk(srcPath).forEach(source -> {
            try {
                Path destination = destPath.resolve(srcPath.relativize(source));
                if (Files.isDirectory(source)) {
                    if (!Files.exists(destination)) Files.createDirectories(destination);
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ------------------------------------------------------------------
    // WP10 (D-43): deterministic promoted-artifact index for rollback.
    // ------------------------------------------------------------------

    public static final String PROMOTED_INDEX_FILE = "promoted-index.json";

    public static File promotedIndexFile(File promotedDir) {
        return new File(promotedDir, PROMOTED_INDEX_FILE);
    }

    public static synchronized void appendPromotedIndex(File promotedDir, BuildArtifact artifact) {
        try {
            org.json.JSONObject root = readPromotedIndexJson(promotedDir);
            org.json.JSONArray entries = root.optJSONArray("entries");
            if (entries == null) {
                entries = new org.json.JSONArray();
                root.put("entries", entries);
            }
            org.json.JSONObject entry = new org.json.JSONObject();
            entry.put("path", artifact.getPath().getAbsolutePath());
            entry.put("buildId", artifact.getBuildId());
            entry.put("sha256", artifact.getSha256());
            entry.put("revision", artifact.getSourceRevision());
            entry.put("promotedAt", System.currentTimeMillis());
            entries.put(entry);
            Files.writeString(promotedIndexFile(promotedDir).toPath(), root.toString(2), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[PromoteTask] Failed to persist promoted index: " + e.getMessage());
        }
    }

    private static org.json.JSONObject readPromotedIndexJson(File promotedDir) throws Exception {
        File indexFile = promotedIndexFile(promotedDir);
        if (indexFile.exists()) {
            String content = Files.readString(indexFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            if (content != null && !content.trim().isEmpty()) {
                return new org.json.JSONObject(content);
            }
        }
        return new org.json.JSONObject();
    }

    /**
     * Deterministically selects the newest promoted known-good artifact whose
     * file still exists and whose checksum verifies. Never a first-match pick.
     */
    public static BuildArtifact findNewestVerifiedPromoted(File promotedDir) {
        if (promotedDir == null || !promotedDir.exists()) {
            return null;
        }
        try {
            org.json.JSONObject root = readPromotedIndexJson(promotedDir);
            org.json.JSONArray entries = root.optJSONArray("entries");
            if (entries != null) {
                for (int i = entries.length() - 1; i >= 0; i--) {
                    org.json.JSONObject entry = entries.getJSONObject(i);
                    File path = new File(entry.optString("path", ""));
                    String sha = entry.optString("sha256", null);
                    if (path.exists()) {
                        if (sha == null || sha.isEmpty() || sha.equalsIgnoreCase(ArtifactManifestStore.computeSha256(path))) {
                            return new BuildArtifact(ArtifactType.EVO_RCP, path, entry.optString("revision", null), null, null,
                                    null, entry.optString("buildId", null), path.length(), sha,
                                    "SUCCESS", "PROMOTED_KNOWN_GOOD", "SUCCESS");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PromoteTask] Failed to read promoted index: " + e.getMessage());
        }
        // Fallback without an index: newest file by modification time (still
        // deterministic - never an arbitrary first entry).
        File[] files = promotedDir.listFiles(f -> !f.getName().equals(PROMOTED_INDEX_FILE));
        if (files != null && files.length > 0) {
            java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            return new BuildArtifact(ArtifactType.EVO_RCP, files[0], null, null, null);
        }
        return null;
    }
}
