package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class SelfDevOrchestrator {

    private final SelfDevContext context;
    private final EvoRcpLifecycle evoLifecycle;
    private final SupervisorLifecycle supervisorLifecycle;
    private final Map<String, SelfDevTask> taskRegistry = new HashMap<>();

    public SelfDevOrchestrator(File projectRoot, Orchestrator orchestrator) {
        this.context = new SelfDevContext(projectRoot, orchestrator);
        this.evoLifecycle = new DefaultEvoRcpLifecycle();
        this.supervisorLifecycle = new DefaultSupervisorLifecycle();

        registerTasks();
    }

    public SelfDevOrchestrator(SelfDevContext context,
                               EvoRcpLifecycle evoLifecycle,
                               SupervisorLifecycle supervisorLifecycle) {
        this.context = context;
        this.evoLifecycle = evoLifecycle != null ? evoLifecycle : new DefaultEvoRcpLifecycle();
        this.supervisorLifecycle = supervisorLifecycle != null ? supervisorLifecycle : new DefaultSupervisorLifecycle();

        registerTasks();
    }

    private void registerTasks() {
        taskRegistry.put("GIT", new GitCheckTask("GIT"));
        taskRegistry.put("GIT_EVO", new GitCheckTask("GIT_EVO"));
        taskRegistry.put("GIT_SUPERVISOR", new GitCheckTask("GIT_SUPERVISOR"));

        taskRegistry.put("COPY", new CopySourceTask("COPY"));
        taskRegistry.put("COPY_SUPERVISOR", new CopySourceTask("COPY_SUPERVISOR"));

        taskRegistry.put("BUILD", new BuildEvoTask("BUILD"));
        taskRegistry.put("BUILD_EVO", new BuildEvoTask("BUILD_EVO"));
        taskRegistry.put("MAVEN", new BuildEvoTask("MAVEN"));
        taskRegistry.put("MAVEN_EVO", new BuildEvoTask("MAVEN_EVO"));

        taskRegistry.put("BUILD_SUPERVISOR", new BuildSupervisorTask("BUILD_SUPERVISOR"));
        taskRegistry.put("BUILD_SUPERVISOR_LOCAL", new BuildSupervisorTask("BUILD_SUPERVISOR_LOCAL"));
        taskRegistry.put("MAVEN_SUPERVISOR", new BuildSupervisorTask("MAVEN_SUPERVISOR"));

        taskRegistry.put("EXPORT", new ExportEvoTask("EXPORT"));
        taskRegistry.put("EXPORT_EVO", new ExportEvoTask("EXPORT_EVO"));
        taskRegistry.put("EXPORT_SUPERVISOR", new ExportSupervisorTask("EXPORT_SUPERVISOR"));

        taskRegistry.put("START_EVO", new StartEvoTask("START_EVO"));
        taskRegistry.put("STOP_EVO", new StopEvoTask("STOP_EVO"));
        taskRegistry.put("START_SUPERVISOR", new StartSupervisorTask("START_SUPERVISOR"));
        taskRegistry.put("START_EVO_SUPERVISOR", new StartSupervisorTask("START_EVO_SUPERVISOR"));
        taskRegistry.put("STOP_SUPERVISOR", new StopSupervisorTask("STOP_SUPERVISOR"));
        taskRegistry.put("STOP_EVO_SUPERVISOR", new StopSupervisorTask("STOP_EVO_SUPERVISOR"));

        taskRegistry.put("SUPERVISOR", new SupervisorCheckTask("SUPERVISOR"));
        taskRegistry.put("GENOME", new GenomeCheckTask("GENOME"));
        taskRegistry.put("PERMISSIONS", new PermissionsCheckTask("PERMISSIONS"));
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

    public String executeCheck(String checkType) {
        if (checkType == null) return "FAIL";

        SelfDevTask task = taskRegistry.get(checkType.toUpperCase());
        if (task != null) {
            TaskResult res = task.execute(context);
            return res.isSuccess() ? "SUCCESS" : "FAIL: " + res.getMessage();
        }

        if ("LLM".equalsIgnoreCase(checkType)) {
            return "SUCCESS";
        }

        return "UNKNOWN";
    }

    public TaskResult runTask(SelfDevTask task) {
        if (task == null) return TaskResult.failure("unknown", "Task is null", null);
        return task.execute(context);
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
