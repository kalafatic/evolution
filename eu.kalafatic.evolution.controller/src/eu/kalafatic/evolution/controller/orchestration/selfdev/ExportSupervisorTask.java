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
        BuildArtifact artifact = builder.getArtifact(context);
        if (artifact == null) {
            TaskResult bRes = builder.build(context);
            if (!bRes.isSuccess()) return bRes;
            artifact = builder.getArtifact(context);
        }
        return deployer.deploy(context, artifact);
    }
}
