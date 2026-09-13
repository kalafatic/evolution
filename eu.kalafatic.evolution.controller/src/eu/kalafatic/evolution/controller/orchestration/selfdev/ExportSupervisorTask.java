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
        return deployer.deploy(context, artifact);
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
        return runResult;
    }
}
