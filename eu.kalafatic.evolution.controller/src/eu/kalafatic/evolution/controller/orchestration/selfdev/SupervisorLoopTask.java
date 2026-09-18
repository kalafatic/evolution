package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class SupervisorLoopTask extends AbstractSelfDevTask {

    public SupervisorLoopTask(String id) {
        super(id, "Supervisor Loop (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        logInfo("Executing Supervisor Loop task ID: " + id);
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        return TaskResult.success(id, "Supervisor Loop task executed successfully.");
    }
}
