package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Request for an orchestration task.
 */
public class TaskRequest {
    private String prompt;
    private File projectRoot;
    private Map<String, Object> context = new HashMap<>();

    public TaskRequest() {}

    public TaskRequest(String prompt, File projectRoot) {
        this.prompt = prompt;
        this.projectRoot = projectRoot;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
