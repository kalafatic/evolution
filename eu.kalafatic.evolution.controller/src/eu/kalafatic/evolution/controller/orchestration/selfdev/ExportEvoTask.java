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
        java.io.File workDir = context.getPreparedReactorDirectory();
        if (workDir == null || !workDir.exists() || !workDir.isDirectory()) {
            return TaskResult.failure(id, "EXPORT_EVO pre-validation failed: prepared reactor directory does not exist at " + (workDir != null ? workDir.getAbsolutePath() : "null"), null);
        }
        java.io.File pomFile = new java.io.File(workDir, "pom.xml");
        if (!pomFile.exists()) {
            return TaskResult.failure(id, "EXPORT_EVO pre-validation failed: missing pom.xml in prepared reactor at " + workDir.getAbsolutePath(), null);
        }
        java.io.File exportDir = context.getExportDirectory();
        if (exportDir == null) {
            return TaskResult.failure(id, "EXPORT_EVO pre-validation failed: export directory in context is null", null);
        }
        eu.kalafatic.evolution.controller.log.Log.log("[SELF-DEV][EXPORT]\nOUTPUT = " + exportDir.getAbsolutePath());
        System.out.println("[SELF-DEV][EXPORT]\nOUTPUT = " + exportDir.getAbsolutePath());
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Context valid for EVO export at " + exportDir.getAbsolutePath()).workingDirectory(exportDir).build();
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
