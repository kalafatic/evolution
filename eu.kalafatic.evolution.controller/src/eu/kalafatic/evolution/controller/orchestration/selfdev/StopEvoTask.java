package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class StopEvoTask extends AbstractSelfDevTask {
    private final EvoRcpRuntime runtime;

    public StopEvoTask(String id) {
        super(id, "Stop EVO RCP Task (" + id + ")");
        this.runtime = new EvoRcpRuntime();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return runtime.stop(context);
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        if (runtime.isAlive()) {
            return TaskResult.failure(id, "StopEvoTask post-validation failed: EVO RCP process is still alive after stop.", null);
        }
        return runResult;
    }
}
