package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Controller and backward-compatible orchestrator wrapper for Self-Development tasks and product lifecycles.
 */
public class SelfDevBootstrapController {

    private final SelfDevOrchestrator orchestrator;

    public SelfDevBootstrapController(File projectRoot, Orchestrator orchestrator) {
        this.orchestrator = new SelfDevOrchestrator(projectRoot, orchestrator);
    }

    public SelfDevBootstrapController(SelfDevOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public void setDebugMode(boolean debugMode) {
        System.out.println("[SelfDevBootstrapController] Setting debug mode: " + debugMode);
        orchestrator.getContext().setDebugMode(debugMode);
    }

    public boolean isDebugMode() {
        return orchestrator.getContext().isDebugMode();
    }

    public void startBootstrap() {
        orchestrator.startBootstrap();
    }

    public void stopBootstrap() {
        orchestrator.stopBootstrap();
    }

    public boolean isRunning() {
        return orchestrator.isRunning();
    }

    public JSONObject getStatus() {
        JSONObject json = new JSONObject();
        json.put("running", isRunning());
        json.put("phase", isRunning() ? "RUNNING" : "STOPPED");
        return json;
    }

    public String check(String checkType) {
        return orchestrator.executeCheck(checkType);
    }

    public String checkSourceSnapshotIntegrity(File candidateRoot) {
        return orchestrator.checkSourceSnapshotIntegrity(candidateRoot);
    }

    public SelfDevOrchestrator getOrchestrator() {
        return orchestrator;
    }

    // Reflective helper methods preserved for test backward compatibility
    private String checkGenome() {
        return check("GENOME");
    }

    private File findSupervisorDir() {
        File srcDir = orchestrator.getContext().getSourceDirectory();
        return new File(srcDir != null && srcDir.exists() ? srcDir : orchestrator.getContext().getProjectRoot(), "eu.kalafatic.evolution.supervisor");
    }

    private String compileSupervisorModule(File supervisorDir) {
        TaskResult res = orchestrator.getSupervisorLifecycle().build(orchestrator.getContext());
        return res.isSuccess() ? "SUCCESS" : "FAIL: " + res.getMessage();
    }
}
