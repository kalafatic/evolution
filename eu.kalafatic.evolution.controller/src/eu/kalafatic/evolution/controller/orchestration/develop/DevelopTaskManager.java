package eu.kalafatic.evolution.controller.orchestration.develop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.develop.agent.DevelopAgent;
import eu.kalafatic.evolution.controller.orchestration.develop.sync.ProjectLockManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Task manager and scheduler owning DevelopTask registration, priority queueing,
 * dependency resolution, worker pool concurrency dispatch, lifecycle state updates, and restart recovery.
 */
public class DevelopTaskManager {

    private static final DevelopTaskManager INSTANCE = new DevelopTaskManager();
    private final Map<String, DevelopTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, DevelopSession> taskSessions = new ConcurrentHashMap<>();

    private int maxConcurrentTasks = 4;
    private final AtomicInteger activeRunningTasks = new AtomicInteger(0);
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final PriorityQueue<DevelopTask> taskQueue = new PriorityQueue<>(
            Comparator.comparingInt(DevelopTask::getPriority).reversed()
                      .thenComparingLong(DevelopTask::getCreatedAt)
    );
    private final Object queueLock = new Object();

    private DevelopTaskManager() {
        startSchedulerLoop();
    }

    public static DevelopTaskManager getInstance() {
        return INSTANCE;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = Math.max(1, maxConcurrentTasks);
        triggerDispatch();
    }

    public int getActiveRunningTasksCount() {
        return activeRunningTasks.get();
    }

    public void registerTask(DevelopTask task) {
        if (task != null && task.getId() != null) {
            tasks.put(task.getId(), task);
        }
    }

    public DevelopTask createTask(String title, String description, String repository, String branch, DevelopPermissions permissions) {
        DevelopTask task = new DevelopTask(title, description, repository, branch, permissions);
        tasks.put(task.getId(), task);
        return task;
    }

    public DevelopTask getTask(String taskId) {
        if (taskId == null) return null;
        return tasks.get(taskId);
    }

    public Collection<DevelopTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public DevelopSession getSessionForTask(String taskId) {
        if (taskId == null) return null;
        return taskSessions.get(taskId);
    }

    public void registerSessionForTask(String taskId, DevelopSession session) {
        if (taskId != null && session != null) {
            taskSessions.put(taskId, session);
            taskSessions.put(session.getSessionId(), session);
        }
    }

    public void submitTaskForExecution(String taskId) {
        DevelopTask task = getTask(taskId);
        if (task == null) return;

        synchronized (queueLock) {
            if (task.getStatus() == DevelopTaskStatus.QUEUED
                    || task.getStatus() == DevelopTaskStatus.WAITING_FOR_DEPENDENCIES
                    || task.getStatus() == DevelopTaskStatus.WAITING_FOR_RESOURCES
                    || task.getStatus() == DevelopTaskStatus.PREPARING
                    || task.getStatus() == DevelopTaskStatus.CODING
                    || task.getStatus() == DevelopTaskStatus.BUILDING
                    || task.getStatus() == DevelopTaskStatus.TESTING) {
                return;
            }

            task.setStatus(DevelopTaskStatus.QUEUED);
            publishEvent(RuntimeEventType.TASK_QUEUED, task, "Task enqueued for scheduling");
            if (!taskQueue.contains(task)) {
                taskQueue.offer(task);
            }
        }
        triggerDispatch();
    }

    public void triggerDispatch() {
        synchronized (queueLock) {
            if (activeRunningTasks.get() >= maxConcurrentTasks) {
                for (DevelopTask t : taskQueue) {
                    if (t.getStatus() == DevelopTaskStatus.QUEUED) {
                        t.setStatus(DevelopTaskStatus.WAITING_FOR_RESOURCES);
                        publishEvent(RuntimeEventType.TASK_WAITING_FOR_RESOURCES, t, "Waiting for worker concurrency capacity");
                    }
                }
                return;
            }

            List<DevelopTask> skippedDueToDependencies = new ArrayList<>();
            while (activeRunningTasks.get() < maxConcurrentTasks && !taskQueue.isEmpty()) {
                DevelopTask candidate = taskQueue.poll();
                if (candidate == null) break;

                if (!areDependenciesSatisfied(candidate)) {
                    if (candidate.getStatus() != DevelopTaskStatus.WAITING_FOR_DEPENDENCIES) {
                        candidate.setStatus(DevelopTaskStatus.WAITING_FOR_DEPENDENCIES);
                        publishEvent(RuntimeEventType.TASK_WAITING_FOR_DEPENDENCIES, candidate, "Waiting for required prerequisite tasks");
                    }
                    skippedDueToDependencies.add(candidate);
                    continue;
                }

                activeRunningTasks.incrementAndGet();
                publishEvent(RuntimeEventType.TASK_STARTED, candidate, "Task dispatched to worker");
                dispatchTaskExecution(candidate);
            }

            taskQueue.addAll(skippedDueToDependencies);
        }
    }

    private boolean areDependenciesSatisfied(DevelopTask task) {
        List<String> deps = task.getDependsOnTaskIds();
        if (deps == null || deps.isEmpty()) {
            return true;
        }

        for (String depId : deps) {
            DevelopTask dep = getTask(depId);
            if (dep == null || dep.getStatus() != DevelopTaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    private void dispatchTaskExecution(DevelopTask task) {
        executor.submit(() -> {
            String repoKey = (task.getRepository() != null && !task.getRepository().trim().isEmpty())
                    ? task.getRepository().trim()
                    : "local-workspace";

            DevelopSession session = getSessionForTask(task.getId());
            boolean lockAcquired = false;
            try {
                lockAcquired = ProjectLockManager.getInstance().acquireProjectLock(repoKey, task.getId(), 5000, 600000);
                if (!lockAcquired) {
                    task.setStatus(DevelopTaskStatus.WAITING_FOR_RESOURCES);
                    synchronized (queueLock) {
                        taskQueue.offer(task);
                    }
                    return;
                }

                if (session != null) {
                    session.addLog("Task dispatched by DevelopTaskManager scheduler.");
                    new DevelopAgent().executeTask(session);
                }
            } catch (Exception ex) {
                Log.log("[SCHEDULER] Error executing task " + task.getId() + ": " + ex.getMessage());
                task.setStatus(DevelopTaskStatus.FAILED);
                if (session != null) {
                    session.addLog("Execution failed: " + ex.getMessage());
                }
            } finally {
                if (lockAcquired) {
                    ProjectLockManager.getInstance().releaseProjectLock(repoKey, task.getId());
                }
                activeRunningTasks.decrementAndGet();
                triggerDispatch();
            }
        });
    }

    private void startSchedulerLoop() {
        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    triggerDispatch();
                } catch (InterruptedException ie) {
                    break;
                } catch (Exception ignored) {
                }
            }
        }, "DevelopTaskSchedulerDaemon");
        daemon.setDaemon(true);
        daemon.start();
    }

    public boolean cancelTask(String taskId) {
        DevelopTask task = getTask(taskId);
        if (task == null) return false;

        synchronized (queueLock) {
            taskQueue.remove(task);
        }

        task.setStatus(DevelopTaskStatus.CANCELLED);
        publishEvent(RuntimeEventType.TASK_CANCELLED, task, "Task cancelled by user");
        DevelopSession session = taskSessions.get(taskId);
        if (session != null) {
            session.cancel();
        }

        ProjectLockManager.getInstance().releaseAllForTask(taskId);
        triggerDispatch();
        return true;
    }

    private void publishEvent(RuntimeEventType type, DevelopTask task, String message) {
        try {
            String sid = task.getSessionId();
            if (sid != null) {
                eu.kalafatic.evolution.controller.orchestration.SessionContainer sc =
                        eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(sid);
                if (sc != null && sc.getEventBus() != null) {
                    sc.getEventBus().publish(new RuntimeEvent(type, task.getId(), "DevelopTaskManager", message));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void recoverTasksOnRestart() {
        for (DevelopTask t : tasks.values()) {
            DevelopTaskStatus s = t.getStatus();
            if (s == DevelopTaskStatus.QUEUED
                    || s == DevelopTaskStatus.WAITING_FOR_RESOURCES
                    || s == DevelopTaskStatus.WAITING_FOR_DEPENDENCIES) {
                synchronized (queueLock) {
                    if (!taskQueue.contains(t)) {
                        taskQueue.offer(t);
                    }
                }
            } else if (s == DevelopTaskStatus.PREPARING
                    || s == DevelopTaskStatus.ANALYZING
                    || s == DevelopTaskStatus.PLANNING
                    || s == DevelopTaskStatus.CODING
                    || s == DevelopTaskStatus.BUILDING
                    || s == DevelopTaskStatus.TESTING
                    || s == DevelopTaskStatus.FIXING
                    || s == DevelopTaskStatus.COMMITTING) {
                t.setStatus(DevelopTaskStatus.FAILED);
                DevelopSession sess = getSessionForTask(t.getId());
                if (sess != null) {
                    sess.addLog("Server restart detected. Interrupted execution transitioned to FAILED.");
                }
            }
        }
        triggerDispatch();
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
