package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TaskResult {
    private final String taskId;
    private final TaskStatus status;
    private final String message;
    private final Throwable error;
    private final String command;
    private final File workingDirectory;
    private final int exitCode;
    private final long duration;
    private final BuildArtifact artifact;
    private final File logFile;
    private final Map<String, Object> diagnostics;

    private TaskResult(Builder builder) {
        this.taskId = builder.taskId;
        this.status = builder.status != null ? builder.status : TaskStatus.READY;
        this.message = builder.message != null ? builder.message : "";
        this.error = builder.error;
        this.command = builder.command != null ? builder.command : "";
        this.workingDirectory = builder.workingDirectory;
        this.exitCode = builder.exitCode;
        this.duration = builder.duration;
        this.artifact = builder.artifact;
        this.logFile = builder.logFile;
        this.diagnostics = new HashMap<>(builder.diagnostics);
    }

    public static TaskResult success(String taskId, String message) {
        return new Builder(taskId).status(TaskStatus.SUCCESS).message(message).build();
    }

    public static TaskResult failure(String taskId, String message, Throwable error) {
        return new Builder(taskId).status(TaskStatus.FAILED).message(message).error(error).build();
    }

    public static TaskResult skipped(String taskId, String message) {
        return new Builder(taskId).status(TaskStatus.SKIPPED).message(message).build();
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == TaskStatus.SUCCESS;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }

    public String getCommand() {
        return command;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public int getExitCode() {
        return exitCode;
    }

    public long getDuration() {
        return duration;
    }

    public BuildArtifact getArtifact() {
        return artifact;
    }

    public File getLogFile() {
        return logFile;
    }

    public Map<String, Object> getDiagnostics() {
        return Collections.unmodifiableMap(diagnostics);
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "taskId='" + taskId + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", exitCode=" + exitCode +
                ", duration=" + duration + "ms" +
                '}';
    }

    public static class Builder {
        private final String taskId;
        private TaskStatus status = TaskStatus.READY;
        private String message = "";
        private Throwable error;
        private String command = "";
        private File workingDirectory;
        private int exitCode = 0;
        private long duration = 0;
        private BuildArtifact artifact;
        private File logFile;
        private final Map<String, Object> diagnostics = new HashMap<>();

        public Builder(String taskId) {
            this.taskId = taskId;
        }

        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder workingDirectory(File workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder artifact(BuildArtifact artifact) {
            this.artifact = artifact;
            return this;
        }

        public Builder logFile(File logFile) {
            this.logFile = logFile;
            return this;
        }

        public Builder diagnostic(String key, Object value) {
            if (key != null && value != null) {
                this.diagnostics.put(key, value);
            }
            return this;
        }

        public TaskResult build() {
            return new TaskResult(this);
        }
    }
}
