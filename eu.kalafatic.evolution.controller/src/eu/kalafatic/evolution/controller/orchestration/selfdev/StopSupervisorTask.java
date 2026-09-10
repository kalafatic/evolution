package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class StopSupervisorTask extends AbstractSelfDevTask {
    private final SupervisorRuntime runtime;

    public StopSupervisorTask(String id) {
        super(id, "Stop Supervisor Task (" + id + ")");
        this.runtime = new SupervisorRuntime();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return runtime.stop(context);
    }
}
