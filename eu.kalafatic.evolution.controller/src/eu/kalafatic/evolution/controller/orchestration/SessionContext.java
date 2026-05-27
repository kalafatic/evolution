package eu.kalafatic.evolution.controller.orchestration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import eu.kalafatic.evolution.controller.agents.IAgent;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityRegistry;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.StepModeController;

/**
 * Encapsulates all session-specific runtime state and isolated services.
 * Ensures true parallel multi-session execution without state corruption.
 */
public class SessionContext {
    private final String sessionId;
    private final ExecutorService executorService;
    private final CapabilityRegistry capabilityRegistry;
    private final StepModeController stepModeController;
    private final RuntimeEventBus eventBus;
    private final Map<String, IAgent> agentRegistry = new ConcurrentHashMap<>();
    private IterationMemoryService memoryService;
    private TaskContext taskContext;

    public SessionContext(String sessionId) {
        this.sessionId = sessionId;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Session-" + sessionId);
            t.setDaemon(true);
            return t;
        });
        // These will be refactored to allow instantiation
        this.capabilityRegistry = new CapabilityRegistry();
        this.stepModeController = new StepModeController();
        this.eventBus = new RuntimeEventBus();
    }

    public String getSessionId() {
        return sessionId;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public CapabilityRegistry getCapabilityRegistry() {
        return capabilityRegistry;
    }

    public StepModeController getStepModeController() {
        return stepModeController;
    }

    public RuntimeEventBus getEventBus() {
        return eventBus;
    }

    public Map<String, IAgent> getAgentRegistry() {
        return agentRegistry;
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

    public void shutdown() {
        executorService.shutdownNow();
        capabilityRegistry.shutdown();
    }
}
