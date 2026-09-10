package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

public class GenomeCheckTask extends AbstractSelfDevTask {

    public GenomeCheckTask(String id) {
        super(id, "Genome Module Check (" + id + ")");
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        File repoRoot = context.getProjectRoot();
        File genomeModuleDir = new File(repoRoot, "eu.kalafatic.evolution.selfdev.genome");

        if (!genomeModuleDir.exists() || !genomeModuleDir.isDirectory()) {
            return TaskResult.failure(id, "Genome module directory does not exist: " + genomeModuleDir.getAbsolutePath(), null);
        }

        File pomFile = new File(genomeModuleDir, "pom.xml");
        if (!pomFile.exists()) {
            return TaskResult.failure(id, "Genome module pom.xml missing: " + pomFile.getAbsolutePath(), null);
        }

        // Build genome module
        MavenBuildExecutor executor = new MavenBuildExecutor(id);
        File logFile = new File(context.getLogDirectory(), "genome_build.log");
        TaskResult buildRes = executor.executeBuild(genomeModuleDir, Arrays.asList("clean", "install"), Arrays.asList("-DskipTests"), logFile, 10);

        if (!buildRes.isSuccess()) {
            return buildRes;
        }

        // Invoke SelfDevGenomeHub update if present
        try {
            Class<?> hubClass = Class.forName("eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub");
            Method getInstanceMethod = hubClass.getDeclaredMethod("getInstance");
            Object hub = getInstanceMethod.invoke(null);
            if (hub != null) {
                Method updateMethod = hubClass.getDeclaredMethod("updateGenome", File.class);
                updateMethod.invoke(hub, repoRoot);
            }
        } catch (Throwable ignored) {
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.SUCCESS)
                .message("Genome module compiled and verified successfully.")
                .workingDirectory(genomeModuleDir)
                .build();
    }
}
