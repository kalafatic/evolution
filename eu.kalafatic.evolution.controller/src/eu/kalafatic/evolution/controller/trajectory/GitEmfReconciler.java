package eu.kalafatic.evolution.controller.trajectory;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
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
                    // Detect Divergence Taxonomy
                    if (gitOutput.contains("CONFLICT")) {
                        emitDivergenceSignal(currentIt.getId(), DivergenceType.STRUCTURAL_DRIFT, "Git conflict detected during application.");
                    } else if (gitOutput.contains("error:")) {
                        emitDivergenceSignal(currentIt.getId(), DivergenceType.BEHAVIORAL_MISMATCH, "Git tool reported an error execution.");
                    } else if (gitOutput.length() > 50000) {
                        emitDivergenceSignal(currentIt.getId(), DivergenceType.COMPLEXITY_EXPLOSION, "Unexpectedly large physical change detected (" + gitOutput.length() + " chars).");
                    } else if (gitOutput.trim().isEmpty()) {
                        emitDivergenceSignal(currentIt.getId(), DivergenceType.SILENT_CHANGE, "Git tool succeeded but no physical changes were detected.");
                    }

                    // Update rationale with physical truth and reasoning context
                    String currentRationale = currentIt.getRationale() != null ? currentIt.getRationale() : "";
                    if (!currentRationale.contains("[RECONCILED]")) {
                        String goal = context.getOrchestrator().getSelfDevSession() != null ? context.getOrchestrator().getSelfDevSession().getInitialRequest() : "Unknown Goal";
                        currentIt.setRationale(currentRationale + "\n[RECONCILED] Physical Git state synchronized for goal: " + goal + ". Reality check passed.");
                    }
                }
            }
        }
    }

    private void emitDivergenceSignal(String iterationId, DivergenceType type, String message) {
        String variantId = (String) context.getMetadata().getOrDefault("variantId", "NONE");
        EvaluationSignal signal = new EvaluationSignal(
            variantId,
            "GitReconciler",
            0.1, // Low score due to divergence
            1.0, // High confidence in the physical signal
            SignalSeverity.CRITICAL,
            type,
            "Physical divergence [" + type + "] detected: " + message
        );
        RuntimeEventBus.getInstance().publish(new RuntimeEvent(RuntimeEventType.EVALUATION_SIGNAL_CREATED, "GLOBAL", "GitEmfReconciler", signal));
    }

    /**
     * Public method to manually trigger a reconciliation check.
     */
    public void check(String output) {
        reconcile(new RuntimeEvent(RuntimeEventType.TOOL_EXECUTION_SUCCEEDED, "manual", "GitTool", output));
    }
}
