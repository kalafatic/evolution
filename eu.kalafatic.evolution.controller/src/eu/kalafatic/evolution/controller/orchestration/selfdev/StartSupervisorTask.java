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
            java.io.File fallbackJar = findSupervisorJarFallback(context);
            if (fallbackJar != null && fallbackJar.exists() && fallbackJar.length() > 0) {
                artifact = new BuildArtifact(ArtifactType.SUPERVISOR, fallbackJar, context.getSourceRevision(), null, null);
                context.recordArtifact(artifact);
            }
        }
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure(id, "StartSupervisorTask pre-validation failed: missing required supervisor JAR build artifact in context.", null);
        }
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Supervisor artifact verified: " + artifact.getPath().getAbsolutePath()).build();
    }

    private java.io.File findSupervisorJarFallback(SelfDevContext context) {
        if (context == null) return null;
        java.io.File runtimeSupervisorDir = new java.io.File(context.getRuntimeDirectory(), "supervisor");
        java.io.File jarInRuntime = new java.io.File(runtimeSupervisorDir, "eu.kalafatic.evolution.supervisor.jar");
        if (jarInRuntime.exists() && jarInRuntime.length() > 0) return jarInRuntime;

        if (context.getExportDirectory() != null) {
            java.io.File exportJar = new java.io.File(context.getExportDirectory(), "eu.kalafatic.evolution.supervisor.jar");
            if (exportJar.exists() && exportJar.length() > 0) return exportJar;
        }

        if (context.getProjectRoot() != null) {
            java.io.File targetJar = new java.io.File(context.getProjectRoot(), "eu.kalafatic.evolution.supervisor/target/eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar");
            if (targetJar.exists() && targetJar.length() > 0) return targetJar;
        }

        if (context.getPreparedReactorDirectory() != null) {
            java.io.File reactorJar = new java.io.File(context.getPreparedReactorDirectory(), "eu.kalafatic.evolution.supervisor/target/eu.kalafatic.evolution.supervisor-1.0.0-SNAPSHOT.jar");
            if (reactorJar.exists() && reactorJar.length() > 0) return reactorJar;
        }

        return null;
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
