package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class SupervisorCheckTask extends AbstractSelfDevTask {

    public SupervisorCheckTask(String id) {
        super(id, "Supervisor HTTP Service Check (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        SupervisorClient client = new SupervisorClient(context);
        boolean alive = client.ping();
        if (alive) {
            return new TaskResult.Builder(id)
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor service is running and responding on HTTP endpoint (" + client.getBaseUrl() + ").")
                    .build();
        } else {
            return TaskResult.failure(id, "Supervisor service is not responding on HTTP endpoint (" + client.getBaseUrl() + ").", null);
        }
    }
}
