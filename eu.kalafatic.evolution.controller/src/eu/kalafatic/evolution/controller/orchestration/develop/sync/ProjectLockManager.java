package eu.kalafatic.evolution.controller.orchestration.develop.sync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import eu.kalafatic.evolution.controller.log.Log;

/**
 * Fine-grained locking coordinator managing project and workspace level locks.
 * Implements a thread-agnostic atomic lease state machine allowing:
 * - Deterministic timeouts on lock acquisition.
 * - Safe preemption of expired leases by new tasks across threads.
 * - Safe release of locks on task cancellation from arbitrary HTTP/worker threads.
 * - Independent repositories to execute concurrently in parallel.
 */
public class ProjectLockManager {

    private static final ProjectLockManager INSTANCE = new ProjectLockManager();

    private static class Lease {
        final String ownerTaskId;
        final long acquiredAt;
        final long leaseTimeoutMs;

        Lease(String ownerTaskId, long leaseTimeoutMs) {
            this.ownerTaskId = ownerTaskId;
            this.acquiredAt = System.currentTimeMillis();
            this.leaseTimeoutMs = leaseTimeoutMs;
        }

        boolean isExpired() {
            return leaseTimeoutMs > 0 && (System.currentTimeMillis() - acquiredAt) > leaseTimeoutMs;
        }
    }

    private final Map<String, AtomicReference<Lease>> projectLocks = new ConcurrentHashMap<>();

    private ProjectLockManager() {
    }

    public static ProjectLockManager getInstance() {
        return INSTANCE;
    }

    /**
     * Attempts to acquire exclusive lock on a repository/project.
     * If held by another task but the lease is expired, the expired lease is preempted.
     */
    public boolean acquireProjectLock(String projectKey, String taskId, long timeoutMs, long leaseTimeoutMs) {
        if (projectKey == null || projectKey.trim().isEmpty() || taskId == null) {
            return true;
        }

        AtomicReference<Lease> ref = projectLocks.computeIfAbsent(projectKey, k -> new AtomicReference<>(null));
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);

        while (true) {
            Lease current = ref.get();

            // Already held by this task
            if (current != null && taskId.equals(current.ownerTaskId)) {
                return true;
            }

            // Lock is free or expired -> attempt CAS
            if (current == null || current.isExpired()) {
                if (current != null && current.isExpired()) {
                    Log.log("[LOCK] Preempting expired lease on " + projectKey + " (held by " + current.ownerTaskId + ")");
                }
                Lease newLease = new Lease(taskId, leaseTimeoutMs);
                if (ref.compareAndSet(current, newLease)) {
                    Log.log("[LOCK] Task " + taskId + " acquired project lock for " + projectKey);
                    return true;
                }
                // CAS failed, retry immediately
                continue;
            }

            // Check if deadline exceeded
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                Log.log("[LOCK] Task " + taskId + " timed out waiting for lock on " + projectKey + " (held by " + current.ownerTaskId + ")");
                return false;
            }

            try {
                Thread.sleep(Math.min(50, remaining));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * Releases exclusive project lock held by taskId. Thread-agnostic: can be called from any thread.
     */
    public void releaseProjectLock(String projectKey, String taskId) {
        if (projectKey == null || taskId == null) return;
        AtomicReference<Lease> ref = projectLocks.get(projectKey);
        if (ref != null) {
            Lease current = ref.get();
            if (current != null && taskId.equals(current.ownerTaskId)) {
                if (ref.compareAndSet(current, null)) {
                    Log.log("[LOCK] Task " + taskId + " released project lock for " + projectKey);
                }
            }
        }
    }

    /**
     * Releases all locks held by a task (safe cleanup on cancellation or failure across any thread).
     */
    public void releaseAllForTask(String taskId) {
        if (taskId == null) return;
        for (Map.Entry<String, AtomicReference<Lease>> e : projectLocks.entrySet()) {
            AtomicReference<Lease> ref = e.getValue();
            Lease current = ref.get();
            if (current != null && taskId.equals(current.ownerTaskId)) {
                if (ref.compareAndSet(current, null)) {
                    Log.log("[LOCK] Auto-released project lock for " + e.getKey() + " held by task " + taskId);
                }
            }
        }
    }
}
