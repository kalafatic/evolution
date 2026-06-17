package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.execution.BackpressureController;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionMemoryGraph;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.trajectory.EvolutionRegistry;
import eu.kalafatic.evolution.controller.trajectory.SignalBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.StepModeController;
import eu.kalafatic.evolution.controller.workflow.WorkflowGraphManager;
import eu.kalafatic.evolution.controller.workflow.WorkflowStepRegistry;

/**
 * Encapsulates all session-specific runtime state and isolated services.
 * Ensures true parallel multi-session execution without state corruption.
 */
public class SessionContext implements SessionContainer {
    private final String sessionId;
    private final ExecutorService executorService;
    private final CapabilityRegistry capabilityRegistry;
    private final StepModeController stepModeController;
    private final RuntimeEventBus eventBus;
    private final SignalBus signalBus;
    private final WorkflowStepRegistry workflowRegistry;
    private final WorkflowGraphManager workflowGraphManager;
    private final EvolutionRegistry evolutionRegistry;
    private final EvolutionMemoryGraph evolutionMemoryGraph;
    private final FileChangeTracker fileChangeTracker;
    private final SelectionState selectionState;
    private final eu.kalafatic.evolution.controller.orchestration.cognitive.SessionCognitiveState cognitiveState;
    private final BackpressureController backpressureController;
    private final OrchestrationStatusManager statusManager;
    private final RuntimeCoordinator runtimeCoordinator;
    private final eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine pressureEngine;
    private final eu.kalafatic.evolution.controller.workflow.EvolutionaryObservabilityManager observabilityManager;
    private final eu.kalafatic.evolution.controller.workflow.RuntimeContextCollector contextCollector;

    private final Map<String, IAgent> agentRegistry = new ConcurrentHashMap<>();
    private IterationMemoryService memoryService;
    private IterationManager iterationManager;
    private TaskContext taskContext;

    public SessionContext(String sessionId) {
        this.sessionId = sessionId;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Session-" + sessionId);
            t.setDaemon(true);
            return t;
        });

        this.eventBus = new RuntimeEventBus(sessionId);
        this.signalBus = new SignalBus(this.eventBus);
        this.workflowRegistry = new WorkflowStepRegistry();
        this.workflowGraphManager = new WorkflowGraphManager(this.eventBus);
        this.evolutionRegistry = new EvolutionRegistry();
        this.capabilityRegistry = new CapabilityRegistry();
        this.stepModeController = new StepModeController(this.eventBus, this.workflowRegistry);
        this.evolutionMemoryGraph = new EvolutionMemoryGraph();
        this.fileChangeTracker = new FileChangeTracker();
        this.selectionState = new SelectionState();
        this.cognitiveState = new eu.kalafatic.evolution.controller.orchestration.cognitive.SessionCognitiveState();
        this.backpressureController = new BackpressureController();
        this.statusManager = new OrchestrationStatusManager();
        this.runtimeCoordinator = new RuntimeCoordinator(sessionId, this.eventBus, this.signalBus, this.workflowRegistry);
        this.pressureEngine = new eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine();
        this.observabilityManager = new eu.kalafatic.evolution.controller.workflow.EvolutionaryObservabilityManager(sessionId);
        this.contextCollector = new eu.kalafatic.evolution.controller.workflow.RuntimeContextCollector(sessionId);
        this.eventBus.subscribe(this.observabilityManager);
        this.eventBus.subscribe(this.contextCollector);

        this.runtimeCoordinator.initialize();

        // Register session-scoped services in capability registry
        try {
            this.capabilityRegistry.register(this.backpressureController);
        } catch (Exception e) {
            // Log if needed
        }

        // Ensure UI projections are initialized for this session
        try {
            Class<?> projectionServiceClass = Class.forName("eu.kalafatic.evolution.view.projection.ProjectionService");
            java.lang.reflect.Method getInstance = projectionServiceClass.getMethod("getInstance");
            Object projectionService = getInstance.invoke(null);
            java.lang.reflect.Method initMethod = projectionServiceClass.getMethod("initializeForSession", RuntimeEventBus.class);
            initMethod.invoke(projectionService, this.eventBus);
        } catch (Exception e) {
            // View bundle might not be present in all environments (e.g. headless tests)
        }
    }

    @Override
    public eu.kalafatic.evolution.controller.orchestration.cognitive.SessionCognitiveState getCognitiveState() {
        return cognitiveState;
    }

    @Override
    public BackpressureController getBackpressureController() {
        return backpressureController;
    }

    @Override
    public OrchestrationStatusManager getStatusManager() {
        return statusManager;
    }

    @Override
    public SelectionState getSelectionState() {
        return selectionState;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public CapabilityRegistry getCapabilityRegistry() {
        return capabilityRegistry;
    }

    public StepModeController getStepModeController() {
        return stepModeController;
    }

    @Override
    public RuntimeEventBus getEventBus() {
        return eventBus;
    }

    @Override
    public SignalBus getSignalBus() {
        return signalBus;
    }

    @Override
    public WorkflowStepRegistry getWorkflowRegistry() {
        return workflowRegistry;
    }

    @Override
    public WorkflowGraphManager getWorkflowGraphManager() {
        return workflowGraphManager;
    }

    @Override
    public EvolutionRegistry getEvolutionRegistry() {
        return evolutionRegistry;
    }

    @Override
    public EvolutionMemoryGraph getEvolutionMemoryGraph() {
        if (memoryService != null) {
            return memoryService.getEvolutionGraph();
        }
        return evolutionMemoryGraph;
    }

    @Override
    public FileChangeTracker getFileChangeTracker() {
        return fileChangeTracker;
    }

    @Override
    public OrchestrationState getSessionState() {
        return taskContext != null ? taskContext.getOrchestrationState() : null;
    }

    @Override
    public IterationManager getIterationManager() {
        return iterationManager;
    }

    @Override
    public void setIterationManager(IterationManager manager) {
        this.iterationManager = manager;
    }

    @Override
    public RuntimeCoordinator getRuntimeCoordinator() {
        return runtimeCoordinator;
    }

    @Override
    public Map<String, IAgent> getAgentRegistry() {
        return agentRegistry;
    }

    @Override
    public eu.kalafatic.evolution.controller.kernel.EvolutionaryPressureEngine getPressureEngine() {
        return pressureEngine;
    }

    @Override
    public eu.kalafatic.evolution.controller.workflow.EvolutionaryObservabilityManager getObservabilityManager() {
        return observabilityManager;
    }

    @Override
    public eu.kalafatic.evolution.controller.workflow.RuntimeContextCollector getContextCollector() {
        return contextCollector;
    }

    public synchronized IterationMemoryService getMemoryService(File projectRoot) {
        if (memoryService == null) {
            memoryService = new IterationMemoryService(projectRoot);
        }
        return memoryService;
    }

    public TaskContext getTaskContext() {
        return taskContext;
    }

    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }

    @Override
    public void shutdown() {
        executorService.shutdownNow();
        eventBus.shutdown();
        capabilityRegistry.shutdown();
    }
}
