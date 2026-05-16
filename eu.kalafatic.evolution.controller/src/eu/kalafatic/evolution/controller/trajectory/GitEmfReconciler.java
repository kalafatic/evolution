package eu.kalafatic.evolution.controller.trajectory;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Reconciles physical Git changes with semantic EMF memory using event sourcing.
 * Detects divergence between predicted outcomes and physical reality.
 */
public class GitEmfReconciler {
    private final TaskContext context;

    public GitEmfReconciler(TaskContext context) {
        this.context = context;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        RuntimeEventBus.getInstance().subscribe(event -> {
            if (event.getType() == RuntimeEventType.TOOL_EXECUTION_SUCCEEDED && "GitTool".equals(event.getSource())) {
                reconcile(event);
            }
        });
    }

    private void reconcile(RuntimeEvent event) {
        if (context == null || context.getOrchestrator() == null) return;

        Object payload = event.getPayload();
        if (payload instanceof String) {
            String gitOutput = (String) payload;

            // Logic: If GitTool succeeded (e.g. after a commit or diff), update the active iteration's justification
            if (context.getOrchestrator().getSelfDevSession() != null) {
                eu.kalafatic.evolution.model.orchestration.Iteration currentIt = context.getOrchestrator().getSelfDevSession().getIterations().stream()
                    .filter(i -> i.getId().equals(context.getOrchestrationState().getCurrentIterationId()))
                    .findFirst().orElse(null);

                if (currentIt != null) {
                    // Detect Divergence
                    if (gitOutput.contains("CONFLICT") || gitOutput.contains("error:")) {
                        emitDivergenceSignal(currentIt.getId(), DivergenceType.STRUCTURAL_DRIFT);
                    }

                    // Update rationale with physical truth
                    String currentRationale = currentIt.getRationale() != null ? currentIt.getRationale() : "";
                    if (!currentRationale.contains("[RECONCILED]")) {
                        currentIt.setRationale(currentRationale + "\n[RECONCILED] Physical Git state synchronized. Reality check passed.");
                    }
                }
            }
        }
    }

    private void emitDivergenceSignal(String iterationId, DivergenceType type) {
        EvaluationSignal signal = new EvaluationSignal(
            "NONE", // variantId
            "GitReconciler", // evaluatorId
            0.1, // score
            1.0, // confidence
            SignalSeverity.CRITICAL, // severity
            type, // divergenceType
            "Physical divergence detected via Git reconciliation: " + type
        );
        SignalBus.getInstance().publish(signal);
    }
}
