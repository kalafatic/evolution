package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Shared context for an orchestration task, including project info, state, and logs.
 */
public class TaskContext {
    private final Orchestrator orchestrator;
    private final File projectRoot;
    private final Map<String, String> state = new ConcurrentHashMap<>();
    private final List<String> logs = new ArrayList<>();
    private String sharedMemory = "";

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
        return sharedMemory;
    }

    public void appendSharedMemory(String content) {
        this.sharedMemory += "\n" + content;
    }
}
