package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SupervisorLauncherTool {

    public SupervisorHandle launch(SupervisorConfiguration config) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("java");

        for (Map.Entry<String, String> entry : config.getJvmSystemProperties().entrySet()) {
            command.add("-D" + entry.getKey() + "=" + entry.getValue());
        }

        command.add("-jar");
        command.add(config.getSupervisorJar().getAbsolutePath());
        command.add(config.getProjectRoot().getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(config.getProjectRoot());

        File logDir = new File(config.getProjectRoot(), "self-dev-run/logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        File stdoutFile = new File(logDir, "supervisor-stdout.log");
        File stderrFile = new File(logDir, "supervisor-stderr.log");

        pb.redirectOutput(stdoutFile);
        pb.redirectError(stderrFile);

        Map<String, String> env = pb.environment();
        env.putAll(config.getEnvironmentVariables());

        Process process = pb.start();
        SupervisorHandle handle = new SupervisorHandle(process);
        handle.setStdoutFile(stdoutFile);
        handle.setStderrFile(stderrFile);
        return handle;
    }
}
