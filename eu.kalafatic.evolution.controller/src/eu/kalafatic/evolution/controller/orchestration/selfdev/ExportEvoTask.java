package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class ExportEvoTask extends AbstractSelfDevTask {
    private final EvoRcpBuilder builder;

    public ExportEvoTask(String id) {
        super(id, "Export EVO Product (" + id + ")");
        this.builder = new TychoEvoRcpBuilder();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return builder.exportProduct(context);
    }
}
