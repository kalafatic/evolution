package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

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
    private final List<TokenRequestListener> tokenRequestListeners = new CopyOnWriteArrayList<>();
    private CompletableFuture<Boolean> approvalFuture;

    public interface LogListener {
        void onLog(String message);
    }

    public interface ApprovalListener {
        void onApprovalRequested(String message);
    }

    public interface TokenRequestListener {
        void onTokenRequested(String providerName, CompletableFuture<String> tokenFuture);
    }

    public TaskContext(Orchestrator orchestrator, File projectRoot) {
        this.orchestrator = orchestrator;
        this.projectRoot = projectRoot;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public void log(String message) {
        logs.add(message);
        notifyListeners(message);
    }

    public void addLogListener(LogListener listener) {
        listeners.add(listener);
    }

    public void addApprovalListener(ApprovalListener listener) {
        approvalListeners.add(listener);
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
        if (approvalFuture != null) {
            approvalFuture.complete(approved);
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
}
