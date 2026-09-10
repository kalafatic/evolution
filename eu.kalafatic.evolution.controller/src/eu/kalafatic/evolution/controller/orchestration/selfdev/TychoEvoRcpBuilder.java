package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TychoEvoRcpBuilder extends AbstractProjectBuilder implements EvoRcpBuilder {

    public TychoEvoRcpBuilder() {
        super("build_evo_rcp");
    }

    @Override
    public TaskResult build(SelfDevContext context) {
        if (context == null) {
            return TaskResult.failure("build_evo_rcp", "SelfDevContext is null", null);
        }

        File srcDir = context.getSourceDirectory() != null && context.getSourceDirectory().exists() ?
                context.getSourceDirectory() : context.getProjectRoot();

        File logFile = getLogFile(context, "evo_build.log");
        List<String> goals = Arrays.asList("clean", "install");
        List<String> args = Arrays.asList("-DskipTests");

        TaskResult buildResult = mavenExecutor.executeBuild(srcDir, goals, args, logFile, 45);
        if (!buildResult.isSuccess()) {
            return buildResult;
        }

        BuildArtifact artifact = getArtifact(context);
        return new TaskResult.Builder("build_evo_rcp")
                .status(TaskStatus.SUCCESS)
                .message("EVO RCP project build completed.")
                .duration(buildResult.getDuration())
                .artifact(artifact)
                .logFile(logFile)
                .build();
    }

    @Override
    public TaskResult exportProduct(SelfDevContext context) {
        if (context == null) {
            return TaskResult.failure("export_evo_rcp", "SelfDevContext is null", null);
        }

        BuildArtifact artifact = getArtifact(context);
        if (artifact != null && artifact.getPath().exists()) {
            return new TaskResult.Builder("export_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("Found EVO RCP product artifact: " + artifact.getPath().getAbsolutePath())
                    .artifact(artifact)
                    .build();
        }

        TaskResult bRes = build(context);
        if (!bRes.isSuccess()) {
            return TaskResult.failure("export_evo_rcp", "Failed to build EVO product for export: " + bRes.getMessage(), bRes.getError());
        }

        artifact = getArtifact(context);
        if (artifact != null && artifact.getPath().exists()) {
            return new TaskResult.Builder("export_evo_rcp")
                    .status(TaskStatus.SUCCESS)
                    .message("EVO RCP product built and exported: " + artifact.getPath().getAbsolutePath())
                    .artifact(artifact)
                    .build();
        }

        return TaskResult.failure("export_evo_rcp", "Could not locate exported EVO RCP product artifact post-build.", null);
    }

    @Override
    public BuildArtifact getArtifact(SelfDevContext context) {
        if (context == null) return null;

        File srcDir = context.getSourceDirectory() != null && context.getSourceDirectory().exists() ?
                context.getSourceDirectory() : context.getProjectRoot();

        File repoTarget = new File(srcDir, "eu.kalafatic.evolution.repository/target/products");
        File viewTarget = new File(srcDir, "eu.kalafatic.evolution.view/target/products");

        File productDir = repoTarget.exists() ? repoTarget : (viewTarget.exists() ? viewTarget : null);
        if (productDir != null && productDir.exists()) {
            File[] archives = productDir.listFiles((dir, name) -> name.endsWith(".zip") || name.endsWith(".tar.gz"));
            if (archives != null && archives.length > 0) {
                return new BuildArtifact(ArtifactType.EVO_RCP, archives[0], context.getSourceRevision(), null, null);
            }
            File[] dirs = productDir.listFiles(File::isDirectory);
            if (dirs != null && dirs.length > 0) {
                return new BuildArtifact(ArtifactType.EVO_RCP, dirs[0], context.getSourceRevision(), null, null);
            }
        }

        File exportDir = context.getExportDirectory();
        if (exportDir != null && exportDir.exists()) {
            File[] archives = exportDir.listFiles((dir, name) -> name.endsWith(".zip") || name.endsWith(".tar.gz"));
            if (archives != null && archives.length > 0) {
                return new BuildArtifact(ArtifactType.EVO_RCP, archives[0], context.getSourceRevision(), null, null);
            }
        }

        return null;
    }
}
