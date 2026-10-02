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
                artifact = new BuildArtifact(ArtifactType.EVO_RCP, fallbackProduct, context.getSourceRevision(), null, null);
                context.recordArtifact(artifact);
            }
        }
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure(id, "StartEvoTask pre-validation failed: missing required EVO RCP build artifact in context.", null);
        }
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("EVO RCP artifact verified: " + artifact.getPath().getAbsolutePath()).workingDirectory(runtimeDir).build();
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

        String productId = "evolution";
        if (context.getResourceManager() != null && context.getResourceManager().getProductDefinition() != null) {
            productId = context.getResourceManager().getProductDefinition().getProductId().toLowerCase();
        }

        final String reqProductId = productId;

        if (context.getExportDirectory() != null && context.getExportDirectory().exists()) {
            java.io.File[] archives = context.getExportDirectory().listFiles((dir, name) -> {
                String lname = name.toLowerCase();
                boolean isArchive = lname.endsWith(".zip") || lname.endsWith(".tar.gz") || lname.endsWith(".tgz");
                boolean matchesProduct = lname.startsWith(reqProductId + "-") || lname.startsWith(reqProductId + "_") || lname.equals(reqProductId + ".zip") || lname.equals(reqProductId + ".tar.gz");
                return isArchive && matchesProduct;
            });
            if (archives != null && archives.length > 0) return archives[0];
        }

        java.io.File repoTarget = new java.io.File(context.getProjectRoot(), "eu.kalafatic.evolution.repository/target");
        if (repoTarget.exists() && repoTarget.isDirectory()) return repoTarget;

        java.io.File reactorTarget = new java.io.File(context.getPreparedReactorDirectory(), "eu.kalafatic.evolution.repository/target");
        if (reactorTarget.exists() && reactorTarget.isDirectory()) return reactorTarget;

        return null;
    }
}
