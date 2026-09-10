package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class BuildEvoTask extends AbstractSelfDevTask {
    private final EvoRcpBuilder builder;

    public BuildEvoTask(String id) {
        super(id, "Build EVO RCP (" + id + ")");
        this.builder = new TychoEvoRcpBuilder();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return builder.build(context);
    }
}
