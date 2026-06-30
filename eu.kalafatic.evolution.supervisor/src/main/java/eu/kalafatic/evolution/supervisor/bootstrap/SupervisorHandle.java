package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.File;

public class SupervisorHandle {
    private final Process process;
    private final long startTime;
    private File stdoutFile;
    private File stderrFile;

    public SupervisorHandle(Process process) {
        this.process = process;
        this.startTime = System.currentTimeMillis();
    }

    public File getStdoutFile() {
        return stdoutFile;
    }

    public void setStdoutFile(File stdoutFile) {
        this.stdoutFile = stdoutFile;
    }

    public File getStderrFile() {
        return stderrFile;
    }

    public void setStderrFile(File stderrFile) {
        this.stderrFile = stderrFile;
    }

    public Process getProcess() {
        return process;
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public long getUptimeMs() {
        return System.currentTimeMillis() - startTime;
    }

    public void stop() {
        if (process != null) {
            process.destroy();
        }
    }
}
