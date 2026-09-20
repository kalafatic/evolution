package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.log.Log;

public class BuildEvoTask extends AbstractSelfDevTask {
    private final EvoRcpBuilder builder;

    public BuildEvoTask(String id) {
        super(id, "Build EVO RCP (" + id + ")");
        TychoEvoRcpBuilder evoBuilder = new TychoEvoRcpBuilder();
        evoBuilder.setSkipTests(true);
        this.builder = evoBuilder;
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        java.io.File workDir = context.getPreparedReactorDirectory();
        if (workDir == null || !workDir.exists() || !workDir.isDirectory()) {
            return TaskResult.failure(id, "BUILD_EVO pre-validation failed: prepared reactor directory does not exist at " + (workDir != null ? workDir.getAbsolutePath() : "null"), null);
        }
        java.io.File pomFile = new java.io.File(workDir, "pom.xml");
        if (!pomFile.exists()) {
            return TaskResult.failure(id, "BUILD_EVO pre-validation failed: missing pom.xml in prepared reactor at " + workDir.getAbsolutePath(), null);
        }
        java.io.File sourceRepo = context.getRepositoryRoot();
        if (sourceRepo != null && sourceRepo.exists() && workDir.getCanonicalFile().equals(sourceRepo.getCanonicalFile())) {
            return TaskResult.failure(id, "BUILD_EVO pre-validation failed: prepared reactor (" + workDir.getAbsolutePath() + ") points to canonical Git repository root (" + sourceRepo.getAbsolutePath() + "). Direct build on Git root is forbidden.", null);
        }

        eu.kalafatic.evolution.controller.log.Log.log("[SELF-DEV][BUILD]\nWORKDIR = " + workDir.getAbsolutePath());
        System.out.println("[SELF-DEV][BUILD]\nWORKDIR = " + workDir.getAbsolutePath());
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Context valid for EVO build at " + workDir.getAbsolutePath()).workingDirectory(workDir).build();
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
