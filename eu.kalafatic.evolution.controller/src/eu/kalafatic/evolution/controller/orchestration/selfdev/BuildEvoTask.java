package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class BuildEvoTask extends AbstractSelfDevTask {
    private final EvoRcpBuilder builder;

    public BuildEvoTask(String id) {
        super(id, "Build EVO RCP (" + id + ")");
        this.builder = new TychoEvoRcpBuilder();
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        java.io.File workDir = context.getPreparedReactorDirectory();
        eu.kalafatic.evolution.controller.log.Log.log("[SELF-DEV][BUILD]\nWORKDIR = " + (workDir != null ? workDir.getAbsolutePath() : "null"));
        System.out.println("[SELF-DEV][BUILD]\nWORKDIR = " + (workDir != null ? workDir.getAbsolutePath() : "null"));
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Context valid for EVO build.").build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return builder.build(context);
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure(id, "EVO RCP build post-validation failed: expected artifact missing from context or disk.", null);
        }
        return runResult;
    }
}
