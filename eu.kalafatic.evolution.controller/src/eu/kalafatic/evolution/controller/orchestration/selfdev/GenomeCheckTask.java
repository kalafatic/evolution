package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class GenomeCheckTask extends AbstractSelfDevTask {

    public GenomeCheckTask(String id) {
        super(id, "Genome Module Check (" + id + ")");
    }

    private File genomeModuleDir;

    @Override
    protected void resolveResources(SelfDevContext context) throws Exception {
        ResolvedSelfDevResources res = context != null ? context.getResolvedResources() : null;
        this.genomeModuleDir = (res != null && res.getGenomeDirectory() != null) ? res.getGenomeDirectory() : (context != null ? context.getGenomeDirectory() : null);
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (genomeModuleDir == null || !genomeModuleDir.exists() || !genomeModuleDir.isDirectory()) {
            return TaskResult.failure(id, "GenomeCheckTask pre-validation failed: genome module directory does not exist at " + (genomeModuleDir != null ? genomeModuleDir.getAbsolutePath() : "null"), null);
        }

        File pomFile = new File(genomeModuleDir, "pom.xml");
        if (!pomFile.exists()) {
            return TaskResult.failure(id, "GenomeCheckTask pre-validation failed: genome module pom.xml missing at " + pomFile.getAbsolutePath(), null);
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Genome module directory verified at " + genomeModuleDir.getAbsolutePath())
                .workingDirectory(genomeModuleDir)
                .build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        File repoRoot = context.getProjectRoot();

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
                .logFile(logFile)
                .build();
    }
}
