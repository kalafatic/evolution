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

    public SelfDevPreflightResult executePreflight() {
        return orchestrator.executePreflight();
    }

    public TaskResult runTask(String taskId) {
        return orchestrator.executeTaskWithDependencies(taskId);
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

    /**
     * WP8 - single controlled entry point for a Darwin-driven self-dev
     * iteration. The objective is recorded in the run-scoped ledger and then
     * delegated through the EXISTING Darwin chain
     * (OrchestratorServiceImpl -> KernelFacade -> IterationManager ->
     * ADarwinEngine). No parallel iteration engine is created.
     *
     * @param objective  the development objective
     * @param sessionId  the Darwin session to submit into (existing chain)
     * @return JSON status: runId, iterationId, ledger path, submission result.
     */
    public org.json.JSONObject startIteration(String objective, String sessionId) {
        SelfDevContext context = orchestrator.getContext();
        String iterationId = context.getRunId() + "-iter-" + System.currentTimeMillis();

        SelfDevIterationLedger ledger = new SelfDevIterationLedger(context);
        ledger.record(iterationId, SelfDevIterationLedger.Stage.OBJECTIVE_RECEIVED, "SUCCESS", null, objective);
        ledger.record(iterationId, SelfDevIterationLedger.Stage.ANALYSIS, "IN_PROGRESS", null, "Delegated to Darwin chain");

        // Capture the iteration baseline (read-only) so change attribution and
        // rollback are scoped to this iteration (WP9).
        try {
            File repoRoot = context.getRepositoryRoot();
            if (repoRoot != null && new GitManager(repoRoot).isGitRepository()) {
                GitBaseline baseline = GitBaseline.capture(context, new GitManager(repoRoot));
                GitBaseline.persist(context, baseline);
                ledger.record(iterationId, SelfDevIterationLedger.Stage.RUN_PREPARED, "SUCCESS", null,
                        "baseline=" + baseline.getHead());
            } else {
                ledger.record(iterationId, SelfDevIterationLedger.Stage.RUN_PREPARED, "WARNING", null,
                        "No git repository at " + (repoRoot != null ? repoRoot.getAbsolutePath() : "null"));
            }
        } catch (Exception e) {
            ledger.record(iterationId, SelfDevIterationLedger.Stage.RUN_PREPARED, "FAILED", null, e.getMessage());
        }

        String submissionStatus = "NOT_SUBMITTED";
        String submissionDetail = null;
        try {
            eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl service =
                    eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl.getInstance();
            if (service != null && sessionId != null && !sessionId.trim().isEmpty()) {
                eu.kalafatic.evolution.controller.orchestration.TaskRequest request =
                        new eu.kalafatic.evolution.controller.orchestration.TaskRequest(objective,
                                context.getPreparedReactorDirectory());
                service.submit(sessionId, request);
                submissionStatus = "SUBMITTED_VIA_DARWIN_CHAIN";
                ledger.record(iterationId, SelfDevIterationLedger.Stage.SOURCE_MODIFICATION, "IN_PROGRESS",
                        null, "Objective submitted to existing Darwin orchestration");
            } else {
                submissionDetail = "No active Darwin session available; objective recorded in the ledger only.";
                ledger.record(iterationId, SelfDevIterationLedger.Stage.ANALYSIS, "COMPLETED", null, submissionDetail);
            }
        } catch (Exception e) {
            submissionStatus = "SUBMISSION_ERROR";
            submissionDetail = e.getMessage();
            ledger.record(iterationId, SelfDevIterationLedger.Stage.ANALYSIS, "FAILED", null, submissionDetail);
        }

        org.json.JSONObject json = new org.json.JSONObject();
        json.put("runId", context.getRunId());
        json.put("iterationId", iterationId);
        json.put("submissionStatus", submissionStatus);
        json.put("submissionDetail", submissionDetail);
        json.put("ledgerFile", ledger.getLedgerFile() != null ? ledger.getLedgerFile().getAbsolutePath() : null);
        return json;
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
