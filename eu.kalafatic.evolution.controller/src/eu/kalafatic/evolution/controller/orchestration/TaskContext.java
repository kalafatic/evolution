package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.utils.log.Log;

/**
 * Shared context for an orchestration task, including project info, state, and logs.
 */
public class TaskContext {
    public static final String PLAN_APPROVAL_MESSAGE = "Plan review required. Please verify and modify the task list in the Approval tab.";
    private final Orchestrator orchestrator;
    private final File projectRoot;
    private final Map<String, String> state = new ConcurrentHashMap<>();
    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());
    private final List<LogListener> listeners = new CopyOnWriteArrayList<>();
    private final List<ApprovalListener> approvalListeners = new CopyOnWriteArrayList<>();
    private final List<InputListener> inputListeners = new CopyOnWriteArrayList<>();
    private final List<TokenRequestListener> tokenRequestListeners = new CopyOnWriteArrayList<>();
    private final List<String> instructionFiles = new CopyOnWriteArrayList<>();
    private String currentTaskName = "Orchestration";
    private String threadId = "Default";
    private String currentTaskId = "Unknown";
    private int currentIteration = 0;
    private String currentPhase = "INIT";
    private CompletableFuture<Boolean> approvalFuture;
    private CompletableFuture<String> inputFuture;
    private volatile boolean paused = false;
    private volatile boolean autoApprove = false;
    private PlatformMode platformMode = null;
    private final Object pauseLock = new Object();

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

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public void log(String message) {
        logs.add(message);
        Log.log(message);
        notifyListeners(message);
    }

    /**
     * Logs only to the console and file, without notifying UI listeners (chat history).
     */
    public void consoleLog(String message) {
        Log.log(message);
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

    public void addTokenRequestListener(TokenRequestListener listener) {
        tokenRequestListeners.add(listener);
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
            inputFuture = null;
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
        if (orchestrator != null && orchestrator.getAiChat() != null &&orchestrator.getAiChat().getPromptInstructions() != null) {
        	
            return orchestrator.getAiChat().getPromptInstructions().isAutoApprove();
        }
        return autoApprove;
    }

    public void setAutoApprove(boolean autoApprove) {
        this.autoApprove = autoApprove;
        if (orchestrator != null && orchestrator.getAiChat() != null &&orchestrator.getAiChat().getPromptInstructions() != null) {
        	orchestrator.getAiChat().getPromptInstructions().setAutoApprove(autoApprove);
        }
    }

    public void checkPause() {
        if (paused) {
            synchronized (pauseLock) {
                while (paused) {
                    try {
                        pauseLock.wait();
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
    }
}
