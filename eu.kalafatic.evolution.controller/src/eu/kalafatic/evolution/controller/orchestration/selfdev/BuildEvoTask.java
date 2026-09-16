package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.log.Log;

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
        TaskResult result = builder.build(context);
        if (!result.isSuccess()) {
            log("[MAVEN] ========================================");
            log("[MAVEN][FATAL] Global Maven result: FAILURE");
            log("[MAVEN] Failed component: Evolution");
            log("[MAVEN] Error category: " + result.getDiagnostic("errorCategory", "UNKNOWN"));
            log("[MAVEN] Root cause: " + result.getMessage());
            log("[MAVEN] Recovery attempts: " + result.getDiagnostic("recoveryAttempts", "0"));
            log("[MAVEN] ========================================");
        } else {
            log("[MAVEN] ========================================");
            log("[MAVEN] Global Maven result: SUCCESS");
            log("[MAVEN] Supervisor: SUCCESS");
            log("[MAVEN] Evolution: SUCCESS");
            log("[MAVEN] Artifacts: VALID");
            log("[MAVEN] ========================================");
        }
        return result;
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists() || artifact.getPath().length() == 0) {
            log("[MAVEN] ========================================");
            log("[MAVEN][FATAL] Global Maven result: FAILURE");
            log("[MAVEN] Failed component: Evolution");
            log("[MAVEN] Error category: ARTIFACT_MISSING");
            log("[MAVEN] Root cause: Expected EVO RCP artifact missing or empty");
            log("[MAVEN] Recovery attempts: 0");
            log("[MAVEN] ========================================");
            return TaskResult.failure(id, "EVO RCP build post-validation failed: expected artifact missing or empty.", null);
        }
        return runResult;
    }

    private void log(String msg) {
        Log.log(msg);
        System.out.println(msg);
    }
}
