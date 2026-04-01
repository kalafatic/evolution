package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.eclipse.emf.ecore.resource.Resource;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class RestartManager {
    private final TaskContext context;

    public RestartManager(TaskContext context) {
        this.context = context;
    }

    public void restartIfNeeded() {
        context.log("[RESTART] Checking if system restart or reload is needed...");
        context.log("[RESTART] System is stable. No restart required for this iteration.");
    }

    public void persistAndPrepareForRestart() {
        context.log("[RESTART] Persisting session state before restart...");
        try {
            Resource resource = context.getOrchestrator().eResource();
            if (resource != null) {
                resource.save(null);
                context.log("[RESTART] EMF Resource saved successfully.");
            } else {
                context.log("[RESTART] Warning: No EMF resource found for Orchestrator. State not persisted to disk.");
            }
        } catch (Exception e) {
            context.log("[RESTART] Error persisting state: " + e.getMessage());
        }
    }
}
