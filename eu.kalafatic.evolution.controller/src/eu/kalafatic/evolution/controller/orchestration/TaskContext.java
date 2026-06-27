package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.kernel.EvolutionProfile;
import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevProtocol;
import eu.kalafatic.evolution.controller.orchestration.workspace.SemanticWorkspace;
import eu.kalafatic.evolution.controller.trajectory.SignalBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Shared context for an orchestration task, including project info, state, and logs.
 */
public class TaskContext {
    public static final String PLAN_APPROVAL_MESSAGE = "Plan review required. Please verify and modify the task list in the Approval tab.";
    private final Orchestrator orchestrator;
    private final File projectRoot;
    private final SelfDevProtocol protocol;
    private final Map<String, String> state = new ConcurrentHashMap<>();
    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());
    private final List<LogListener> listeners = new CopyOnWriteArrayList<>();
    private final List<ApprovalListener> approvalListeners = new CopyOnWriteArrayList<>();
    private final List<InputListener> inputListeners = new CopyOnWriteArrayList<>();
    private final List<TokenRequestListener> tokenRequestListeners = new CopyOnWriteArrayList<>();
    private final List<String> instructionFiles = new CopyOnWriteArrayList<>();
    private final FileChangeTracker fileChangeTracker = new FileChangeTracker();
    private String currentTaskName = "Orchestration";
    private String sessionId = "Default";
    private String currentTaskId = "Unknown";
    private int currentIteration = 0;
    private String currentPhase = "INIT";
    private final String deterministicExecutionId;
    private Instant startTime;
    private CompletableFuture<Boolean> approvalFuture;
    private CompletableFuture<String> inputFuture;
    private volatile boolean paused = false;
    private volatile Boolean localAutoApprove = null;
    private PlatformMode platformMode = null;
    private eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile behaviorProfile = null;
    private final SystemStateHolder stateHolder = new SystemStateHolder();
    private OrchestrationState orchestrationState;
    private EvolutionKernelContext kernelContext;
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    private final Object pauseLock = new Object();
    private AiService aiService = new AiService();

    public interface LogListener {
        void onLog(String message);
    }

    public interface ApprovalListener {
        void onApprovalRequested(String message);
    }

    public interface InputListener {
        void onInputRequested(String message);
    }

    public interface TokenRequestListener {
        void onTokenRequested(String providerName, CompletableFuture<String> tokenFuture);
    }

    public TaskContext(Orchestrator orchestrator, File projectRoot) {
        this.orchestrator = orchestrator;
        this.projectRoot = projectRoot;
        this.protocol = new SelfDevProtocol(projectRoot);
        this.deterministicExecutionId = UUID.randomUUID().toString();
    }

    public List<String> getInstructionFiles() {
        return instructionFiles;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public String getCurrentTaskName() {
        return currentTaskName;
    }

    public void setCurrentTaskName(String currentTaskName) {
        this.currentTaskName = currentTaskName;
    }

    public String getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(String currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    public int getCurrentIteration() {
        return currentIteration;
    }

    public void setCurrentIteration(int currentIteration) {
        this.currentIteration = currentIteration;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getDeterministicExecutionId() {
        return deterministicExecutionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        if (this.orchestrationState == null || !this.orchestrationState.getSessionId().equals(sessionId)) {
            this.orchestrationState = new OrchestrationState(sessionId);
        }
    }

    public void log(String message) {
        String formatted = "[" + sessionId + "] " + message;
        logs.add(formatted);
        Log.log(formatted);
        notifyListeners(formatted);
    }

    public void consoleLog(String message) {
        Log.log("[" + sessionId + "] " + message);
    }

    public void addLogListener(LogListener listener) {
        listeners.add(listener);
    }

    public void addApprovalListener(ApprovalListener listener) {
        approvalListeners.add(listener);
    }

    public void addInputListener(InputListener listener) {
        inputListeners.add(listener);
    }

    public List<LogListener> getLogListeners() {
        return listeners;
    }

    public List<ApprovalListener> getApprovalListeners() {
        return approvalListeners;
    }

    public List<InputListener> getInputListeners() {
        return inputListeners;
    }

    public void addTokenRequestListener(TokenRequestListener listener) {
        tokenRequestListeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
        approvalListeners.clear();
        inputListeners.clear();
        tokenRequestListeners.clear();
    }

    private void notifyListeners(String message) {
        for (LogListener listener : listeners) {
            listener.onLog(message);
        }
    }

    public CompletableFuture<Boolean> requestApproval(String message) {
        approvalFuture = new CompletableFuture<>();
        for (ApprovalListener listener : approvalListeners) {
            listener.onApprovalRequested(message);
        }
        return approvalFuture;
    }

    public void provideApproval(boolean approved) {
        if (approvalFuture != null && !approvalFuture.isDone()) {
            approvalFuture.complete(approved);
        }
        if (inputFuture != null && !inputFuture.isDone()) {
            provideInput(approved ? "Approved" : "Rejected");
        }
    }

    public CompletableFuture<String> requestInput(String message) {
        inputFuture = new CompletableFuture<>();
        for (InputListener listener : inputListeners) {
            listener.onInputRequested(message);
        }
        return inputFuture;
    }

    public void provideInput(String input) {
        if (inputFuture != null) {
            inputFuture.complete(input);
            // DO NOT NULLIFY: prevent race conditions where requestInput might return null
            // if provideInput is called immediately by a listener on another thread.
        }
    }

    public boolean isWaitingForInput() {
        return inputFuture != null && !inputFuture.isDone();
    }

    public boolean isWaitingForApproval() {
        return approvalFuture != null && !approvalFuture.isDone();
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }

    public boolean isAutoApprove() {
        if (localAutoApprove != null) {
            return localAutoApprove;
        }

        // 1. Check BitState (Most accurate reflection of current policy)
        if (orchestrationState != null && orchestrationState.getBitState() != 0) {
            if (eu.kalafatic.evolution.controller.orchestration.behavior.BitState.isAutoApprove(orchestrationState.getBitState())) {
                return true;
            }
        }

        // 2. Check Session-specific model
        if (orchestrator != null && orchestrator.getAiChat() != null) {
            var session = orchestrator.getAiChat().getSessions().stream()
                    .filter(s -> sessionId.equals(s.getId()))
                    .findFirst().orElse(null);
            if (session != null && session.isAutoApprove()) {
                return true;
            }

            // 3. Fallback to PromptInstructions
            if (orchestrator.getAiChat().getPromptInstructions() != null) {
                return orchestrator.getAiChat().getPromptInstructions().isAutoApprove();
            }
        }
        return false;
    }

    public void setAutoApprove(boolean autoApprove) {
        this.localAutoApprove = autoApprove;
    }

    public void checkPause() {
        boolean externalPause = false;
        JSONObject control = protocol.readControl();
        if (control != null) {
            externalPause = control.optBoolean("pause", false);
        }

        if (paused || externalPause) {
            synchronized (pauseLock) {
                while (paused || externalPause) {
                    try {
                        pauseLock.wait(2000);
                        control = protocol.readControl();
                        if (control != null) {
                            externalPause = control.optBoolean("pause", false);
                        } else {
                            externalPause = false;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    public CompletableFuture<String> requestToken(String providerName) {
        CompletableFuture<String> tokenFuture = new CompletableFuture<>();
        if (tokenRequestListeners.isEmpty()) {
            tokenFuture.completeExceptionally(new Exception("No token request listeners registered."));
        } else {
            for (TokenRequestListener listener : tokenRequestListeners) {
                listener.onTokenRequested(providerName, tokenFuture);
            }
        }
        return tokenFuture;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void putState(String key, String value) {
        state.put(key, value);
    }

    public String getState(String key) {
        return state.get(key);
    }

    public AiService getAiService() {
        return aiService;
    }

    public void setAiService(AiService aiService) {
        this.aiService = aiService;
    }

    public String getSharedMemory() {
        String mem = orchestrator.getSharedMemory();
        return mem != null ? mem : "";
    }

    public void appendSharedMemory(String content) {
        String current = getSharedMemory();
        if (current.isEmpty()) {
            orchestrator.setSharedMemory(content);
        } else {
            orchestrator.setSharedMemory(current + "\n" + content);
        }
    }

    public PlatformMode getPlatformMode() {
        return platformMode;
    }

    public void setPlatformMode(PlatformMode platformMode) {
        this.platformMode = platformMode;
        this.behaviorProfile = null;
    }

    public eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile getBehaviorProfile() {
        if (behaviorProfile == null) {
            eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorResolver resolver = new eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorResolver();
            behaviorProfile = resolver.resolve(this);

            // BREAK RECURSION: Use direct field access or lazy state init without triggering profile refresh again.
            if (orchestrationState == null) {
                orchestrationState = new OrchestrationState(sessionId);
            }

            long resolvedBitState = resolver.resolveBitState(this);
            if (orchestrationState.getBitState() != resolvedBitState) {
                orchestrationState.setBitState(resolvedBitState);
                log("[KERNEL] Policy state synchronized: " + resolvedBitState);
            }
        }
        return behaviorProfile;
    }

    public SystemStateHolder getStateHolder() {
        return stateHolder;
    }

    public OrchestrationState getOrchestrationState() {
        if (orchestrationState == null) {
            orchestrationState = new OrchestrationState(sessionId);
        }
        return orchestrationState;
    }

    public EvolutionProfile getExecutionProfile() {
        return getOrchestrationState().getExecutionProfile();
    }

    public EvolutionKernelContext getKernelContext() {
        if (kernelContext == null) {
            Log.log("[CONTEXT] Initializing new EvolutionKernelContext for session: " + sessionId);
            SessionContainer session = SessionManager.getInstance().getSession(sessionId);
            if (session != null) {
                kernelContext = new EvolutionKernelContext(projectRoot, session.getEventBus(), session.getSignalBus(), session.getMemoryService(projectRoot));
            } else {
                // Fallback for tests or situations where session is not yet registered in SessionManager
                RuntimeEventBus bus = new RuntimeEventBus(sessionId);
                kernelContext = new EvolutionKernelContext(projectRoot, bus, new SignalBus(bus), new eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService(projectRoot));
            }
        }
        return kernelContext;
    }

    public void setKernelContext(EvolutionKernelContext kernelContext) {
        this.kernelContext = kernelContext;
    }

    public SemanticWorkspace getSemanticWorkspace() {
        return getOrchestrationState().getSemanticWorkspace();
    }

    public FileChangeTracker getFileChangeTracker() {
        return fileChangeTracker;
    }

    public SelfDevProtocol getProtocol() {
        return protocol;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
