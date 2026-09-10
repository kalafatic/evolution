package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class StartEvoTask extends AbstractSelfDevTask {
    private final EvoRcpRuntime runtime;

    public StartEvoTask(String id) {
        super(id, "Start EVO RCP Task (" + id + ")");
        this.runtime = new EvoRcpRuntime();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        TaskResult startRes = runtime.start(context);
        if (!startRes.isSuccess()) return startRes;
        return runtime.waitUntilReady(context, 30);
    }
}
