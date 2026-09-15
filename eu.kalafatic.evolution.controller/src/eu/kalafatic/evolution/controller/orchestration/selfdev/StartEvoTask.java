package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class StartEvoTask extends AbstractSelfDevTask {
    private final EvoRcpRuntime runtime;

    public StartEvoTask(String id) {
        super(id, "Start EVO RCP Task (" + id + ")");
        this.runtime = new EvoRcpRuntime();
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        java.io.File runtimeDir = context.getRuntimeDirectory();
        eu.kalafatic.evolution.controller.log.Log.log("[SELF-DEV][START]\nRUNTIME = " + (runtimeDir != null ? runtimeDir.getAbsolutePath() : "null"));
        System.out.println("[SELF-DEV][START]\nRUNTIME = " + (runtimeDir != null ? runtimeDir.getAbsolutePath() : "null"));

        BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure(id, "StartEvoTask pre-validation failed: missing required EVO RCP build artifact in context.", null);
        }
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("EVO RCP artifact verified: " + artifact.getPath().getAbsolutePath()).build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        TaskResult startRes = runtime.start(context);
        if (!startRes.isSuccess()) return startRes;
        return runtime.waitUntilReady(context, 30);
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        if (!runtime.isAlive()) {
            return TaskResult.failure(id, "StartEvoTask post-validation failed: EVO process started but is not alive.", null);
        }
        return runResult;
    }
}
