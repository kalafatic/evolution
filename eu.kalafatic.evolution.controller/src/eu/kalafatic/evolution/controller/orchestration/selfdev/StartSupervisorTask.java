package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class StartSupervisorTask extends AbstractSelfDevTask {
    private final SupervisorRuntime runtime;

    public StartSupervisorTask(String id) {
        super(id, "Start Supervisor Task (" + id + ")");
        this.runtime = new SupervisorRuntime();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return runtime.start(context);
    }
}
