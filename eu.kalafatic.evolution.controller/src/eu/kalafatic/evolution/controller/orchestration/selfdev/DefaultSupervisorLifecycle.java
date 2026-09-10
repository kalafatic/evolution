package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class DefaultSupervisorLifecycle extends AbstractSupervisorLifecycle {

    public DefaultSupervisorLifecycle() {
        this(new MavenSupervisorBuilder(), new SupervisorDeployer(), new SupervisorRuntime());
    }

    public DefaultSupervisorLifecycle(SupervisorBuilder builder,
                                     Deployer<BuildArtifact> deployer,
                                     SupervisorRuntime runtime) {
        super(builder, deployer, runtime);
    }

    @Override
    public TaskResult build(SelfDevContext context) {
        if (context == null) return TaskResult.failure("supervisor_lifecycle_build", "Context is null", null);
        return builder.build(context);
    }

    @Override
    public TaskResult deploy(SelfDevContext context) {
        if (context == null) return TaskResult.failure("supervisor_lifecycle_deploy", "Context is null", null);
        BuildArtifact artifact = builder.getArtifact(context);
        if (artifact == null) {
            TaskResult bRes = build(context);
            if (!bRes.isSuccess()) {
                return TaskResult.failure("supervisor_lifecycle_deploy", "Build failed during deploy: " + bRes.getMessage(), bRes.getError());
            }
            artifact = builder.getArtifact(context);
        }
        return deployer.deploy(context, artifact);
    }

    @Override
    public TaskResult start(SelfDevContext context) {
        if (context == null) return TaskResult.failure("supervisor_lifecycle_start", "Context is null", null);
        return runtime.start(context);
    }

    @Override
    public TaskResult verifyReady(SelfDevContext context, long timeoutSeconds) {
        if (context == null) return TaskResult.failure("supervisor_lifecycle_verify", "Context is null", null);
        return runtime.waitUntilReady(context, timeoutSeconds);
    }

    @Override
    public TaskResult sendCommand(String command, String param) {
        return runtime.getClient().sendCommand(command, param);
    }

    @Override
    public TaskResult stop(SelfDevContext context) {
        if (context == null) return TaskResult.failure("supervisor_lifecycle_stop", "Context is null", null);
        return runtime.stop(context);
    }

    @Override
    public TaskResult runFullLifecycle(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        System.out.println("[DefaultSupervisorLifecycle] Starting Full Supervisor Lifecycle...");

        TaskResult buildRes = build(context);
        if (!buildRes.isSuccess()) return buildRes;

        TaskResult deployRes = deploy(context);
        if (!deployRes.isSuccess()) return deployRes;

        TaskResult startRes = start(context);
        if (!startRes.isSuccess()) return startRes;

        TaskResult pingRes = sendCommand("ping", null);
        if (!pingRes.isSuccess()) return pingRes;

        long duration = System.currentTimeMillis() - startTime;
        return new TaskResult.Builder("supervisor_full_lifecycle")
                .status(TaskStatus.SUCCESS)
                .message("Full Supervisor Lifecycle executed successfully.")
                .duration(duration)
                .build();
    }
}
