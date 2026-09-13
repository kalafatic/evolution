package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class BuildSupervisorTask extends AbstractSelfDevTask {
    private final SupervisorBuilder builder;

    public BuildSupervisorTask(String id) {
        super(id, "Build Supervisor Module (" + id + ")");
        this.builder = new MavenSupervisorBuilder();
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Context valid for Supervisor build.").build();
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
        BuildArtifact artifact = context.getArtifact(ArtifactType.SUPERVISOR);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure(id, "Supervisor build post-validation failed: expected JAR artifact missing from context or disk.", null);
        }
        return runResult;
    }
}
