package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.Collections;
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
        SelfDevTask git = new GitCheckTask("GIT");
        SelfDevTask gitEvo = new GitCheckTask("GIT_EVO");
        SelfDevTask gitSuper = new GitCheckTask("GIT_SUPERVISOR");

        SelfDevTask copy = new CopySourceTask("COPY");
        SelfDevTask copySuper = new CopySourceTask("COPY_SUPERVISOR");

        SelfDevTask build = new BuildEvoTask("BUILD");
        build.addDependency("COPY");
        SelfDevTask buildEvo = new BuildEvoTask("BUILD_EVO");
        buildEvo.addDependency("COPY");
        SelfDevTask maven = new BuildEvoTask("MAVEN");
        maven.addDependency("COPY");
        SelfDevTask mavenEvo = new BuildEvoTask("MAVEN_EVO");
        mavenEvo.addDependency("COPY");

        SelfDevTask buildSuperLocal = new BuildSupervisorTask("BUILD_SUPERVISOR_LOCAL");
        buildSuperLocal.addDependency("COPY_SUPERVISOR");
        SelfDevTask buildSuper = new BuildSupervisorTask("BUILD_SUPERVISOR");
        buildSuper.addDependency("BUILD_SUPERVISOR_LOCAL");
        SelfDevTask mavenSuper = new BuildSupervisorTask("MAVEN_SUPERVISOR");
        mavenSuper.addDependency("BUILD_SUPERVISOR_LOCAL");

        SelfDevTask export = new ExportEvoTask("EXPORT");
        export.addDependency("BUILD_EVO");
        SelfDevTask exportEvo = new ExportEvoTask("EXPORT_EVO");
        exportEvo.addDependency("BUILD_EVO");
        SelfDevTask exportSuper = new ExportSupervisorTask("EXPORT_SUPERVISOR");
        exportSuper.addDependency("BUILD_SUPERVISOR_LOCAL");

        SelfDevTask startEvo = new StartEvoTask("START_EVO");
        startEvo.addDependency("EXPORT_EVO");
        SelfDevTask stopEvo = new StopEvoTask("STOP_EVO");

        SelfDevTask startSuper = new StartSupervisorTask("START_SUPERVISOR");
        startSuper.addDependency("EXPORT_SUPERVISOR");
        SelfDevTask startEvoSuper = new StartSupervisorTask("START_EVO_SUPERVISOR");
        startEvoSuper.addDependency("EXPORT_SUPERVISOR");
        SelfDevTask stopSuper = new StopSupervisorTask("STOP_SUPERVISOR");
        SelfDevTask stopEvoSuper = new StopSupervisorTask("STOP_EVO_SUPERVISOR");

        SelfDevTask supervisorCheck = new SupervisorCheckTask("SUPERVISOR");
        supervisorCheck.addDependency("START_SUPERVISOR");

        SelfDevTask genome = new GenomeCheckTask("GENOME");
        SelfDevTask permissions = new PermissionsCheckTask("PERMISSIONS");

        taskRegistry.put("GIT", git);
        taskRegistry.put("GIT_EVO", gitEvo);
        taskRegistry.put("GIT_SUPERVISOR", gitSuper);

        taskRegistry.put("COPY", copy);
        taskRegistry.put("COPY_SUPERVISOR", copySuper);

        taskRegistry.put("BUILD", build);
        taskRegistry.put("BUILD_EVO", buildEvo);
        taskRegistry.put("MAVEN", maven);
        taskRegistry.put("MAVEN_EVO", mavenEvo);

        taskRegistry.put("BUILD_SUPERVISOR", buildSuper);
        taskRegistry.put("BUILD_SUPERVISOR_LOCAL", buildSuperLocal);
        taskRegistry.put("MAVEN_SUPERVISOR", mavenSuper);

        taskRegistry.put("EXPORT", export);
        taskRegistry.put("EXPORT_EVO", exportEvo);
        taskRegistry.put("EXPORT_SUPERVISOR", exportSuper);

        taskRegistry.put("START_EVO", startEvo);
        taskRegistry.put("STOP_EVO", stopEvo);
        taskRegistry.put("START_SUPERVISOR", startSuper);
        taskRegistry.put("START_EVO_SUPERVISOR", startEvoSuper);
        taskRegistry.put("STOP_SUPERVISOR", stopSuper);
        taskRegistry.put("STOP_EVO_SUPERVISOR", stopEvoSuper);

        taskRegistry.put("SUPERVISOR", supervisorCheck);
        taskRegistry.put("GENOME", genome);
        taskRegistry.put("PERMISSIONS", permissions);
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
