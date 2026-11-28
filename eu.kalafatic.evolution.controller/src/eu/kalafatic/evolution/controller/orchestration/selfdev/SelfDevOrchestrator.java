package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class SelfDevOrchestrator {

    private final SelfDevContext context;
    private final EvoRcpLifecycle evoLifecycle;
    private final SupervisorLifecycle supervisorLifecycle;
    private final Map<String, SelfDevTask> taskRegistry = new HashMap<>();
    private final List<String> graphValidationErrors = new ArrayList<>();

    /**
     * Task IDs whose cached SUCCESS may be reused without re-execution:
     * pure, side-effect-free checks. Process-affecting, build, export, deploy,
     * start/stop and rollback tasks must always re-verify.
     */
    private static final Set<String> REUSABLE_CHECK_TASKS = Set.of(
            "GIT", "GIT_EVO", "GIT_SUPERVISOR", "LLM", "PERMISSIONS", "SUPERVISOR");

    private volatile SelfDevPreflightResult lastPreflightResult;
    private volatile boolean preflightAttempted;

    public SelfDevOrchestrator(File projectRoot, Orchestrator orchestrator) {
        this.context = new SelfDevContext(projectRoot, orchestrator);
        this.evoLifecycle = new DefaultEvoRcpLifecycle();
        this.supervisorLifecycle = new DefaultSupervisorLifecycle();

        registerTasks();
        validateTaskGraph();
    }

    public SelfDevOrchestrator(SelfDevContext context,
                               EvoRcpLifecycle evoLifecycle,
                               SupervisorLifecycle supervisorLifecycle) {
        this.context = context;
        this.evoLifecycle = evoLifecycle != null ? evoLifecycle : new DefaultEvoRcpLifecycle();
        this.supervisorLifecycle = supervisorLifecycle != null ? supervisorLifecycle : new DefaultSupervisorLifecycle();

        registerTasks();
        validateTaskGraph();
    }

    public synchronized SelfDevPreflightResult executePreflight() {
        return executePreflightInternal();
    }

    private SelfDevPreflightResult executePreflightInternal() {
        SelfDevPreflight preflight = new SelfDevPreflight();
        SelfDevPreflightResult res = preflight.executePreflight(context, this);
        if (res.isSuccess() && res.getResolvedResources() != null) {
            context.setResolvedResources(res.getResolvedResources());
        } else {
            System.out.println("[SelfDevOrchestrator] Preflight " + res.getStatus()
                    + ": resolved resource snapshot NOT installed; tasks remain blocked until preflight succeeds.");
        }
        this.lastPreflightResult = res;
        this.preflightAttempted = true;
        return res;
    }

    /**
     * Orchestrator-level preflight gate: every task execution entry point must have a
     * valid resolved resource snapshot for the current run. In production the snapshot
     * can only be installed by a successful preflight (WP1 gating); when absent, the
     * gate runs preflight automatically on first use.
     *
     * @return null when the gate passes; otherwise the failed preflight result.
     */
    private SelfDevPreflightResult ensurePreflightGate() {
        if (context.getResolvedResources() != null) {
            return null;
        }
        if (!preflightAttempted) {
            synchronized (this) {
                if (!preflightAttempted) {
                    executePreflightInternal();
                }
            }
        }
        SelfDevPreflightResult res = lastPreflightResult;
        if (res == null || !res.isSuccess()) {
            return res;
        }
        return null;
    }

    /**
     * Registers a task with duplicate-ID detection.
     */
    private void register(SelfDevTask task) {
        if (task == null || task.getId() == null) {
            throw new IllegalStateException("Cannot register null task.");
        }
        String taskId = task.getId().trim().toUpperCase();
        if (taskRegistry.containsKey(taskId)) {
            throw new IllegalStateException("Duplicate task registration detected for ID: " + taskId);
        }
        taskRegistry.put(taskId, task);
    }

    /**
     * Static DAG validation: unknown dependencies, self-dependencies and cycles.
     * Errors are recorded and exposed for preflight.
     */
    private void validateTaskGraph() {
        graphValidationErrors.clear();
        for (SelfDevTask task : taskRegistry.values()) {
            String taskId = task.getId();
            for (String depId : task.getDependencies()) {
                if (depId.equalsIgnoreCase(taskId)) {
                    graphValidationErrors.add("Self-dependency detected: task [" + taskId + "] depends on itself.");
                } else if (!taskRegistry.containsKey(depId)) {
                    graphValidationErrors.add("Unknown dependency: task [" + taskId + "] depends on unregistered task [" + depId + "].");
                }
            }
        }
        for (String taskId : taskRegistry.keySet()) {
            if (hasCycle(taskId, new HashSet<>(), new HashSet<>())) {
                graphValidationErrors.add("Cycle detected in task graph involving task [" + taskId + "].");
            }
        }
        if (!graphValidationErrors.isEmpty()) {
            System.err.println("[SelfDevOrchestrator] TASK GRAPH VALIDATION FAILED:");
            for (String err : graphValidationErrors) {
                System.err.println("  - " + err);
            }
        }
    }

    private boolean hasCycle(String taskId, Set<String> visiting, Set<String> visited) {
        if (visiting.contains(taskId)) {
            return true;
        }
        if (visited.contains(taskId)) {
            return false;
        }
        visiting.add(taskId);
        SelfDevTask task = taskRegistry.get(taskId);
        if (task != null) {
            for (String depId : task.getDependencies()) {
                if (hasCycle(depId, visiting, visited)) {
                    visiting.remove(taskId);
                    return true;
                }
            }
        }
        visiting.remove(taskId);
        visited.add(taskId);
        return false;
    }

    public List<String> getGraphValidationErrors() {
        return Collections.unmodifiableList(graphValidationErrors);
    }

    private void registerTasks() {
        register(new GitCheckTask("GIT"));
        register(new GitCheckTask("GIT_EVO"));
        register(new GitCheckTask("GIT_SUPERVISOR"));

        register(new CopySourceTask("COPY"));
        register(new CopySourceTask("COPY_SUPERVISOR"));

        // Canonical build/export/deploy IDs only; alias IDs (BUILD, MAVEN, EXPORT, DEPLOY)
        // were removed after confirming no production consumers depend on them.
        SelfDevTask buildEvo = new BuildEvoTask("BUILD_EVO");
        buildEvo.addDependency("COPY");
        register(buildEvo);
        SelfDevTask mavenEvo = new BuildEvoTask("MAVEN_EVO");
        mavenEvo.addDependency("COPY");
        register(mavenEvo);

        SelfDevTask buildSuperLocal = new BuildSupervisorTask("BUILD_SUPERVISOR_LOCAL");
        buildSuperLocal.addDependency("COPY_SUPERVISOR");
        register(buildSuperLocal);
        SelfDevTask buildSuper = new BuildSupervisorTask("BUILD_SUPERVISOR");
        buildSuper.addDependency("BUILD_SUPERVISOR_LOCAL");
        register(buildSuper);
        register(new BuildSupervisorTask("MAVEN_SUPERVISOR"));

        SelfDevTask exportEvo = new ExportEvoTask("EXPORT_EVO");
        exportEvo.addDependency("BUILD_EVO");
        register(exportEvo);
        SelfDevTask exportSuper = new ExportSupervisorTask("EXPORT_SUPERVISOR");
        exportSuper.addDependency("BUILD_SUPERVISOR_LOCAL");
        register(exportSuper);

        SelfDevTask deployEvo = new DeployEvoTask("DEPLOY_EVO");
        deployEvo.addDependency("EXPORT_EVO");
        register(deployEvo);

        SelfDevTask startEvo = new StartEvoTask("START_EVO");
        startEvo.addDependency("DEPLOY_EVO");
        register(startEvo);
        register(new StopEvoTask("STOP_EVO"));

        SelfDevTask startSuper = new StartSupervisorTask("START_SUPERVISOR");
        startSuper.addDependency("EXPORT_SUPERVISOR");
        register(startSuper);
        SelfDevTask startEvoSuper = new StartSupervisorTask("START_EVO_SUPERVISOR");
        startEvoSuper.addDependency("START_SUPERVISOR");
        startEvoSuper.addDependency("DEPLOY_EVO");
        register(startEvoSuper);
        register(new StopSupervisorTask("STOP_SUPERVISOR"));
        register(new StopSupervisorTask("STOP_EVO_SUPERVISOR"));

        SelfDevTask supervisorCheck = new SupervisorCheckTask("SUPERVISOR");
        supervisorCheck.addDependency("START_SUPERVISOR");
        register(supervisorCheck);

        register(new GenomeCheckTask("GENOME"));
        register(new PermissionsCheckTask("PERMISSIONS"));

        SelfDevTask createProject = new CreateEvoProjectTask("CREATE_EVO_PROJECT");
        createProject.addDependency("START_EVO");
        register(createProject);

        SelfDevTask createInference = new CreateInferenceTask("CREATE_INFERENCE_TASK");
        createInference.addDependency("CREATE_EVO_PROJECT");
        register(createInference);

        SelfDevTask analyticChat = new AnalyticChatValidationTask("ANALYTIC_CHAT_VALIDATION");
        analyticChat.addDependency("CREATE_INFERENCE_TASK");
        register(analyticChat);

        SelfDevTask promote = new PromoteTask("PROMOTE");
        promote.addDependency("START_EVO");
        register(promote);

        register(new RollbackTask("ROLLBACK"));

        register(new LlmCheckTask("LLM"));
        SelfDevTask supervisorLoop = new SupervisorLoopTask("SUPERVISOR_LOOP");
        supervisorLoop.addDependency("START_SUPERVISOR");
        register(supervisorLoop);
        register(new SelfDevLoopTask("SELF_DEV_LOOP"));
    }

    public Map<String, SelfDevTask> getTaskRegistry() {
        return Collections.unmodifiableMap(taskRegistry);
    }

    public SelfDevContext getContext() {
        return context;
    }

    public EvoRcpLifecycle getEvoRcpLifecycle() {
        return evoLifecycle;
    }

    public SupervisorLifecycle getSupervisorLifecycle() {
        return supervisorLifecycle;
    }

    public synchronized TaskResult executeTaskWithDependencies(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return TaskResult.failure("unknown", "Task ID is null or empty", null);
        }
        String normalizedId = taskId.trim().toUpperCase();

        // Preflight gate: every execution entry point must pass a successful preflight
        // for the current run before any task executes (D-39).
        SelfDevPreflightResult gateFailure = ensurePreflightGate();
        if (gateFailure != null) {
            String msg = "BLOCKED: self-dev preflight gate failed (status: " + gateFailure.getStatus()
                    + "). Correct the configuration and re-run preflight before executing task " + normalizedId + ".";
            System.err.println("[SelfDevOrchestrator] " + msg);
            return TaskResult.blocked(normalizedId, msg);
        }

        return executeTaskWithDependenciesInternal(normalizedId, new java.util.HashSet<>());
    }

    private TaskResult executeTaskWithDependenciesInternal(String taskId, java.util.Set<String> visitingStack) {
        SelfDevTask task = taskRegistry.get(taskId);
        if (task == null) {
            return TaskResult.failure(taskId, "Unknown task ID: " + taskId, null);
        }

        // Cached-result reuse is permitted ONLY for pure, side-effect-free checks.
        // Process-affecting, build, export, deploy, start/stop and rollback tasks
        // always re-verify (D-01 class of false-success defects).
        TaskResult existingResult = context.getTaskResult(taskId);
        if (existingResult != null && existingResult.isSuccess() && REUSABLE_CHECK_TASKS.contains(taskId)) {
            System.out.println("[SelfDevOrchestrator] Task [" + taskId + "] is a pure check and already succeeded. Reusing result.");
            return existingResult;
        }

        if (existingResult != null && (existingResult.getStatus() == TaskStatus.FAILED || existingResult.getStatus() == TaskStatus.BLOCKED)) {
            System.out.println("[SelfDevOrchestrator] Task [" + taskId + "] previously evaluated as " + existingResult.getStatus() + ". Returning existing result.");
            return existingResult;
        }

        if (visitingStack.contains(taskId)) {
            String msg = "Circular dependency detected involving task: " + taskId;
            System.err.println("[SelfDevOrchestrator] " + msg);
            return TaskResult.failure(taskId, msg, null);
        }

        visitingStack.add(taskId);

        try {
            // Process all explicit dependencies first in topological order
            for (String depId : task.getDependencies()) {
                System.out.println("[SelfDevOrchestrator] Task [" + taskId + "] requires dependency [" + depId + "]. Evaluating...");
                TaskResult depRes = context.getTaskResult(depId);

                if (depRes == null) {
                    System.out.println("[SelfDevOrchestrator] Dependency [" + depId + "] for task [" + taskId + "] not yet executed. Executing dependency...");
                    depRes = executeTaskWithDependenciesInternal(depId, visitingStack);
                }

                if (depRes == null || !depRes.isSuccess()) {
                    String depStatusStr = depRes != null ? depRes.getStatus().name() : "NOT_EXECUTED";
                    String msg = "BLOCKED: required dependency " + depId + " failed or could not be executed (status: " + depStatusStr + ", message: " + (depRes != null ? depRes.getMessage() : "none") + ").";
                    System.err.println("[SelfDevOrchestrator] Task [" + taskId + "] " + msg);
                    TaskResult blockedResult = TaskResult.blocked(taskId, msg);
                    context.recordTaskResult(blockedResult);
                    return blockedResult;
                }
            }

            // Dependencies satisfied, now execute the task itself
            System.out.println("[SelfDevOrchestrator] All dependencies satisfied for task [" + taskId + "]. Executing...");
            return task.execute(context);

        } finally {
            visitingStack.remove(taskId);
        }
    }

    public String executeCheck(String checkType) {
        if (checkType == null) return "FAIL";

        TaskResult res = executeTaskWithDependencies(checkType);
        if (res.isSuccess()) {
            return "SUCCESS";
        } else if (res.getStatus() == TaskStatus.BLOCKED) {
            return "BLOCKED: " + res.getMessage();
        } else {
            return "FAIL: " + res.getMessage();
        }
    }

    public TaskResult runTask(SelfDevTask task) {
        if (task == null) return TaskResult.failure("unknown", "Task is null", null);
        return executeTaskWithDependencies(task.getId());
    }

    public void startBootstrap() {
        System.out.println("[SelfDevOrchestrator] Starting bootstrap flow...");
        supervisorLifecycle.start(context);
    }

    public void stopBootstrap() {
        System.out.println("[SelfDevOrchestrator] Stopping bootstrap flow...");
        supervisorLifecycle.stop(context);
        evoLifecycle.stop(context);
    }

    public boolean isRunning() {
        TaskResult ping = supervisorLifecycle.sendCommand("ping", null);
        return ping.isSuccess();
    }

    public String checkSourceSnapshotIntegrity(File candidateRoot) {
        if (candidateRoot == null || !candidateRoot.exists()) {
            return "ERROR: Candidate directory is null or does not exist.";
        }

        File forgeTargetPkg = new File(candidateRoot, "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/forge/model/llm");
        if (!forgeTargetPkg.exists()) {
            return "SelfDev source snapshot is incomplete: expected Forge target package at " + forgeTargetPkg.getAbsolutePath();
        }

        File supervisorTargetPkg = new File(candidateRoot, "eu.kalafatic.evolution.supervisor/src/main/java/eu/kalafatic/evolution/supervisor");
        if (!supervisorTargetPkg.exists()) {
            return "SelfDev source snapshot is incomplete: expected Supervisor target package at " + supervisorTargetPkg.getAbsolutePath();
        }

        return null;
    }
}
