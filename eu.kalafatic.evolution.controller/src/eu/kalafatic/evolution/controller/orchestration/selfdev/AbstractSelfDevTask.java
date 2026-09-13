package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractSelfDevTask implements SelfDevTask {
    protected final String id;
    protected final String name;
    protected final Set<String> dependencies = new LinkedHashSet<>();
    protected volatile TaskStatus status = TaskStatus.READY;
    protected volatile boolean cancelled = false;

    protected AbstractSelfDevTask(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    @Override
    public void addDependency(String dependencyTaskId) {
        if (dependencyTaskId != null && !dependencyTaskId.trim().isEmpty() && !dependencyTaskId.equalsIgnoreCase(this.id)) {
            this.dependencies.add(dependencyTaskId.trim().toUpperCase());
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TaskStatus getStatus() {
        return status;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public TaskResult execute(SelfDevContext context) {
        if (cancelled) {
            status = TaskStatus.SKIPPED;
            TaskResult res = TaskResult.skipped(id, "Task was cancelled before execution.");
            if (context != null) context.recordTaskResult(res);
            return res;
        }

        long startTime = System.currentTimeMillis();
        status = TaskStatus.RUNNING;
        logInfo(">>> Starting task [" + id + ": " + name + "]...");

        try {
            TaskResult preValidation = validate(context);
            if (preValidation != null && !preValidation.isSuccess() && preValidation.getStatus() != TaskStatus.READY) {
                status = preValidation.getStatus();
                logError("Pre-validation failed for task [" + id + "]: " + preValidation.getMessage());
                if (context != null) context.recordTaskResult(preValidation);
                return preValidation;
            }

            TaskResult result = run(context);
            long duration = System.currentTimeMillis() - startTime;

            TaskResult finalResult = new TaskResult.Builder(id)
                    .status(result.getStatus())
                    .message(result.getMessage())
                    .error(result.getError())
                    .command(result.getCommand())
                    .workingDirectory(result.getWorkingDirectory())
                    .exitCode(result.getExitCode())
                    .duration(duration)
                    .artifact(result.getArtifact())
                    .logFile(result.getLogFile())
                    .build();

            status = finalResult.getStatus();
            if (finalResult.isSuccess()) {
                logInfo("<<< Finished task [" + id + "]. Status: SUCCESS (took " + duration + "ms)");
            } else {
                logError("<<< Task [" + id + "] failed. Status: " + finalResult.getStatus() + ", Message: " + finalResult.getMessage());
            }

            if (context != null) {
                context.recordTaskResult(finalResult);
            }
            return finalResult;

        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            status = TaskStatus.FAILED;
            logError("<<< Task [" + id + "] threw exception: " + t.getMessage(), t);

            TaskResult errResult = new TaskResult.Builder(id)
                    .status(TaskStatus.FAILED)
                    .message("Task exception: " + t.getMessage())
                    .error(t)
                    .duration(duration)
                    .build();

            if (context != null) {
                context.recordTaskResult(errResult);
            }
            return errResult;
        }
    }

    @Override
    public TaskResult validate(SelfDevContext context) {
        if (context == null) {
            return TaskResult.failure(id, "SelfDevContext is null.", null);
        }
        for (String depId : getDependencies()) {
            TaskResult depResult = context.getTaskResult(depId);
            if (depResult == null || !depResult.isSuccess()) {
                String depStatusStr = depResult != null ? depResult.getStatus().name() : "NOT_EXECUTED";
                String msg = "BLOCKED: required dependency " + depId + " failed or was not executed (status: " + depStatusStr + ").";
                return TaskResult.blocked(id, msg);
            }
        }
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Context and dependencies valid").build();
    }

    protected abstract TaskResult run(SelfDevContext context) throws Exception;

    protected void logInfo(String message) {
        System.out.println("[" + getClass().getSimpleName() + "] " + message);
    }

    protected void logError(String message) {
        System.err.println("[" + getClass().getSimpleName() + "] " + message);
    }

    protected void logError(String message, Throwable t) {
        System.err.println("[" + getClass().getSimpleName() + "] " + message);
        if (t != null) {
            t.printStackTrace();
        }
    }
}
