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

        SupervisorClient client = new SupervisorClient(context);
        boolean reachable = client.ping();

        if (!reachable) {
            String msg = "Self-Dev Loop verification failed: Child Supervisor process for run " + context.getRunId() + " is not responsive on HTTP endpoint " + client.getBaseUrl();
            logError(msg);
            return TaskResult.failure(id, msg, null);
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.SUCCESS)
                .message("Self-Dev Loop active and verifying child Supervisor responsiveness on " + client.getBaseUrl())
                .workingDirectory(context.getRuntimeDirectory())
                .build();
    }
}
