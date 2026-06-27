package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
    eu.kalafatic.evolution.controller.orchestration.cognitive.SessionCognitiveState getCognitiveState();
    IterationManager getIterationManager();
    void setIterationManager(IterationManager manager);
    RuntimeCoordinator getRuntimeCoordinator();
    eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService getMemoryService(java.io.File projectRoot);
    eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine getPressureEngine();
    eu.kalafatic.evolution.controller.workflow.EvolutionaryObservabilityManager getObservabilityManager();
    eu.kalafatic.evolution.controller.workflow.RuntimeContextCollector getContextCollector();

    void shutdown();
}
