package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.nio.file.Path;

/**
 * Immutable snapshot of resolved Self-Dev resources and paths.
 * Acts as the single source of truth for all tasks and context layers.
 */
public class ResolvedSelfDevResources {
    private final File repositoryRoot;
    private final File projectRoot;
    private final File sourceDirectory;
    private final File reactorDirectory;
    private final File buildDirectory;
    private final File exportDirectory;
    private final File runtimeDirectory;
    private final File logDirectory;
    private final File supervisorDirectory;
    private final File genomeDirectory;

    private final String os;
    private final String ws;
    private final String arch;
    private final String productId;
    private final String launcher;

    private final File javaExecutable;
    private final File mavenExecutable;

    public ResolvedSelfDevResources(File repositoryRoot, File projectRoot, File sourceDirectory,
                                   File reactorDirectory, File buildDirectory, File exportDirectory,
                                   File runtimeDirectory, File logDirectory, File supervisorDirectory,
                                   File genomeDirectory, String os, String ws, String arch,
                                   String productId, String launcher, File javaExecutable,
                                   File mavenExecutable) {
        this.repositoryRoot = normalize(repositoryRoot);
        this.projectRoot = normalize(projectRoot);
        this.sourceDirectory = normalize(sourceDirectory);
        this.reactorDirectory = normalize(reactorDirectory);
        this.buildDirectory = normalize(buildDirectory);
        this.exportDirectory = normalize(exportDirectory);
        this.runtimeDirectory = normalize(runtimeDirectory);
        this.logDirectory = normalize(logDirectory);
        this.supervisorDirectory = normalize(supervisorDirectory);
        this.genomeDirectory = normalize(genomeDirectory);
        this.os = os != null ? os : "win32";
        this.ws = ws != null ? ws : "win32";
        this.arch = arch != null ? arch : "x86_64";
        this.productId = productId != null ? productId : "evolution";
        this.launcher = launcher != null ? launcher : "evo";
        this.javaExecutable = normalize(javaExecutable);
        this.mavenExecutable = normalize(mavenExecutable);
    }

    private static File normalize(File f) {
        return f != null ? f.getAbsoluteFile().toPath().normalize().toFile() : null;
    }

    public File getRepositoryRoot() { return repositoryRoot; }
    public File getProjectRoot() { return projectRoot; }
    public File getSourceDirectory() { return sourceDirectory; }
    public File getReactorDirectory() { return reactorDirectory; }
    public File getBuildDirectory() { return buildDirectory; }
    public File getExportDirectory() { return exportDirectory; }
    public File getRuntimeDirectory() { return runtimeDirectory; }
    public File getLogDirectory() { return logDirectory; }
    public File getSupervisorDirectory() { return supervisorDirectory; }
    public File getGenomeDirectory() { return genomeDirectory; }

    public String getOs() { return os; }
    public String getWs() { return ws; }
    public String getArch() { return arch; }
    public String getProductId() { return productId; }
    public String getLauncher() { return launcher; }

    public File getJavaExecutable() { return javaExecutable; }
    public File getMavenExecutable() { return mavenExecutable; }

    @Override
    public String toString() {
        return "ResolvedSelfDevResources[" +
                "source=" + sourceDirectory +
                ", reactor=" + reactorDirectory +
                ", build=" + buildDirectory +
                ", export=" + exportDirectory +
                ", supervisor=" + supervisorDirectory +
                ", genome=" + genomeDirectory +
                ']';
    }
}
