package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
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

        SelfDevBuildPolicy policy = SelfDevBuildPolicy.resolve(context);
        File logFile = getLogFile(context, "supervisor_build.log");
        List<String> goals = Arrays.asList("validate", "clean", "package");
        List<String> args = new ArrayList<>();
        if (policy.isSkipTests()) {
            args.add("-DskipTests");
        }

        // WP3 (D-13): supervisor reactor builds must use the isolated source copy,
        // never the canonical Git repository root.
        File isolatedReactor = context.getPreparedReactorDirectory();
        if (isolatedReactor != null && new File(isolatedReactor, "pom.xml").exists()) {
            File genomeDir = new File(isolatedReactor, "eu.kalafatic.evolution.selfdev.genome");
            if (genomeDir.exists()) {
                goals = Arrays.asList("validate", "clean", "install");
                args = new ArrayList<>(Arrays.asList("-pl", "eu.kalafatic.evolution.supervisor", "-am"));
                if (policy.isSkipTests()) {
                    args.add("-DskipTests");
                }
                log("[MAVEN] Building Supervisor module via isolated reactor at " + isolatedReactor.getAbsolutePath() + " (policy: " + policy + ")");
                TaskResult buildResult = mavenExecutor.executeBuild(isolatedReactor, goals, args, logFile, 15);
                if (!buildResult.isSuccess()) {
                    return buildResult;
                }
                BuildArtifact artifact = getArtifact(context);
                if (artifact == null || artifact.getPath() == null || !artifact.getPath().exists() || artifact.getPath().length() == 0) {
                    return TaskResult.failure("build_supervisor", "Maven returned exit code 0 but supervisor JAR artifact was missing, empty, or unverified at " + (supervisorModuleDir != null ? supervisorModuleDir.getAbsolutePath() : "null"), null);
                }
                // WP4: record authoritative provenance (size, SHA-256, build ID).
                ArtifactManifestStore manifestStore = context.getArtifactManifestStore();
                artifact = manifestStore.record(ArtifactType.SUPERVISOR, artifact.getPath(),
                        context.getSourceRevision(), manifestStore.nextBuildId(),
                        "SUCCESS", policy.isSkipTests() ? "SKIPPED_BY_POLICY" : "BUILD_EXECUTED",
                        null, java.util.Collections.emptyMap());
                context.recordArtifact(artifact);
                return new TaskResult.Builder("build_supervisor")
                        .status(TaskStatus.SUCCESS)
                        .message("Supervisor build completed and artifact verified: " + artifact.getPath().getAbsolutePath())
                        .duration(buildResult.getDuration())
                        .artifact(artifact)
                        .logFile(logFile)
                        .diagnostic("buildPolicy", policy.toString())
                        .build();
            }
        }

        // Refuse to build directly inside the canonical repository (D-13).
        File repoRoot = context.getRepositoryRoot();
        try {
            if (repoRoot != null && repoRoot.getCanonicalFile().equals(supervisorModuleDir.getCanonicalFile())) {
                return TaskResult.failure("build_supervisor", "Refusing to build supervisor module at canonical repository root " + supervisorModuleDir.getAbsolutePath() + ". Run COPY_SUPERVISOR and build from the isolated reactor.", null);
            }
        } catch (Exception e) {
            return TaskResult.failure("build_supervisor", "Supervisor build location validation exception: " + e.getMessage(), e);
        }

        log("[MAVEN] Building Supervisor module at " + supervisorModuleDir.getAbsolutePath() + " (policy: " + policy + ")");
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

        // WP4: record authoritative provenance (size, SHA-256, build ID).
        ArtifactManifestStore manifestStore = context.getArtifactManifestStore();
        artifact = manifestStore.record(ArtifactType.SUPERVISOR, artifact.getPath(),
                context.getSourceRevision(), manifestStore.nextBuildId(),
                "SUCCESS", policy.isSkipTests() ? "SKIPPED_BY_POLICY" : "BUILD_EXECUTED",
                null, java.util.Collections.emptyMap());
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

        // WP4: authoritative resolution through the per-run manifest store.
        try {
            BuildArtifact recorded = context.getArtifactManifestStore().resolve(ArtifactType.SUPERVISOR);
            if (recorded != null) {
                return recorded;
            }
        } catch (Exception e) {
            System.err.println("[MavenSupervisorBuilder] Manifest artifact resolution failed: " + e.getMessage());
            return null;
        }

        File supervisorModuleDir = context.getSupervisorDirectory();
        File buildDir = context.getBuildDirectory();
        java.util.List<File> candidateDirs = new ArrayList<>();
        File repoRoot = context.getRepositoryRoot();
        if (buildDir != null && buildDir.exists()) {
            candidateDirs.add(new File(buildDir, (supervisorModuleDir != null ? supervisorModuleDir.getName() : "eu.kalafatic.evolution.supervisor") + "/target"));
        }
        if (supervisorModuleDir != null && !isInside(repoRoot, supervisorModuleDir)) {
            // WP3/D-38: artifact discovery from the canonical repository is forbidden.
            candidateDirs.add(new File(supervisorModuleDir, "target"));
        }
        File exportDir = context.getExportDirectory();
        if (exportDir != null && exportDir.exists()) {
            candidateDirs.add(exportDir);
        }

        for (File targetDir : candidateDirs) {
            File[] all = targetDir.listFiles((dir, name) -> name.endsWith(".jar")
                    && name.startsWith("eu.kalafatic.evolution.supervisor")
                    && !name.contains("-sources") && !name.contains("-javadoc") && !name.contains("-original") && !name.contains("-tests"));
            if (all == null || all.length == 0) {
                continue;
            }
            // Deterministic selection: the shaded versioned jar is preferred; any
            // remaining ambiguity is an error, never a first-match pick (WP4, D-10).
            java.util.List<File> versioned = java.util.Arrays.stream(all)
                    .filter(f -> f.getName().matches("eu\\.kalafatic\\.evolution\\.supervisor-\\d.*\\.jar"))
                    .toList();
            if (versioned.size() == 1) {
                return new BuildArtifact(ArtifactType.SUPERVISOR, versioned.get(0), context.getSourceRevision(), null, null);
            }
            if (all.length == 1) {
                return new BuildArtifact(ArtifactType.SUPERVISOR, all[0], context.getSourceRevision(), null, null);
            }
            System.err.println("[MavenSupervisorBuilder] Ambiguous supervisor artifact candidates in " + targetDir
                    + ": " + java.util.Arrays.toString(all) + " - refusing first-match selection.");
            return null;
        }

        return null;
    }

    private static boolean isInside(File root, File candidate) {
        if (root == null || candidate == null) {
            return false;
        }
        try {
            String rootCanon = root.getCanonicalPath();
            String candidateCanon = candidate.getCanonicalPath();
            return candidateCanon.equals(rootCanon) || candidateCanon.startsWith(rootCanon + File.separator);
        } catch (Exception e) {
            return false;
        }
    }

    private void log(String message) {
        Log.log(message);
        System.out.println(message);
    }
}
