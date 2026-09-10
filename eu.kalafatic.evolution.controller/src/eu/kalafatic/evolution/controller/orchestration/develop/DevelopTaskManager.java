package eu.kalafatic.evolution.controller.orchestration.develop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Task manager owning DevelopTask registration, tracking, lifecycle state updates, and execution dispatch.
 */
public class DevelopTaskManager {

    private static final DevelopTaskManager INSTANCE = new DevelopTaskManager();
    private final Map<String, DevelopTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, DevelopSession> taskSessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private DevelopTaskManager() {
    }

    public static DevelopTaskManager getInstance() {
        return INSTANCE;
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
            taskSessions.put(session.getSessionId(), session); // allow lookup by session ID as well
        }
    }

    public boolean cancelTask(String taskId) {
        DevelopTask task = getTask(taskId);
        if (task == null) return false;

        task.setStatus(DevelopTaskStatus.CANCELLED);
        DevelopSession session = taskSessions.get(taskId);
        if (session != null) {
            session.cancel();
        }
        return true;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
