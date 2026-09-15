package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class ExportEvoTask extends AbstractSelfDevTask {
    private final EvoRcpBuilder builder;

    public ExportEvoTask(String id) {
        super(id, "Export EVO Product (" + id + ")");
        this.builder = new TychoEvoRcpBuilder();
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        java.io.File exportDir = context.getExportDirectory();
        eu.kalafatic.evolution.controller.log.Log.log("[SELF-DEV][EXPORT]\nOUTPUT = " + (exportDir != null ? exportDir.getAbsolutePath() : "null"));
        System.out.println("[SELF-DEV][EXPORT]\nOUTPUT = " + (exportDir != null ? exportDir.getAbsolutePath() : "null"));
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Context valid for EVO export.").build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return builder.exportProduct(context);
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure(id, "Export EVO post-validation failed: exported product artifact missing from context or disk.", null);
        }
        return runResult;
    }
}
