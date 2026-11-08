package eu.kalafatic.evolution.forge.model.inference;

import java.io.Serializable;
import org.json.JSONObject;

/**
 * Authoritative Inference Provenance model capturing verifiable runtime evidence
 * of model artifact identity, conversion lineage, engine execution, backend, and fallback status.
 */
public class InferenceProvenance implements Serializable {

    private static final long serialVersionUID = 1L;

    // Artifact identity
    private String artifactName = "UNKNOWN";
    private String artifactPath = "UNKNOWN";
    private String artifactFormat = "UNKNOWN"; // EVO, GGUF, DIRECT_SNAPSHOT, UNKNOWN
    private String artifactChecksum = "UNKNOWN"; // SHA-256
    private String verificationStatus = "UNVERIFIED"; // VERIFIED, UNVERIFIED, FAILED
    private String trainingLineage = "UNKNOWN";

    // Conversion information
    private String sourceEvoArtifact = "NONE";
    private String exportedGgufArtifact = "NONE";
    private String ggufChecksum = "NONE";
    private String conversionTimestamp = "NONE";
    private String adapterInfo = "NONE";
    private String artifactRelationship = "DIRECT"; // DIRECT, EXPORTED_GGUF, CONVERTED, NONE

    // Runtime information
    private String actualEngine = "UNKNOWN"; // ReferenceEvoInferenceEngine, LlamaCppRunner, OllamaService, etc.
    private String runtimeProvider = "UNKNOWN";
    private String backend = "UNKNOWN"; // NATIVE_EVO, LLAMA_CPP, OLLAMA, UNKNOWN
    private String loadedModelIdentity = "UNKNOWN";
    private String loadedModelChecksum = "UNKNOWN";
    private String sourceFormat = "UNKNOWN"; // EVO, GGUF, OLLAMA_REGISTRY, UNKNOWN
    private boolean isNative = false;
    private boolean fallbackActivated = false;
    private String fallbackReason = "NONE";

    // Trace & Accounting
    private String inferenceId = "UNKNOWN";
    private String sessionId = "UNKNOWN";
    private String conversationId = "UNKNOWN";
    private String darwinMode = "UNKNOWN";
    private int promptTokens = -1;
    private int completionTokens = -1;
    private int totalTokens = -1;
    private long executionDurationMs = -1;

    public InferenceProvenance() {
    }

    public static InferenceProvenance createUnverified(String inferenceId, String sessionId) {
        InferenceProvenance p = new InferenceProvenance();
        p.setInferenceId(inferenceId != null ? inferenceId : "UNKNOWN");
        p.setSessionId(sessionId != null ? sessionId : "UNKNOWN");
        p.setVerificationStatus("UNVERIFIED");
        return p;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("inferenceId", inferenceId);
        json.put("sessionId", sessionId);
        json.put("conversationId", conversationId);
        json.put("darwinMode", darwinMode);
        json.put("artifactName", artifactName);
        json.put("artifactPath", artifactPath);
        json.put("artifactFormat", artifactFormat);
        json.put("artifactChecksum", artifactChecksum);
        json.put("verificationStatus", verificationStatus);
        json.put("trainingLineage", trainingLineage);
        json.put("sourceEvoArtifact", sourceEvoArtifact);
        json.put("exportedGgufArtifact", exportedGgufArtifact);
        json.put("ggufChecksum", ggufChecksum);
        json.put("conversionTimestamp", conversionTimestamp);
        json.put("adapterInfo", adapterInfo);
        json.put("artifactRelationship", artifactRelationship);
        json.put("actualEngine", actualEngine);
        json.put("runtimeProvider", runtimeProvider);
        json.put("backend", backend);
        json.put("loadedModelIdentity", loadedModelIdentity);
        json.put("loadedModelChecksum", loadedModelChecksum);
        json.put("sourceFormat", sourceFormat);
        json.put("isNative", isNative);
        json.put("fallbackActivated", fallbackActivated);
        json.put("fallbackReason", fallbackReason);
        json.put("promptTokens", promptTokens);
        json.put("completionTokens", completionTokens);
        json.put("totalTokens", totalTokens);
        json.put("executionDurationMs", executionDurationMs);
        return json;
    }

    public static InferenceProvenance fromJsonObject(JSONObject json) {
        if (json == null) return null;
        InferenceProvenance p = new InferenceProvenance();
        p.inferenceId = json.optString("inferenceId", "UNKNOWN");
        p.sessionId = json.optString("sessionId", "UNKNOWN");
        p.conversationId = json.optString("conversationId", "UNKNOWN");
        p.darwinMode = json.optString("darwinMode", "UNKNOWN");
        p.artifactName = json.optString("artifactName", "UNKNOWN");
        p.artifactPath = json.optString("artifactPath", "UNKNOWN");
        p.artifactFormat = json.optString("artifactFormat", "UNKNOWN");
        p.artifactChecksum = json.optString("artifactChecksum", "UNKNOWN");
        p.verificationStatus = json.optString("verificationStatus", "UNVERIFIED");
        p.trainingLineage = json.optString("trainingLineage", "UNKNOWN");
        p.sourceEvoArtifact = json.optString("sourceEvoArtifact", "NONE");
        p.exportedGgufArtifact = json.optString("exportedGgufArtifact", "NONE");
        p.ggufChecksum = json.optString("ggufChecksum", "NONE");
        p.conversionTimestamp = json.optString("conversionTimestamp", "NONE");
        p.adapterInfo = json.optString("adapterInfo", "NONE");
        p.artifactRelationship = json.optString("artifactRelationship", "DIRECT");
        p.actualEngine = json.optString("actualEngine", "UNKNOWN");
        p.runtimeProvider = json.optString("runtimeProvider", "UNKNOWN");
        p.backend = json.optString("backend", "UNKNOWN");
        p.loadedModelIdentity = json.optString("loadedModelIdentity", "UNKNOWN");
        p.loadedModelChecksum = json.optString("loadedModelChecksum", "UNKNOWN");
        p.sourceFormat = json.optString("sourceFormat", "UNKNOWN");
        p.isNative = json.optBoolean("isNative", false);
        p.fallbackActivated = json.optBoolean("fallbackActivated", false);
        p.fallbackReason = json.optString("fallbackReason", "NONE");
        p.promptTokens = json.optInt("promptTokens", -1);
        p.completionTokens = json.optInt("completionTokens", -1);
        p.totalTokens = json.optInt("totalTokens", -1);
        p.executionDurationMs = json.optLong("executionDurationMs", -1);
        return p;
    }

    // Getters and setters
    public String getArtifactName() { return artifactName; }
    public void setArtifactName(String artifactName) { this.artifactName = artifactName != null ? artifactName : "UNKNOWN"; }

    public String getArtifactPath() { return artifactPath; }
    public void setArtifactPath(String artifactPath) { this.artifactPath = artifactPath != null ? artifactPath : "UNKNOWN"; }

    public String getArtifactFormat() { return artifactFormat; }
    public void setArtifactFormat(String artifactFormat) { this.artifactFormat = artifactFormat != null ? artifactFormat : "UNKNOWN"; }

    public String getArtifactChecksum() { return artifactChecksum; }
    public void setArtifactChecksum(String artifactChecksum) { this.artifactChecksum = artifactChecksum != null ? artifactChecksum : "UNKNOWN"; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus != null ? verificationStatus : "UNVERIFIED"; }

    public String getTrainingLineage() { return trainingLineage; }
    public void setTrainingLineage(String trainingLineage) { this.trainingLineage = trainingLineage != null ? trainingLineage : "UNKNOWN"; }

    public String getSourceEvoArtifact() { return sourceEvoArtifact; }
    public void setSourceEvoArtifact(String sourceEvoArtifact) { this.sourceEvoArtifact = sourceEvoArtifact != null ? sourceEvoArtifact : "NONE"; }

    public String getExportedGgufArtifact() { return exportedGgufArtifact; }
    public void setExportedGgufArtifact(String exportedGgufArtifact) { this.exportedGgufArtifact = exportedGgufArtifact != null ? exportedGgufArtifact : "NONE"; }

    public String getGgufChecksum() { return ggufChecksum; }
    public void setGgufChecksum(String ggufChecksum) { this.ggufChecksum = ggufChecksum != null ? ggufChecksum : "NONE"; }

    public String getConversionTimestamp() { return conversionTimestamp; }
    public void setConversionTimestamp(String conversionTimestamp) { this.conversionTimestamp = conversionTimestamp != null ? conversionTimestamp : "NONE"; }

    public String getAdapterInfo() { return adapterInfo; }
    public void setAdapterInfo(String adapterInfo) { this.adapterInfo = adapterInfo != null ? adapterInfo : "NONE"; }

    public String getArtifactRelationship() { return artifactRelationship; }
    public void setArtifactRelationship(String artifactRelationship) { this.artifactRelationship = artifactRelationship != null ? artifactRelationship : "DIRECT"; }

    public String getActualEngine() { return actualEngine; }
    public void setActualEngine(String actualEngine) { this.actualEngine = actualEngine != null ? actualEngine : "UNKNOWN"; }

    public String getRuntimeProvider() { return runtimeProvider; }
    public void setRuntimeProvider(String runtimeProvider) { this.runtimeProvider = runtimeProvider != null ? runtimeProvider : "UNKNOWN"; }

    public String getBackend() { return backend; }
    public void setBackend(String backend) { this.backend = backend != null ? backend : "UNKNOWN"; }

    public String getLoadedModelIdentity() { return loadedModelIdentity; }
    public void setLoadedModelIdentity(String loadedModelIdentity) { this.loadedModelIdentity = loadedModelIdentity != null ? loadedModelIdentity : "UNKNOWN"; }

    public String getLoadedModelChecksum() { return loadedModelChecksum; }
    public void setLoadedModelChecksum(String loadedModelChecksum) { this.loadedModelChecksum = loadedModelChecksum != null ? loadedModelChecksum : "UNKNOWN"; }

    public String getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(String sourceFormat) { this.sourceFormat = sourceFormat != null ? sourceFormat : "UNKNOWN"; }

    public boolean isNative() { return isNative; }
    public void setNative(boolean isNative) { this.isNative = isNative; }

    public boolean isFallbackActivated() { return fallbackActivated; }
    public void setFallbackActivated(boolean fallbackActivated) { this.fallbackActivated = fallbackActivated; }

    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason != null ? fallbackReason : "NONE"; }

    public String getInferenceId() { return inferenceId; }
    public void setInferenceId(String inferenceId) { this.inferenceId = inferenceId != null ? inferenceId : "UNKNOWN"; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId != null ? sessionId : "UNKNOWN"; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId != null ? conversationId : "UNKNOWN"; }

    public String getDarwinMode() { return darwinMode; }
    public void setDarwinMode(String darwinMode) { this.darwinMode = darwinMode != null ? darwinMode : "UNKNOWN"; }

    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

    public long getExecutionDurationMs() { return executionDurationMs; }
    public void setExecutionDurationMs(long executionDurationMs) { this.executionDurationMs = executionDurationMs; }
}
