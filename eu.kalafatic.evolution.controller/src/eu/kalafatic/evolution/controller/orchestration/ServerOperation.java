package eu.kalafatic.evolution.controller.orchestration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

/**
 * Standard server-side asynchronous operation model.
 * Captures stable operation ID, owning session, owning security principal,
 * operation type, lifecycle timestamps, explicit states, progress, cancellation,
 * and diagnostic failure information.
 */
public class ServerOperation {
    private final String operationId;
    private final String sessionId;
    private final String ownerPrincipal;
    private final String operationType;

    private final Instant createdAt;
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private volatile Instant lastTransitionAt;

    private volatile ServerOperationState state = ServerOperationState.CREATED;
    private volatile double progress = 0.0;
    private volatile String statusMessage = "Created";

    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);
    private volatile String cancellationReason;

    private volatile String resultSummary;
    private final Map<String, Object> resultPayload = new ConcurrentHashMap<>();

    private volatile String failureReason;
    private volatile Throwable failureCause;
    private volatile String failureStackTrace;

    private volatile Future<?> executionFuture;
    private volatile Thread workerThread;

    public ServerOperation(String operationId, String sessionId, String ownerPrincipal, String operationType) {
        this.operationId = operationId != null && !operationId.trim().isEmpty() ? operationId : "op-" + UUID.randomUUID().toString();
        this.sessionId = sessionId != null && !sessionId.trim().isEmpty() ? sessionId : "GLOBAL";
        this.ownerPrincipal = ownerPrincipal != null && !ownerPrincipal.trim().isEmpty() ? ownerPrincipal : "ANONYMOUS";
        this.operationType = operationType != null && !operationType.trim().isEmpty() ? operationType : "GENERAL";
        this.createdAt = Instant.now();
        this.lastTransitionAt = this.createdAt;
    }

    public static ServerOperation create(String sessionId, String ownerPrincipal, String operationType) {
        return new ServerOperation(null, sessionId, ownerPrincipal, operationType);
    }

    public synchronized boolean transitionTo(ServerOperationState newState, String message) {
        if (newState == null) {
            throw new IllegalArgumentException("Target state cannot be null");
        }
        if (this.state == newState) {
            if (message != null) {
                this.statusMessage = message;
            }
            return true;
        }
        if (!this.state.canTransitionTo(newState)) {
            throw new IllegalStateException(
                "Invalid operation state transition from " + this.state + " to " + newState + " for operation " + operationId
            );
        }

        this.state = newState;
        this.lastTransitionAt = Instant.now();
        if (message != null) {
            this.statusMessage = message;
        }

        if (newState == ServerOperationState.RUNNING && this.startedAt == null) {
            this.startedAt = this.lastTransitionAt;
        } else if (newState.isTerminal()) {
            this.completedAt = this.lastTransitionAt;
            if (newState == ServerOperationState.COMPLETED) {
                this.progress = 1.0;
            }
        }

        return true;
    }

    public synchronized void setProgress(double progress, String statusMessage) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
        if (statusMessage != null && !statusMessage.trim().isEmpty()) {
            this.statusMessage = statusMessage;
        }
    }

    public synchronized boolean cancel(String reason) {
        this.cancellationRequested.set(true);
        this.cancellationReason = reason != null ? reason : "Cancelled by user";

        if (this.state.isTerminal()) {
            return false;
        }

        if (this.state == ServerOperationState.CREATED || this.state == ServerOperationState.QUEUED) {
            transitionTo(ServerOperationState.CANCELLED, this.cancellationReason);
        } else if (this.state.isActive()) {
            transitionTo(ServerOperationState.CANCELLING, "Cancellation requested: " + this.cancellationReason);
        }

        // Interrupt running future/thread if assigned
        if (executionFuture != null) {
            executionFuture.cancel(true);
        }
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }

        return true;
    }

    public synchronized void markCancelled(String reason) {
        this.cancellationRequested.set(true);
        if (reason != null) {
            this.cancellationReason = reason;
        }
        if (this.state != ServerOperationState.CANCELLED) {
            if (this.state.canTransitionTo(ServerOperationState.CANCELLED)) {
                transitionTo(ServerOperationState.CANCELLED, this.cancellationReason != null ? this.cancellationReason : "Operation cancelled");
            } else if (this.state.canTransitionTo(ServerOperationState.CANCELLING)) {
                transitionTo(ServerOperationState.CANCELLING, "Cancellation in progress");
                transitionTo(ServerOperationState.CANCELLED, this.cancellationReason != null ? this.cancellationReason : "Operation cancelled");
            }
        }
    }

    public synchronized void complete(String summary, Map<String, Object> payload) {
        if (payload != null) {
            this.resultPayload.putAll(payload);
        }
        this.resultSummary = summary != null ? summary : "Operation completed successfully";
        transitionTo(ServerOperationState.COMPLETED, this.resultSummary);
    }

    public synchronized void fail(String failureReason, Throwable cause) {
        this.failureReason = failureReason != null ? failureReason : (cause != null ? cause.getMessage() : "Operation failed");
        this.failureCause = cause;
        if (cause != null) {
            StringWriter sw = new StringWriter();
            cause.printStackTrace(new PrintWriter(sw));
            this.failureStackTrace = sw.toString();
        }
        transitionTo(ServerOperationState.FAILED, this.failureReason);
    }

    public synchronized void timeout(String timeoutReason) {
        this.failureReason = timeoutReason != null ? timeoutReason : "Operation timed out";
        transitionTo(ServerOperationState.TIMED_OUT, this.failureReason);
    }

    // Getters and Setters
    public String getOperationId() { return operationId; }
    public String getSessionId() { return sessionId; }
    public String getOwnerPrincipal() { return ownerPrincipal; }
    public String getOperationType() { return operationType; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getLastTransitionAt() { return lastTransitionAt; }
    public ServerOperationState getState() { return state; }
    public double getProgress() { return progress; }
    public String getStatusMessage() { return statusMessage; }
    public boolean isCancellationRequested() { return cancellationRequested.get(); }
    public String getCancellationReason() { return cancellationReason; }
    public String getResultSummary() { return resultSummary; }
    public Map<String, Object> getResultPayload() { return Collections.unmodifiableMap(resultPayload); }
    public String getFailureReason() { return failureReason; }
    public Throwable getFailureCause() { return failureCause; }
    public String getFailureStackTrace() { return failureStackTrace; }
    public Future<?> getExecutionFuture() { return executionFuture; }
    public void setExecutionFuture(Future<?> executionFuture) { this.executionFuture = executionFuture; }
    public Thread getWorkerThread() { return workerThread; }
    public void setWorkerThread(Thread workerThread) { this.workerThread = workerThread; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("operationId", operationId);
        json.put("sessionId", sessionId);
        json.put("ownerPrincipal", ownerPrincipal);
        json.put("operationType", operationType);
        json.put("state", state.name());
        json.put("progress", progress);
        json.put("statusMessage", statusMessage != null ? statusMessage : "");
        json.put("cancellationRequested", cancellationRequested.get());
        if (cancellationReason != null) json.put("cancellationReason", cancellationReason);
        json.put("createdAt", createdAt.toString());
        if (startedAt != null) json.put("startedAt", startedAt.toString());
        if (completedAt != null) json.put("completedAt", completedAt.toString());
        if (lastTransitionAt != null) json.put("lastTransitionAt", lastTransitionAt.toString());
        if (resultSummary != null) json.put("resultSummary", resultSummary);
        if (!resultPayload.isEmpty()) json.put("resultPayload", new JSONObject(resultPayload));
        if (failureReason != null) json.put("failureReason", failureReason);
        if (failureStackTrace != null) json.put("failureStackTrace", failureStackTrace);
        return json;
    }
}
