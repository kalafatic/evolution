package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.KernelFactory;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;

/**
 * Supervisor for autonomous self-development sessions.
 * Pure session coordinator. All strategic reasoning delegated to the Kernel.
 *
 * @evo:21:A reason=kernel-refactor-alignment
 */
public class SelfDevSupervisor {
    private final SelfDevSession session;
    private final TaskContext context;
    private static final int MAX_FAILURES = 3;
    private final SelfDevBootstrapController bootstrapController;

    public SelfDevSupervisor(SelfDevSession session, TaskContext context) {
        this.session = session;
        this.context = context;
        this.bootstrapController = new SelfDevBootstrapController(context.getProjectRoot(), context.getOrchestrator());
    }

    public void startSession() {
        BehaviorProfile profile = context.getBehaviorProfile();

        // Mode routing is now handled by IterationManager.handle()
        String modeName = (context.getPlatformMode() != null) ? context.getPlatformMode().getType().toString().replace("_", " ") : "Self-Development";
        context.log("[SUPERVISOR] Starting " + modeName + " Session: " + session.getId());

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
            session.setMaxIterations(maxIter);

            for (int i = 1; i <= maxIter; i++) {
                if (session.getStatus() != SelfDevStatus.RUNNING) {
                    context.log("[SUPERVISOR] Session status changed. Stopping.");
                    break;
                }

                context.log("[SUPERVISOR] Starting Iteration " + i + " of " + maxIter);

                context.getKernelContext().getEventBus().publish(
                    new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                        eu.kalafatic.evolution.controller.workflow.RuntimeEventType.ITERATION_STARTED,
                        context.getSessionId(), "Supervisor", "iteration-" + i));

                Iteration iteration = OrchestrationFactory.eINSTANCE.createIteration();
                iteration.setId("iteration-" + i);
                iteration.setBranchName("selfdev/" + session.getId() + "/" + iteration.getId());
                session.getIterations().add(iteration);

                // Authority Hierarchy: Supervisor delegates ALL progress authority to the Kernel (IterationManager).
                IterationManager kernel = createIterationManager(iteration, context.getAiService());

                // KERNEL AUTHORITY: IterationManager is the sole authority for state transitions.
                // It will manage the 'running' iteration and decide when to stop.
                EvaluationResult result = kernel.runIteration(iteration);

                if (!result.isSuccess() || result.getDecision() == SelfDevDecision.ROLLBACK) {
                    failureCount++;

                    context.getKernelContext().getEventBus().publish(
                        new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                            eu.kalafatic.evolution.controller.workflow.RuntimeEventType.TASK_FAILED,
                            context.getSessionId(), "Supervisor", "Iteration failed: " + i)
                            .withMetadata("iteration", i)
                            .withMetadata("decision", result.getDecision().toString()));

                    context.log("[SUPERVISOR] Iteration " + i + " failed. Total failures: " + failureCount);
                    if (failureCount >= MAX_FAILURES) {
                        context.log("[SUPERVISOR] Max failures (" + MAX_FAILURES + ") reached. Stopping session.");
                        session.setStatus(SelfDevStatus.FAILED);
                        return;
                    }
                }

                if (result.getDecision() == SelfDevDecision.STOP) {
                    context.log("[SUPERVISOR] STOP decision reached. Terminating session.");
                    session.setStatus(SelfDevStatus.COMPLETED);
                    break;
                }

                context.log("[SUPERVISOR] Iteration " + i + " logic complete. Preparing for potential restart.");
                restartManager.persistAndPrepareForRestart();

                if (profile.hasTrait(BehaviorTrait.WORKFLOW_SELF_DEV)) {
                    context.log("[SUPERVISOR] Delegating build and restart to external Supervisor...");
                    try {
                        // RESTART CONTINUITY: Final persistence check
                        restartManager.persistAndPrepareForRestart();
                        bootstrapController.startBootstrap();
                        Thread.sleep(2000);
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

            context.getKernelContext().getEventBus().publish(
                new eu.kalafatic.evolution.controller.workflow.RuntimeEvent(
                    eu.kalafatic.evolution.controller.workflow.RuntimeEventType.FLOW_COMPLETED,
                    context.getSessionId(), "Supervisor", session.getStatus().toString()));
            context.log("[SUPERVISOR] Session completed. Status: " + session.getStatus());

        } catch (Exception e) {
            context.log("[SUPERVISOR] Critical failure in session: " + e.getMessage());
            session.setStatus(SelfDevStatus.FAILED);
        }
    }

    protected IterationManager createIterationManager(Iteration iteration, eu.kalafatic.evolution.controller.orchestration.AiService aiService) {
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(context.getSessionId());
        return KernelFactory.create(context, session, aiService);
    }

    public void stopSession() {
        context.log("[SUPERVISOR] Stopping Session: " + session.getId());
        session.setStatus(SelfDevStatus.STOPPED);
    }
}
