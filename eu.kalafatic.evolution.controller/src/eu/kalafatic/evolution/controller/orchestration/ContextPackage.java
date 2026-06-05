package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Deterministic structure for minimal task context.
 */
public class ContextPackage {
    private String goal;
    private String step;
    private ArchitectureContext architectureContext;
    private List<String> scope = new ArrayList<>();
    private String code;
    private String dependencies;
    private List<String> constraints = new ArrayList<>();
    private String attachmentContext;
    private int attempt;
    private String lastFeedback;

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public ArchitectureContext getArchitectureContext() {
        return architectureContext;
    }

    public void setArchitectureContext(ArchitectureContext architectureContext) {
        this.architectureContext = architectureContext;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDependencies() {
        return dependencies;
    }

    public void setDependencies(String dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public String getLastFeedback() {
        return lastFeedback;
    }

    public void setLastFeedback(String lastFeedback) {
        this.lastFeedback = lastFeedback;
    }

    public String getAttachmentContext() {
        return attachmentContext;
    }

    public void setAttachmentContext(String attachmentContext) {
        this.attachmentContext = attachmentContext;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("goal", goal);
        json.put("step", step);
        if (architectureContext != null) {
            json.put("architecture", architectureContext.toJson());
        }
        json.put("scope", new JSONArray(scope));
        json.put("code", code);
        json.put("dependencies", dependencies);
        json.put("constraints", new JSONArray(constraints));
        json.put("attachmentContext", attachmentContext);
        json.put("attempt", attempt);
        json.put("lastFeedback", lastFeedback);
        return json;
    }
}
