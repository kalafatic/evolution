package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.log.Log;

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
        log("[MAVEN] Global Maven phase started");
        log("[MAVEN] Source repositories verified by Global Git Check");
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Context valid for Supervisor build.").build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        TaskResult result = builder.build(context);
        if (!result.isSuccess()) {
            log("[MAVEN] ========================================");
            log("[MAVEN][FATAL] Global Maven result: FAILURE");
            log("[MAVEN] Failed component: Supervisor");
            log("[MAVEN] Error category: " + result.getDiagnostic("errorCategory", "UNKNOWN"));
            log("[MAVEN] Root cause: " + result.getMessage());
            log("[MAVEN] Recovery attempts: " + result.getDiagnostic("recoveryAttempts", "0"));
            log("[MAVEN] ========================================");
        }
        return result;
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        BuildArtifact artifact = context.getArtifact(ArtifactType.SUPERVISOR);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists() || artifact.getPath().length() == 0) {
            log("[MAVEN] ========================================");
            log("[MAVEN][FATAL] Global Maven result: FAILURE");
            log("[MAVEN] Failed component: Supervisor");
            log("[MAVEN] Error category: ARTIFACT_MISSING");
            log("[MAVEN] Root cause: Expected Supervisor artifact missing or empty");
            log("[MAVEN] Recovery attempts: 0");
            log("[MAVEN] ========================================");
            return TaskResult.failure(id, "Supervisor build post-validation failed: expected JAR artifact missing or empty.", null);
        }
        return runResult;
    }

    private void log(String msg) {
        Log.log(msg);
        System.out.println(msg);
    }
}
