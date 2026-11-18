package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public class ExportSupervisorTask extends AbstractSelfDevTask {
    private final SupervisorDeployer deployer;
    private final SupervisorBuilder builder;

    public ExportSupervisorTask(String id) {
        super(id, "Export Supervisor Product (" + id + ")");
        this.deployer = new SupervisorDeployer();
        this.builder = new MavenSupervisorBuilder();
    }

    private BuildArtifact artifact;

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        this.artifact = context.getArtifact(ArtifactType.SUPERVISOR);
        if (this.artifact == null) {
            this.artifact = builder.getArtifact(context);
        }
        if (this.artifact == null || this.artifact.getPath() == null || !this.artifact.getPath().exists()) {
            return TaskResult.failure(id, "ExportSupervisorTask pre-validation failed: no validated supervisor build artifact found in context or disk for export.", null);
        }
        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Supervisor build artifact verified: " + artifact.getPath().getAbsolutePath())
                .workingDirectory(artifact.getPath().getParentFile())
                .build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        // WP4 (D-14): EXPORT_SUPERVISOR produces a real export record: the
        // supervisor jar is materialized into the export directory and recorded
        // in the per-run artifact manifest with a checksum.
        TaskResult deployRes = deployer.deploy(context, artifact);
        if (!deployRes.isSuccess()) {
            return deployRes;
        }

        File exportDir = context.getExportDirectory();
        if (exportDir == null) {
            return TaskResult.failure(id, "ExportSupervisorTask failed: export directory is null.", null);
        }
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File exportJar = new File(exportDir, "eu.kalafatic.evolution.supervisor.jar");
        java.nio.file.Files.copy(artifact.getPath().toPath(), exportJar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        ArtifactManifestStore store = context.getArtifactManifestStore();
        BuildArtifact prior = null;
        try {
            prior = store.resolve(ArtifactType.SUPERVISOR);
        } catch (Exception ignored) {
            prior = null;
        }
        String buildId = prior != null && prior.getBuildId() != null ? prior.getBuildId() : store.nextBuildId();
        BuildArtifact exported = store.record(ArtifactType.SUPERVISOR, exportJar,
                context.getSourceRevision(), buildId,
                prior != null ? prior.getBuildResult() : null,
                prior != null ? prior.getTestResult() : null,
                "SUCCESS", java.util.Collections.emptyMap());
        context.recordArtifact(exported);

        return new TaskResult.Builder(id)
                .status(TaskStatus.SUCCESS)
                .message("Supervisor exported and recorded: " + exportJar.getAbsolutePath())
                .artifact(exported)
                .workingDirectory(exportDir)
                .build();
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        File exportDir = context.getExportDirectory();
        if (exportDir == null || !exportDir.exists()) {
            return TaskResult.failure(id, "ExportSupervisorTask post-validation failed: export directory does not exist.", null);
        }
        File exportJar = new File(exportDir, "eu.kalafatic.evolution.supervisor.jar");
        if (!exportJar.exists() || exportJar.length() == 0) {
            return TaskResult.failure(id, "ExportSupervisorTask post-validation failed: exported supervisor jar missing or empty at " + exportJar.getAbsolutePath(), null);
        }
        return runResult;
    }
}
