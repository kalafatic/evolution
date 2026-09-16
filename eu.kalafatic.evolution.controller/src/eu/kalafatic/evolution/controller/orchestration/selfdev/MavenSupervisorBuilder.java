package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import eu.kalafatic.evolution.controller.log.Log;

public class MavenSupervisorBuilder extends AbstractProjectBuilder implements SupervisorBuilder {

    public MavenSupervisorBuilder() {
        super("build_supervisor");
    }

    @Override
    public TaskResult build(SelfDevContext context) {
        if (context == null) {
            return TaskResult.failure("build_supervisor", "SelfDevContext is null", null);
        }

        File supervisorModuleDir = context.getSupervisorDirectory();

        if (supervisorModuleDir == null || !supervisorModuleDir.exists()) {
            return TaskResult.failure("build_supervisor", "Supervisor module directory does not exist: " + (supervisorModuleDir != null ? supervisorModuleDir.getAbsolutePath() : "null"), null);
        }

        File logFile = getLogFile(context, "supervisor_build.log");
        List<String> goals = Arrays.asList("validate", "clean", "package");
        List<String> args = Arrays.asList("-DskipTests");

        log("[MAVEN] Building Supervisor module at " + supervisorModuleDir.getAbsolutePath());
        TaskResult buildResult = mavenExecutor.executeBuild(supervisorModuleDir, goals, args, logFile, 15);
        if (!buildResult.isSuccess()) {
            return buildResult;
        }

        BuildArtifact artifact = getArtifact(context);
        if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists() || artifact.getPath().length() == 0) {
            log("[MAVEN][ARTIFACT]");
            log("[MAVEN][ARTIFACT] Type: " + ArtifactType.SUPERVISOR);
            log("[MAVEN][ARTIFACT] Path: " + (artifact != null && artifact.getPath() != null ? artifact.getPath().getAbsolutePath() : "null"));
            log("[MAVEN][ARTIFACT] Exists: false");
            log("[MAVEN][ARTIFACT] Validation: FAILED");

            return TaskResult.failure("build_supervisor", "Maven returned exit code 0 but supervisor JAR artifact was missing, empty, or unverified at " + (supervisorModuleDir != null ? supervisorModuleDir.getAbsolutePath() : "null"), null);
        }

        log("[MAVEN][ARTIFACT]");
        log("[MAVEN][ARTIFACT] Type: " + ArtifactType.SUPERVISOR);
        log("[MAVEN][ARTIFACT] Path: " + artifact.getPath().getAbsolutePath());
        log("[MAVEN][ARTIFACT] Exists: true");
        log("[MAVEN][ARTIFACT] Size: " + artifact.getPath().length() + " bytes");
        log("[MAVEN][ARTIFACT] Validation: SUCCESS");

        context.recordArtifact(artifact);

        return new TaskResult.Builder("build_supervisor")
                .status(TaskStatus.SUCCESS)
                .message("Supervisor build completed and artifact verified: " + artifact.getPath().getAbsolutePath())
                .duration(buildResult.getDuration())
                .artifact(artifact)
                .logFile(logFile)
                .build();
    }

    @Override
    public BuildArtifact getArtifact(SelfDevContext context) {
        if (context == null) return null;

        File supervisorModuleDir = context.getSupervisorDirectory();
        File buildDir = context.getBuildDirectory();
        if (buildDir != null && buildDir.exists()) {
            File wsTargetDir = new File(buildDir, (supervisorModuleDir != null ? supervisorModuleDir.getName() : "eu.kalafatic.evolution.supervisor") + "/target");
            if (wsTargetDir.exists()) {
                File[] jars = wsTargetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.endsWith("-sources.jar"));
                if (jars != null && jars.length > 0) {
                    return new BuildArtifact(ArtifactType.SUPERVISOR, jars[0], context.getSourceRevision(), null, null);
                }
            }
        }

        File targetDir = supervisorModuleDir != null ? new File(supervisorModuleDir, "target") : null;
        if (targetDir != null && targetDir.exists()) {
            File[] jars = targetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.endsWith("-sources.jar"));
            if (jars != null && jars.length > 0) {
                return new BuildArtifact(ArtifactType.SUPERVISOR, jars[0], context.getSourceRevision(), null, null);
            }
        }

        File exportDir = context.getExportDirectory();
        if (exportDir != null && exportDir.exists()) {
            File[] jars = exportDir.listFiles((dir, name) -> name.contains("supervisor") && name.endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                return new BuildArtifact(ArtifactType.SUPERVISOR, jars[0], context.getSourceRevision(), null, null);
            }
        }

        return null;
    }

    private void log(String message) {
        Log.log(message);
        System.out.println(message);
    }
}
