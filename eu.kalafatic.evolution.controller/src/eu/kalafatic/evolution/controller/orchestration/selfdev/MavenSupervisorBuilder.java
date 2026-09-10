package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenSupervisorBuilder extends AbstractProjectBuilder implements SupervisorBuilder {

    public MavenSupervisorBuilder() {
        super("build_supervisor");
    }

    @Override
    public TaskResult build(SelfDevContext context) {
        if (context == null) {
            return TaskResult.failure("build_supervisor", "SelfDevContext is null", null);
        }

        File srcDir = context.getSourceDirectory() != null && context.getSourceDirectory().exists() ?
                context.getSourceDirectory() : context.getProjectRoot();

        File supervisorModuleDir = new File(srcDir, "eu.kalafatic.evolution.supervisor");
        if (!supervisorModuleDir.exists()) {
            return TaskResult.failure("build_supervisor", "Supervisor module directory does not exist: " + supervisorModuleDir.getAbsolutePath(), null);
        }

        File logFile = getLogFile(context, "supervisor_build.log");
        List<String> goals = Arrays.asList("clean", "package");
        List<String> args = Arrays.asList("-DskipTests");

        TaskResult buildResult = mavenExecutor.executeBuild(supervisorModuleDir, goals, args, logFile, 15);
        if (!buildResult.isSuccess()) {
            return buildResult;
        }

        BuildArtifact artifact = getArtifact(context);
        return new TaskResult.Builder("build_supervisor")
                .status(TaskStatus.SUCCESS)
                .message("Supervisor build completed successfully.")
                .duration(buildResult.getDuration())
                .artifact(artifact)
                .logFile(logFile)
                .build();
    }

    @Override
    public BuildArtifact getArtifact(SelfDevContext context) {
        if (context == null) return null;

        File srcDir = context.getSourceDirectory() != null && context.getSourceDirectory().exists() ?
                context.getSourceDirectory() : context.getProjectRoot();

        File targetDir = new File(srcDir, "eu.kalafatic.evolution.supervisor/target");
        if (targetDir.exists()) {
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
}
