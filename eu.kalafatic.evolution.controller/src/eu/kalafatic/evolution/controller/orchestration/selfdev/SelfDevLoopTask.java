package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class SelfDevLoopTask extends AbstractSelfDevTask {

    static final int PING_ATTEMPTS = 5;
    static final long PING_INTERVAL_MS = 2000;

    public SelfDevLoopTask(String id) {
        super(id, "Self-Dev Health Verification (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        logInfo("Executing Self-Dev health verification task ID: " + id);
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null", null);
        }

        SupervisorClient client = new SupervisorClient(context);
        boolean reachable = false;
        int attempts = 0;
        for (int i = 0; i < PING_ATTEMPTS; i++) {
            checkCancelled();
            attempts++;
            if (client.ping()) {
                reachable = true;
                break;
            }
            Thread.sleep(PING_INTERVAL_MS);
        }

        if (!reachable) {
            String msg = "Self-Dev health verification failed after " + attempts + " attempts: child Supervisor process for run "
                    + context.getRunId() + " is not responsive on HTTP endpoint " + client.getBaseUrl()
                    + ". (This task performs a bounded health ping; the autonomous iteration loop is managed by the Darwin engine.)";
            logError(msg);
            return TaskResult.failure(id, msg, null);
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.SUCCESS)
                .message("Self-Dev health verified (" + attempts + " attempt(s)) on " + client.getBaseUrl()
                        + ". (Bounded health ping only; the autonomous iteration loop is managed by the Darwin engine.)")
                .workingDirectory(context.getRuntimeDirectory())
                .build();
    }
}
