package eu.kalafatic.evolution.controller.orchestration.selfdev;

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
    private static final int MAX_FAILURES = 3;

    public SelfDevSupervisor(SelfDevSession session, TaskContext context) {
        this.session = session;
        this.context = context;
    }

    public void startSession() {
        context.log("[SUPERVISOR] Starting Self-Development Session: " + session.getId());
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

                IterationManager iterationManager = new IterationManager(iteration, context);
                EvaluationResult result = iterationManager.run();

                if (!result.isSuccess()) {
                    failureCount++;
                    context.log("[SUPERVISOR] Iteration " + i + " failed. Total failures: " + failureCount);
                    if (failureCount >= MAX_FAILURES) {
                        context.log("[SUPERVISOR] Max failures (" + MAX_FAILURES + ") reached. Stopping session.");
                        session.setStatus(SelfDevStatus.FAILED);
                        break;
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

    public void stopSession() {
        context.log("[SUPERVISOR] Stopping Session: " + session.getId());
        session.setStatus(SelfDevStatus.STOPPED);
    }
}
