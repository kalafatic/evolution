package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RcpBuildTool {

    public BuildResult build(BuildConfiguration config) {
        long startTime = System.currentTimeMillis();
        String os = System.getProperty("os.name").toLowerCase();
        String mvnCmd = os.contains("win") ? "mvn.cmd" : "mvn";

        List<String> command = new ArrayList<>();
        command.add(mvnCmd);
        if (config.getGoals().isEmpty()) {
            command.add("clean");
            command.add("package");
        } else {
            command.addAll(config.getGoals());
        }

        if (config.isSkipTests()) {
            command.add("-DskipTests");
        }

        for (String profile : config.getProfiles()) {
            command.add("-P" + profile);
        }

        if (!command.contains("-B") && !command.contains("--batch-mode")) {
            command.add("-B");
        }
        if (!command.contains("-ntp") && !command.contains("--no-transfer-progress")) {
            command.add("-ntp");
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(config.getWorkspacePath());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {}

            String stdout = readStream(process.getInputStream());
            String stderr = "";

            int exitCode = process.waitFor();
            long duration = System.currentTimeMillis() - startTime;

            boolean success = (exitCode == 0);
            String summary;
            if (success) {
                summary = "Build successful";
            } else {
                StringBuilder errSb = new StringBuilder("Build failed with exit code ").append(exitCode);
                if (stderr != null && !stderr.trim().isEmpty()) {
                    errSb.append(": ").append(stderr.trim().replaceAll("\\r?\\n", " "));
                } else if (stdout != null && stdout.contains("ERROR")) {
                    String[] lines = stdout.split("\\r?\\n");
                    for (String l : lines) {
                        if (l.contains("[ERROR]")) {
                            errSb.append(" | ").append(l.trim());
                        }
                    }
                }
                summary = errSb.toString();
            }

            BuildResult result = new BuildResult(success, summary, exitCode, stdout, stderr, duration);

            if (success) {
                result.setProducedArtifact(findArtifact(config.getWorkspacePath()));
            }

            return result;

        } catch (Exception e) {
            System.err.println("[RcpBuildTool] Build execution error: " + e.getMessage());
            e.printStackTrace();
            return new BuildResult(false, "Build process failed: " + e.getMessage(),
                    -1, "", e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    private String readStream(InputStream is) {
        return new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
    }

    private File findArtifact(File workspace) {
        File wsBuildDir = new File(System.getProperty("user.home"), "workspace/runtime/builds");
        File supervisorTarget = new File(wsBuildDir, "eu.kalafatic.evolution.supervisor/target");
        if (supervisorTarget.exists()) {
            File[] jars = supervisorTarget.listFiles((dir, name) -> name.endsWith("-shaded.jar") || (name.endsWith(".jar") && !name.endsWith("-sources.jar")));
            if (jars != null && jars.length > 0) {
                return jars[0];
            }
        }

        if (workspace != null) {
            File localSupervisorTarget = new File(workspace, "eu.kalafatic.evolution.supervisor/target");
            if (localSupervisorTarget.exists()) {
                File[] jars = localSupervisorTarget.listFiles((dir, name) -> name.endsWith("-shaded.jar") || (name.endsWith(".jar") && !name.endsWith("-sources.jar")));
                if (jars != null && jars.length > 0) {
                    return jars[0];
                }
            }
        }
        return null;
    }
}
