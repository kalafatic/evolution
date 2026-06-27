package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.trajectory.SignalBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.WorkflowStepRegistry;

/**
 * Manages high-level coordination between session-scoped buses and registries.
 */
public class RuntimeCoordinator {
    private final String sessionId;
    private final RuntimeEventBus eventBus;
    private final SignalBus signalBus;
    private final WorkflowStepRegistry workflowRegistry;

    public RuntimeCoordinator(String sessionId, RuntimeEventBus eventBus, SignalBus signalBus, WorkflowStepRegistry workflowRegistry) {
        this.sessionId = sessionId;
        this.eventBus = eventBus;
        this.signalBus = signalBus;
        this.workflowRegistry = workflowRegistry;
    }

    public void initialize() {
        // Wiring logic between buses if needed
    }

    public String getSessionId() {
        return sessionId;
    }
}
