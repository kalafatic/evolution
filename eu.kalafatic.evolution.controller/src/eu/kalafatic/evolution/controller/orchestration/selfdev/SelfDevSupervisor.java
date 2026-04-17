package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;

public class SelfDevSupervisor {
    private final SelfDevSession session;
    private final TaskContext context;
    private final AnalyticAgent analyticAgent = new AnalyticAgent();
    private static final int MAX_FAILURES = 3;

    public SelfDevSupervisor(SelfDevSession session, TaskContext context) {
        this.session = session;
        this.context = context;
    }

    public void startSession() {
        context.log("[SUPERVISOR] Starting Self-Development Session: " + session.getId());

        // Analytic Phase for the initial request
        try {
            String initialRequest = session.getInitialRequest();
            if (initialRequest != null && !initialRequest.isEmpty()) {
                String refinedRequest = analyzeAndClarify(initialRequest);
                session.setInitialRequest(refinedRequest);
            }
        } catch (Exception e) {
            context.log("[SUPERVISOR] Analytic Phase warning: " + e.getMessage());
        }

        session.setStatus(SelfDevStatus.RUNNING);
        session.setStartTime(System.currentTimeMillis());

        int failureCount = 0;
        RestartManager restartManager = new RestartManager(context);

        try {
            for (int i = 1; i <= session.getMaxIterations(); i++) {
                if (session.getStatus() != SelfDevStatus.RUNNING) {
                    context.log("[SUPERVISOR] Session status changed. Stopping.");
                    break;
                }

                context.log("[SUPERVISOR] Starting Iteration " + i + " of " + session.getMaxIterations());
                Iteration iteration = OrchestrationFactory.eINSTANCE.createIteration();
                iteration.setId("iteration-" + i);
                iteration.setBranchName("selfdev/" + session.getId() + "/" + iteration.getId());
                session.getIterations().add(iteration);

                IterationManager iterationManager = createIterationManager(iteration);
                EvaluationResult result = iterationManager.run();

                if (!result.isSuccess() || result.getDecision() == SelfDevDecision.ROLLBACK) {
                    failureCount++;
                    context.log("[SUPERVISOR] Iteration " + i + " failed. Total failures: " + failureCount);
                    if (failureCount >= MAX_FAILURES) {
                        context.log("[SUPERVISOR] Max failures (" + MAX_FAILURES + ") reached. Stopping session.");
                        session.setStatus(SelfDevStatus.FAILED);
                        return; // Exit immediately
                    }
                }

                if (result.getDecision() == SelfDevDecision.STOP) {
                    context.log("[SUPERVISOR] STOP decision reached. Terminating session.");
                    session.setStatus(SelfDevStatus.COMPLETED);
                    break;
                }

                restartManager.persistAndPrepareForRestart();
                restartManager.restartIfNeeded();
            }

            if (session.getStatus() == SelfDevStatus.RUNNING) {
                session.setStatus(SelfDevStatus.COMPLETED);
            }
            context.log("[SUPERVISOR] Session completed. Status: " + session.getStatus());

        } catch (Exception e) {
            context.log("[SUPERVISOR] Critical failure in session: " + e.getMessage());
            session.setStatus(SelfDevStatus.FAILED);
        }
    }

    private String analyzeAndClarify(String request) throws Exception {
        OrchestrationStatusManager.getInstance().updateAgentStatus("Analytic", "Analyzing request...");
        try {
            JSONObject analysis = analyticAgent.analyze(request, context);
            context.log("[SUPERVISOR] Analytic Agent identified category: " + analysis.optString("category"));

            if (analysis.optBoolean("isAmbiguous", false)) {
                String question = analysis.optString("clarificationQuestion", "The request is ambiguous. Can you please provide more details?");
                context.log("[SUPERVISOR] Request is ambiguous. Asking for clarification...");

                if (context.isAutoApprove()) {
                    context.log("Evo-Supervisor: Auto-approval enabled. Skipping clarification in headless mode.");
                    return request;
                }

                String clarification = context.requestInput(question).get();
                if (clarification == null || clarification.trim().isEmpty()) {
                    context.log("[SUPERVISOR] No clarification provided. Proceeding with original request.");
                    return request;
                }

                context.log("[SUPERVISOR] Received clarification: " + clarification);
                context.appendSharedMemory("User Clarification: " + clarification);

                // Recursively analyze with the clarification
                return analyzeAndClarify(request + "\nClarification: " + clarification);
            }

            String refined = analysis.optString("refinedPrompt", request);
            if (!refined.equals(request)) {
                context.log("[SUPERVISOR] Analytic Agent refined the prompt.");
            }
            return refined;
        } catch (Exception e) {
            context.log("[SUPERVISOR] Analytic Warning: " + e.getMessage() + ". Proceeding with original request.");
            return request;
        } finally {
            OrchestrationStatusManager.getInstance().updateAgentStatus("Analytic", "Idle");
        }
    }

    protected IterationManager createIterationManager(Iteration iteration) {
        return new IterationManager(iteration, context);
    }

    public void stopSession() {
        context.log("[SUPERVISOR] Stopping Session: " + session.getId());
        session.setStatus(SelfDevStatus.STOPPED);
    }
}
