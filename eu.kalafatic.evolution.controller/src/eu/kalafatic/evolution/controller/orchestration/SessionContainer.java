package eu.kalafatic.evolution.controller.orchestration;

import java.util.concurrent.ExecutorService;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionMemoryGraph;
import eu.kalafatic.evolution.controller.trajectory.SignalBus;
import eu.kalafatic.evolution.controller.trajectory.EvolutionRegistry;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.WorkflowStepRegistry;

/**
 * Interface for isolated session containers.
 */
public interface SessionContainer {
    String getSessionId();
    RuntimeEventBus getEventBus();
    SignalBus getSignalBus();
    WorkflowStepRegistry getWorkflowRegistry();
    EvolutionRegistry getEvolutionRegistry();
    CapabilityRegistry getCapabilityRegistry();
    ExecutorService getExecutorService();
    EvolutionMemoryGraph getEvolutionMemoryGraph();
    FileChangeTracker getFileChangeTracker();
    OrchestrationState getSessionState();
    RuntimeCoordinator getRuntimeCoordinator();

    void shutdown();
}
