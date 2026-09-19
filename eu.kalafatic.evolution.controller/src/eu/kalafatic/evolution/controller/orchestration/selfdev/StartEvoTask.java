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
            java.io.File fallbackProduct = findEvoRcpProductFallback(context);
            if (fallbackProduct != null && fallbackProduct.exists()) {
                artifact = new BuildArtifact(ArtifactType.EVO_RCP, fallbackProduct, context.getSourceRevision(), fallbackProduct.getParentFile(), null);
                context.recordArtifact(artifact);
            }
        }
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

    private java.io.File findEvoRcpProductFallback(SelfDevContext context) {
        if (context == null) return null;

        java.io.File evoRuntimeDir = new java.io.File(context.getRuntimeDirectory(), "evo");
        if (evoRuntimeDir.exists() && evoRuntimeDir.isDirectory()) {
            java.io.File[] files = evoRuntimeDir.listFiles();
            if (files != null && files.length > 0) return evoRuntimeDir;
        }

        if (context.getExportDirectory() != null && context.getExportDirectory().exists()) {
            java.io.File[] zips = context.getExportDirectory().listFiles((dir, name) -> name.endsWith(".zip") || name.startsWith("evolution"));
            if (zips != null && zips.length > 0) return zips[0];
        }

        java.io.File repoTarget = new java.io.File(context.getProjectRoot(), "eu.kalafatic.evolution.repository/target");
        if (repoTarget.exists() && repoTarget.isDirectory()) return repoTarget;

        java.io.File reactorTarget = new java.io.File(context.getPreparedReactorDirectory(), "eu.kalafatic.evolution.repository/target");
        if (reactorTarget.exists() && reactorTarget.isDirectory()) return reactorTarget;

        return null;
    }
}
