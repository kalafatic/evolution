package eu.kalafatic.evolution.controller.manager.removal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable-ish outcome of a model removal operation, including every stage
 * result and a truthful final classification.
 */
public class ModelRemovalResult {

    private final String removalId;
    private final String modelName;
    private final String modelType;
    private final String endpointUrl;
    private ModelRemovalStatus status = ModelRemovalStatus.FAILED;

    private String canonicalIdentity = "";
    private String requestedIdentity = "";
    private String provider = "";
    private String artifactLocation = "";
    private String ownership = "";
    private Set<String> resolvedAliases = new LinkedHashSet<>();

    private String runtimeOutcome = "Not applicable";
    private String registryOutcome = "Not applicable";
    private String configurationOutcome = "Not applicable";
    private String artifactOutcome = "Not applicable";
    private String verificationOutcome = "Not performed";

    private final List<ModelRemovalStepResult> stepResults = new ArrayList<>();
    private final StringBuilder logBuilder = new StringBuilder();

    public ModelRemovalResult(String removalId, String modelName, String modelType, String endpointUrl) {
        this.removalId = removalId;
        this.modelName = modelName;
        this.modelType = modelType;
        this.endpointUrl = endpointUrl;
        this.requestedIdentity = modelName;
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

    public String getCanonicalIdentity() {
        return canonicalIdentity;
    }

    public void setCanonicalIdentity(String canonicalIdentity) {
        this.canonicalIdentity = canonicalIdentity != null ? canonicalIdentity : "";
    }

    public String getRequestedIdentity() {
        return requestedIdentity;
    }

    public void setRequestedIdentity(String requestedIdentity) {
        this.requestedIdentity = requestedIdentity != null ? requestedIdentity : "";
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider != null ? provider : "";
    }

    public String getArtifactLocation() {
        return artifactLocation;
    }

    public void setArtifactLocation(String artifactLocation) {
        this.artifactLocation = artifactLocation != null ? artifactLocation : "";
    }

    public String getOwnership() {
        return ownership;
    }

    public void setOwnership(String ownership) {
        this.ownership = ownership != null ? ownership : "";
    }

    public Set<String> getResolvedAliases() {
        return Collections.unmodifiableSet(resolvedAliases);
    }

    public void setResolvedAliases(Set<String> aliases) {
        this.resolvedAliases = aliases != null ? new LinkedHashSet<>(aliases) : new LinkedHashSet<>();
    }

    public String getRuntimeOutcome() {
        return runtimeOutcome;
    }

    public void setRuntimeOutcome(String runtimeOutcome) {
        this.runtimeOutcome = runtimeOutcome != null ? runtimeOutcome : "";
    }

    public String getRegistryOutcome() {
        return registryOutcome;
    }

    public void setRegistryOutcome(String registryOutcome) {
        this.registryOutcome = registryOutcome != null ? registryOutcome : "";
    }

    public String getConfigurationOutcome() {
        return configurationOutcome;
    }

    public void setConfigurationOutcome(String configurationOutcome) {
        this.configurationOutcome = configurationOutcome != null ? configurationOutcome : "";
    }

    public String getArtifactOutcome() {
        return artifactOutcome;
    }

    public void setArtifactOutcome(String artifactOutcome) {
        this.artifactOutcome = artifactOutcome != null ? artifactOutcome : "";
    }

    public String getVerificationOutcome() {
        return verificationOutcome;
    }

    public void setVerificationOutcome(String verificationOutcome) {
        this.verificationOutcome = verificationOutcome != null ? verificationOutcome : "";
    }

    public void addStepResult(ModelRemovalStepResult stepResult) {
        this.stepResults.add(stepResult);
        ModelRemovalStepResult.StepOutcome outcome = stepResult.outcome();
        log("[" + removalId + "][" + stepResult.stepName() + "] " + outcome + ": " + stepResult.message());
    }

    public List<ModelRemovalStepResult> getStepResults() {
        return Collections.unmodifiableList(stepResults);
    }

    public boolean hasFailedStep() {
        for (ModelRemovalStepResult step : stepResults) {
            if (step.outcome() == ModelRemovalStepResult.StepOutcome.FAILED) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when every required stage either succeeded or was explicitly
     * skipped as not applicable.
     */
    public boolean isClean() {
        return !hasFailedStep();
    }

    public String getFinalSummaryLine() {
        return "[" + removalId + "][FINAL] " + status;
    }

    /**
     * Human-readable table row used by the bulk-result dialog.
     */
    public String getSummaryTableRow() {
        return modelName + " | " + runtimeOutcome + " | " + registryOutcome + " | "
                + configurationOutcome + " | " + artifactOutcome + " | " + status;
    }

    public void log(String message) {
        logBuilder.append(message).append("\n");
        System.out.println("[REMOVAL_LOG]" + message);
    }

    public String getDiagnosticLog() {
        return logBuilder.toString();
    }
}
