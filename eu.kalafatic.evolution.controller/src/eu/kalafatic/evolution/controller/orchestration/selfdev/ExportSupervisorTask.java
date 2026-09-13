package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class ExportSupervisorTask extends AbstractSelfDevTask {
    private final SupervisorDeployer deployer;
    private final SupervisorBuilder builder;

    public ExportSupervisorTask(String id) {
        super(id, "Export Supervisor Product (" + id + ")");
        this.deployer = new SupervisorDeployer();
        this.builder = new MavenSupervisorBuilder();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        BuildArtifact artifact = context.getArtifact(ArtifactType.SUPERVISOR);
        if (artifact == null) {
            artifact = builder.getArtifact(context);
        }
        if (artifact == null) {
            return TaskResult.failure(id, "No validated supervisor build artifact found in context for export.", null);
        }
        return deployer.deploy(context, artifact);
    }
}
