package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SupervisorDeployer extends AbstractDeployer<BuildArtifact> {

    public SupervisorDeployer() {
        super("supervisor_deployer");
    }

    @Override
    public TaskResult deploy(SelfDevContext context, BuildArtifact artifact) {
        long startTime = System.currentTimeMillis();
        if (context == null) {
            return TaskResult.failure("deploy_supervisor", "SelfDevContext is null", null);
        }
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists()) {
            return TaskResult.failure("deploy_supervisor", "Invalid or non-existent Supervisor artifact", null);
        }

        File targetRuntimeDir = context.getRuntimeDirectory();
        if (targetRuntimeDir == null) {
            return TaskResult.failure("deploy_supervisor", "Target runtime directory in context is null", null);
        }

        File supervisorSubDir = new File(targetRuntimeDir, "supervisor");
        try {
            deleteRecursively(supervisorSubDir);
            supervisorSubDir.mkdirs();

            File sourceJar = artifact.getPath();
            File destJar = new File(supervisorSubDir, "eu.kalafatic.evolution.supervisor.jar");
            Files.copy(sourceJar.toPath(), destJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

            TaskResult valRes = validateDeployment(context, supervisorSubDir);
            if (!valRes.isSuccess()) {
                return valRes;
            }

            long duration = System.currentTimeMillis() - startTime;
            BuildArtifact deployedArtifact = new BuildArtifact(ArtifactType.SUPERVISOR, destJar, artifact.getSourceRevision(), artifact.getPlatform(), artifact.getMetadata());

            return new TaskResult.Builder("deploy_supervisor")
                    .status(TaskStatus.SUCCESS)
                    .message("Supervisor JAR deployed to " + destJar.getAbsolutePath())
                    .duration(duration)
                    .artifact(deployedArtifact)
                    .workingDirectory(supervisorSubDir)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return TaskResult.failure("deploy_supervisor", "Deployment exception: " + e.getMessage(), e);
        }
    }

    @Override
    public TaskResult validateDeployment(SelfDevContext context, File deployedLocation) {
        if (deployedLocation == null || !deployedLocation.exists()) {
            return TaskResult.failure("deploy_supervisor_validation", "Supervisor deployment location does not exist: " + (deployedLocation != null ? deployedLocation.getAbsolutePath() : "null"), null);
        }

        File jarFile = new File(deployedLocation, "eu.kalafatic.evolution.supervisor.jar");
        if (!jarFile.exists() || jarFile.length() == 0) {
            File[] jars = deployedLocation.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars == null || jars.length == 0) {
                return TaskResult.failure("deploy_supervisor_validation", "Validation failed: Supervisor JAR not found in " + deployedLocation.getAbsolutePath(), null);
            }
        }

        return new TaskResult.Builder("deploy_supervisor_validation")
                .status(TaskStatus.SUCCESS)
                .message("Supervisor deployment validated.")
                .workingDirectory(deployedLocation)
                .build();
    }
}
