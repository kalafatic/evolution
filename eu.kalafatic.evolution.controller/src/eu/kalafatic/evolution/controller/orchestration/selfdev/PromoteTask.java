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

            BuildArtifact promotedArtifact = new BuildArtifact(
                    ArtifactType.EVO_RCP,
                    targetInPromoted,
                    context.getSourceRevision(),
                    context.getOs(),
                    candidate.getMetadata()
            );
            context.recordArtifact(promotedArtifact);

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
}
