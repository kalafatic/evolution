package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.LlmIntentClassifier;
import eu.kalafatic.evolution.controller.agents.AnalyticAgent;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;

/**
 * Supervisor for autonomous self-development sessions.
 *
 * @evo:20:A reason=architecture-documentation-sync
 * @evo:22:A reason=external-supervisor-handoff
 */
public class SelfDevSupervisor {
    private final SelfDevSession session;
    private final TaskContext context;
    protected final AnalyticAgent analyticAgent = new AnalyticAgent();
    protected final LlmIntentClassifier intentClassifier = new LlmIntentClassifier();
    private static final int MAX_FAILURES = 3;
    private final SelfDevBootstrapController bootstrapController;

    public SelfDevSupervisor(SelfDevSession session, TaskContext context) {
        this.session = session;
        this.context = context;
        this.bootstrapController = new SelfDevBootstrapController(context.getProjectRoot(), context.getOrchestrator());
    }

    public void startSession() {
        if (context.getPlatformMode() == null) {
            context.setPlatformMode(new ModeRouter().route(session.getInitialRequest(), context.getOrchestrator()));
        }
        String modeName = (context.getPlatformMode() != null) ? context.getPlatformMode().getType().toString().replace("_", " ") : "Self-Development";
        context.log("[SUPERVISOR] Starting " + modeName + " Session: " + session.getId());

        // Analytic Phase for the initial request
        try {
            String initialRequest = session.getInitialRequest();
            if (initialRequest != null && !initialRequest.isEmpty()) {
                // Intent Gate first
                JSONObject classification;
                try {
                    classification = analyzeIntent(initialRequest);
                } catch (Exception e) {
                    context.log("[SUPERVISOR] Intent Gate error: " + e.getMessage());
                    classification = new JSONObject().put("intent", "new");
                }

                if ("chat".equals(classification.optString("intent"))) {
                    context.log("[SUPERVISOR] Request classified as chat. Stopping session.");
                    session.setStatus(SelfDevStatus.COMPLETED);
                    return;
                }

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
            int maxIter = session.getMaxIterations();
            if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null &&
                context.getOrchestrator().getAiChat().getPromptInstructions() != null) {
                int preferred = context.getOrchestrator().getAiChat().getPromptInstructions().getPreferredMaxIterations();
                if (preferred > 0) {
                    maxIter = preferred;
                }
            }
            session.setMaxIterations(maxIter); // Ensure session model reflects what we are actually using

            for (int i = 1; i <= maxIter; i++) {
                if (session.getStatus() != SelfDevStatus.RUNNING) {
                    context.log("[SUPERVISOR] Session status changed. Stopping.");
                    break;
                }

                context.log("[SUPERVISOR] Starting Iteration " + i + " of " + maxIter);
                Iteration iteration = OrchestrationFactory.eINSTANCE.createIteration();
                iteration.setId("iteration-" + i);
                iteration.setBranchName("selfdev/" + session.getId() + "/" + iteration.getId());
                session.getIterations().add(iteration);

                IterationManager iterationManager = createIterationManager(iteration);
                EvaluationResult result;
                try {
                    result = iterationManager.run();
                } catch (Exception e) {
                    context.log("[SUPERVISOR] Iteration " + i + " encountered an error: " + e.getMessage());
                    result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
                    result.setSuccess(false);
                    result.setDecision(SelfDevDecision.ROLLBACK);
                }

                if (!result.isSuccess() || result.getDecision() == SelfDevDecision.ROLLBACK) {
                    failureCount++;
                    context.log("[SUPERVISOR] Iteration " + i + " failed. Total failures: " + failureCount);
                    if (failureCount >= MAX_FAILURES) {
                        context.log("[SUPERVISOR] Max failures (" + MAX_FAILURES + ") reached. Stopping session.");
                        session.setStatus(SelfDevStatus.FAILED);
                        return; // Exit immediately
                    }
                    // If we have a critical error but haven't reached MAX_FAILURES, we might want to continue to next iteration
                    // But usually, a critical error in Darwin mode might need manual intervention or a different approach.
                }

                if (result.getDecision() == SelfDevDecision.STOP) {
                    context.log("[SUPERVISOR] STOP decision reached. Terminating session.");
                    session.setStatus(SelfDevStatus.COMPLETED);
                    break;
                }

                restartManager.persistAndPrepareForRestart();

                // If we need a real restart via external Supervisor
                if (context.getPlatformMode() != null && context.getPlatformMode().getType() == PlatformType.SELF_DEV_MODE) {
                    context.log("[SUPERVISOR] Delegating build and restart to external Supervisor...");
                    try {
                        bootstrapController.startBootstrap();

                        // Small wait to allow external supervisor to take over
                        Thread.sleep(2000);

                        // In external mode, the external supervisor takes over the iteration loop.
                        // This internal loop can stop or wait.
                        session.setStatus(SelfDevStatus.COMPLETED);
                        context.log("[SUPERVISOR] Handoff complete. Stopping internal supervisor.");
                        break;
                    } catch (Exception e) {
                        context.log("[SUPERVISOR] Bootstrap failed: " + e.getMessage());
                    }
                } else {
                    restartManager.restartIfNeeded();
                }
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

    protected JSONObject analyzeIntent(String request) throws Exception {
        return intentClassifier.classify(request, context);
    }

    private String analyzeAndClarify(String request) throws Exception {
        OrchestrationStatusManager.getInstance().updateAgentStatus("Analytic", "Analyzing request...");
        try {
            JSONObject analysis = analyticAgent.analyze(request, context);
            context.log("[SUPERVISOR] Analytic Agent identified category: " + analysis.optString("category"));

            if (analysis.optBoolean("isAmbiguous", false)) {
                String question = analysis.optString("clarificationQuestion", "The request is ambiguous. Can you please provide more details?");

                // If Darwin mode is enabled, we bypass blocking clarification and let the DarwinEngine handle ambiguity via variants.
                if (context.getOrchestrator().isDarwinMode()) {
                    context.log("[SUPERVISOR] Request identified as ambiguous, but Darwin mode is active. Bypassing clarification to generate variants.");
                    return analysis.optString("refinedPrompt", request);
                }

                context.log("[SUPERVISOR] Request is ambiguous. Asking for clarification...");

                if (context.isAutoApprove()) {
                    context.log("Evo-Supervisor: Auto-approval enabled. Skipping clarification in headless mode.");
                    return request;
                }

                context.log("Evo-Supervisor-Waiting: Waiting for user clarification...");
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
