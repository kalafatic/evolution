package eu.kalafatic.evolution.controller.manager.removal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModelRemovalResult {

    private final String removalId;
    private final String modelName;
    private final String modelType;
    private final String endpointUrl;
    private ModelRemovalStatus status = ModelRemovalStatus.FAILED;
    private final List<ModelRemovalStepResult> stepResults = new ArrayList<>();
    private final StringBuilder logBuilder = new StringBuilder();

    public ModelRemovalResult(String removalId, String modelName, String modelType, String endpointUrl) {
        this.removalId = removalId;
        this.modelName = modelName;
        this.modelType = modelType;
        this.endpointUrl = endpointUrl;
    }

    public String getRemovalId() {
        return removalId;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelType() {
        return modelType;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public ModelRemovalStatus getStatus() {
        return status;
    }

    public void setStatus(ModelRemovalStatus status) {
        this.status = status;
    }

    public void addStepResult(ModelRemovalStepResult stepResult) {
        this.stepResults.add(stepResult);
        log("[" + removalId + "][" + stepResult.stepName() + "] " +
                (stepResult.success() ? "SUCCESS: " : "FAILED: ") + stepResult.message());
    }

    public List<ModelRemovalStepResult> getStepResults() {
        return Collections.unmodifiableList(stepResults);
    }

    public void log(String message) {
        logBuilder.append(message).append("\n");
        System.out.println("[REMOVAL_LOG]" + message);
    }

    public String getDiagnosticLog() {
        return logBuilder.toString();
    }
}
