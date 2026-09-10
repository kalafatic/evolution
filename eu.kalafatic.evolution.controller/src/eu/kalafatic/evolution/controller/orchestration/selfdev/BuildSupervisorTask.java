package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class BuildSupervisorTask extends AbstractSelfDevTask {
    private final SupervisorBuilder builder;

    public BuildSupervisorTask(String id) {
        super(id, "Build Supervisor Module (" + id + ")");
        this.builder = new MavenSupervisorBuilder();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return builder.build(context);
    }
}
