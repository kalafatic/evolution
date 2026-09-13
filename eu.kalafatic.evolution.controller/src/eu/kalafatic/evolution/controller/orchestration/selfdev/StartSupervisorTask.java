package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class StartSupervisorTask extends AbstractSelfDevTask {
    private final SupervisorRuntime runtime;

    public StartSupervisorTask(String id) {
        super(id, "Start Supervisor Task (" + id + ")");
        this.runtime = new SupervisorRuntime();
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        BuildArtifact artifact = context.getArtifact(ArtifactType.SUPERVISOR);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure(id, "StartSupervisorTask pre-validation failed: missing required supervisor JAR build artifact in context.", null);
        }
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Supervisor artifact verified: " + artifact.getPath().getAbsolutePath()).build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return runtime.start(context);
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        if (!runtime.isAlive()) {
            return TaskResult.failure(id, "StartSupervisorTask post-validation failed: process started but is not alive.", null);
        }
        return runResult;
    }
}
