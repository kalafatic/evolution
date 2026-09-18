package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import eu.kalafatic.evolution.controller.resource.ResourceManager;

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

    protected void logTaskStep(String stepName, String details) {
        System.out.println("[" + id + "] " + stepName + (details != null && !details.isEmpty() ? ": " + details : ""));
    }

    @Override
    public TaskResult execute(SelfDevContext context) {
        long startTime = System.currentTimeMillis();
        ResourceManager rm = context != null ? context.getResourceManager() : ResourceManager.getInstance();

        System.out.println("[SELF-DEV][TASK]\nid=" + id + "\nname=" + name + "\nphase=START");
        logTaskStep("==================================================", null);
        logTaskStep("START", "Task [" + id + ": " + name + "]");
        if (context != null) {
            logTaskStep("SELF-DEV PATHS", null);
            logTaskStep("  repository", String.valueOf(context.getRepositoryRoot()));
            logTaskStep("  preparedSource", String.valueOf(context.getPreparedReactorDirectory()));
            logTaskStep("  build", String.valueOf(context.getBuildDirectory()));
            logTaskStep("  export", String.valueOf(context.getExportDirectory()));
            logTaskStep("  targetOS", context.getOs() + "." + context.getWs() + "." + context.getArch());
        } else {
            logTaskStep("ResourceManager repository", String.valueOf(rm.getEvoGitRepository()));
        }
        logTaskStep("--------------------------------------------------", null);

        if (cancelled) {
            status = TaskStatus.SKIPPED;
            TaskResult res = TaskResult.skipped(id, "Task was cancelled before execution.");
            logTaskStep("STATE", status.name());
            logTaskStep("RESULT", res.getMessage());
            logTaskStep("END SKIPPED", res.getMessage());
            if (context != null) context.recordTaskResult(res);
            return res;
        }

        status = TaskStatus.RUNNING;

        try {
            // Step 1: Validate dependencies
            logTaskStep("DEPENDENCIES", dependencies.isEmpty() ? "None" : dependencies.toString());
            TaskResult depValidation = validateDependencies(context);
            if (depValidation != null && !depValidation.isSuccess()) {
                status = depValidation.getStatus();
                logTaskStep("PRE_VALIDATION", "FAILED: " + depValidation.getMessage());
                logTaskStep("STATE", status.name());
                logTaskStep("END BLOCKED", depValidation.getMessage());
                if (context != null) context.recordTaskResult(depValidation);
                return depValidation;
            }

            // Step 2: Resolve resources
            logTaskStep("RESOURCE_RESOLUTION", "Resolving required resources...");
            resolveResources(context);

            // Step 3: Pre-validate task conditions
            TaskResult preVal = preValidate(context);
            if (preVal != null && !preVal.isSuccess() && preVal.getStatus() != TaskStatus.READY) {
                status = preVal.getStatus();
                logTaskStep("PRE_VALIDATION", "FAILED: " + preVal.getMessage());
                logTaskStep("STATE", status.name());
                logTaskStep("END FAILED", preVal.getMessage());
                if (context != null) context.recordTaskResult(preVal);
                return preVal;
            }
            logTaskStep("PRE_VALIDATION", "PASSED");

            // Step 4: Execute main task logic
            System.out.println("[SELF-DEV][TASK]\nid=" + id + "\nphase=EXECUTE");
            logTaskStep("EXECUTION", "Executing task logic...");
            TaskResult runResult = run(context);

            // Step 5: Post-validate output artifacts and state
            logTaskStep("POST_VALIDATION", "Validating task output...");
            TaskResult finalResult = postValidate(context, runResult);

            long duration = System.currentTimeMillis() - startTime;
            TaskResult.Builder builder = new TaskResult.Builder(id)
                    .status(finalResult.getStatus())
                    .message(finalResult.getMessage())
                    .error(finalResult.getError())
                    .command(finalResult.getCommand())
                    .workingDirectory(finalResult.getWorkingDirectory())
                    .exitCode(finalResult.getExitCode())
                    .duration(duration)
                    .artifact(finalResult.getArtifact())
                    .logFile(finalResult.getLogFile());

            for (var entry : finalResult.getDiagnostics().entrySet()) {
                builder.diagnostic(entry.getKey(), entry.getValue());
            }

            TaskResult recordedResult = builder.build();
            status = recordedResult.getStatus();
            System.out.println("[SELF-DEV][TASK]\nid=" + id + "\nphase=RESULT\nstatus=" + status);

            if (recordedResult.getCommand() != null && !recordedResult.getCommand().isEmpty()) {
                logTaskStep("COMMAND", recordedResult.getCommand());
            }
            if (recordedResult.getWorkingDirectory() != null) {
                logTaskStep("WORKING_DIRECTORY", recordedResult.getWorkingDirectory().getAbsolutePath());
            }

            if (recordedResult.isSuccess()) {
                if (recordedResult.getArtifact() != null) {
                    logTaskStep("artifact", recordedResult.getArtifact().getPath().getAbsolutePath());
                }
                logTaskStep("duration", duration + "ms");
                logTaskStep("END SUCCESS", recordedResult.getMessage());
            } else {
                logTaskStep("END FAILED", recordedResult.getMessage());
                if (recordedResult.getCommand() != null) {
                    logTaskStep("command", recordedResult.getCommand());
                }
                if (recordedResult.getLogFile() != null) {
                    logTaskStep("logFile", recordedResult.getLogFile().getAbsolutePath());
                }
            }

            if (context != null) {
                context.recordTaskResult(recordedResult);
            }
            return recordedResult;

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

            logTaskStep("STATE", status.name());
            logTaskStep("END FAILED", errResult.getMessage());

            if (context != null) {
                context.recordTaskResult(errResult);
            }
            return errResult;
        }
    }

    @Override
    public TaskResult validate(SelfDevContext context) {
        return validateDependencies(context);
    }

    protected TaskResult validateDependencies(SelfDevContext context) {
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
        return TaskResult.success(id, "Dependencies valid");
    }

    protected void resolveResources(SelfDevContext context) throws Exception {
        // Default no-op hook for subclasses to resolve and verify required resources
    }

    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        return new TaskResult.Builder(id).status(TaskStatus.READY).message("Pre-validation passed").build();
    }

    protected TaskResult postValidate(SelfDevContext context, TaskResult runResult) throws Exception {
        return runResult;
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
