package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class SupervisorCheckTask extends AbstractSelfDevTask {
    private final SupervisorClient client;

    public SupervisorCheckTask(String id) {
        super(id, "Supervisor HTTP Service Check (" + id + ")");
        this.client = new SupervisorClient();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        boolean alive = client.ping();
        if (alive) {
            return new TaskResult.Builder(id)
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor service is running and responding on HTTP endpoint.")
                    .build();
        } else {
            return TaskResult.failure(id, "Supervisor service is not responding on HTTP endpoint.", null);
        }
    }
}
