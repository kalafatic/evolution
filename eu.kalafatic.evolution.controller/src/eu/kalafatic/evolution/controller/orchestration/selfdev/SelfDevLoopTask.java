package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class SelfDevLoopTask extends AbstractSelfDevTask {

    public SelfDevLoopTask(String id) {
        super(id, "Self-Dev Loop (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        logInfo("Executing Self-Dev Loop task ID: " + id);
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }
        return TaskResult.success(id, "Self-Dev Loop task executed successfully.");
    }
}
