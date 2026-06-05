package eu.kalafatic.evolution.controller.orchestration;

import java.util.concurrent.ExecutorService;

import eu.kalafatic.evolution.controller.execution.BackpressureController;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionMemoryGraph;
import eu.kalafatic.evolution.controller.trajectory.EvolutionRegistry;
import eu.kalafatic.evolution.controller.trajectory.SignalBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.WorkflowGraphManager;
import eu.kalafatic.evolution.controller.workflow.WorkflowStepRegistry;

/**
 * Interface for isolated session containers.
 */
public interface SessionContainer {
    String getSessionId();
    RuntimeEventBus getEventBus();
    SignalBus getSignalBus();
    WorkflowStepRegistry getWorkflowRegistry();
    WorkflowGraphManager getWorkflowGraphManager();
    EvolutionRegistry getEvolutionRegistry();
    CapabilityRegistry getCapabilityRegistry();
    ExecutorService getExecutorService();
    EvolutionMemoryGraph getEvolutionMemoryGraph();
    FileChangeTracker getFileChangeTracker();
    SelectionState getSelectionState();
    BackpressureController getBackpressureController();
    OrchestrationStatusManager getStatusManager();
    java.util.Map<String, eu.kalafatic.evolution.controller.agents.IAgent> getAgentRegistry();
    OrchestrationState getSessionState();
    IterationManager getIterationManager();
    void setIterationManager(IterationManager manager);
    RuntimeCoordinator getRuntimeCoordinator();
    eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService getMemoryService(java.io.File projectRoot);
    eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine getPressureEngine();

    void shutdown();
}
