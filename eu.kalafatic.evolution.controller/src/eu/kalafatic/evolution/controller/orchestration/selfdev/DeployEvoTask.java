package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import eu.kalafatic.evolution.controller.log.Log;

public class DeployEvoTask extends AbstractSelfDevTask {
    private final EvoRcpDeployer deployer;

    public DeployEvoTask(String id) {
        super(id, "Deploy EVO RCP Product (" + id + ")");
        this.deployer = new EvoRcpDeployer();
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (context == null) {
            return TaskResult.failure(id, "DeployEvoTask pre-validation failed: SelfDevContext is null", null);
        }
        BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure(id, "DeployEvoTask pre-validation failed: required EVO RCP artifact missing from context or disk.", null);
        }
        File runtimeDir = context.getRuntimeDirectory();
        if (runtimeDir == null) {
            return TaskResult.failure(id, "DeployEvoTask pre-validation failed: runtime directory is null", null);
        }

        Log.log("[SELF-DEV][DEPLOY]\nARTIFACT = " + artifact.getPath().getAbsolutePath() + "\nRUNTIME = " + runtimeDir.getAbsolutePath());
        System.out.println("[SELF-DEV][DEPLOY]\nARTIFACT = " + artifact.getPath().getAbsolutePath() + "\nRUNTIME = " + runtimeDir.getAbsolutePath());

        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Context valid for EVO RCP deployment at " + runtimeDir.getAbsolutePath())
                .workingDirectory(runtimeDir)
                .build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        BuildArtifact artifact = context.getArtifact(ArtifactType.EVO_RCP);
        return deployer.deploy(context, artifact);
    }

    @Override
    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        if (!runResult.isSuccess()) {
            return runResult;
        }
        File evoRuntimeDir = new File(context.getRuntimeDirectory(), "evo");
        return deployer.validateDeployment(context, evoRuntimeDir);
    }
}
