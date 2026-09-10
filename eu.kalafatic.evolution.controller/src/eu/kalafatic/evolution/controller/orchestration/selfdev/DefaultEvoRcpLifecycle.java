package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class DefaultEvoRcpLifecycle extends AbstractEvoRcpLifecycle {

    public DefaultEvoRcpLifecycle() {
        this(new GitSourceProvider(), new TychoEvoRcpBuilder(), new EvoRcpDeployer(), new EvoRcpRuntime());
    }

    public DefaultEvoRcpLifecycle(SourceProvider sourceProvider,
                                 EvoRcpBuilder builder,
                                 Deployer<BuildArtifact> deployer,
                                 EvoRcpRuntime runtime) {
        super(sourceProvider, builder, deployer, runtime);
    }

    @Override
    public TaskResult fetchSource(SelfDevContext context) {
        if (context == null) return TaskResult.failure("lifecycle_fetch_source", "Context is null", null);
        TaskResult result = sourceProvider.fetchSource(context.getProjectRoot(), context.getSourceDirectory());
        if (result.isSuccess() && result.getDiagnostics().containsKey("revision")) {
            context.setSourceRevision((String) result.getDiagnostics().get("revision"));
        }
        return result;
    }

    @Override
    public TaskResult build(SelfDevContext context) {
        if (context == null) return TaskResult.failure("lifecycle_build", "Context is null", null);
        return builder.build(context);
    }

    @Override
    public TaskResult deploy(SelfDevContext context) {
        if (context == null) return TaskResult.failure("lifecycle_deploy", "Context is null", null);
        BuildArtifact artifact = builder.getArtifact(context);
        if (artifact == null) {
            TaskResult exportRes = builder.exportProduct(context);
            if (!exportRes.isSuccess()) {
                return TaskResult.failure("lifecycle_deploy", "Failed to acquire export artifact for deployment: " + exportRes.getMessage(), exportRes.getError());
            }
            artifact = exportRes.getArtifact();
        }
        return deployer.deploy(context, artifact);
    }

    @Override
    public TaskResult start(SelfDevContext context) {
        if (context == null) return TaskResult.failure("lifecycle_start", "Context is null", null);
        return runtime.start(context);
    }

    @Override
    public TaskResult verifyReady(SelfDevContext context, long timeoutSeconds) {
        if (context == null) return TaskResult.failure("lifecycle_verify", "Context is null", null);
        return runtime.waitUntilReady(context, timeoutSeconds);
    }

    @Override
    public TaskResult stop(SelfDevContext context) {
        if (context == null) return TaskResult.failure("lifecycle_stop", "Context is null", null);
        return runtime.stop(context);
    }

    @Override
    public TaskResult runFullLifecycle(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        System.out.println("[DefaultEvoRcpLifecycle] Starting Full EVO RCP Lifecycle...");

        TaskResult fetchRes = fetchSource(context);
        if (!fetchRes.isSuccess()) return fetchRes;

        TaskResult buildRes = build(context);
        if (!buildRes.isSuccess()) return buildRes;

        TaskResult deployRes = deploy(context);
        if (!deployRes.isSuccess()) return deployRes;

        TaskResult startRes = start(context);
        if (!startRes.isSuccess()) return startRes;

        TaskResult verifyRes = verifyReady(context, 30);
        if (!verifyRes.isSuccess()) return verifyRes;

        TaskResult stopRes = stop(context);
        if (!stopRes.isSuccess()) return stopRes;

        long duration = System.currentTimeMillis() - startTime;
        return new TaskResult.Builder("evo_rcp_full_lifecycle")
                .status(TaskStatus.SUCCESS)
                .message("Full EVO RCP Lifecycle executed successfully (Git -> Build -> Deploy -> Start -> Verify -> Stop).")
                .duration(duration)
                .build();
    }
}
