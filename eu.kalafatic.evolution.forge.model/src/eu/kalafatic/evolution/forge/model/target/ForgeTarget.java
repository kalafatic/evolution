package eu.kalafatic.evolution.forge.model.target;

import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;

public class ForgeTarget {
    private String path;
    private ForgeTargetType type;
    private boolean valid;
    private String statusMessage;
    private String displayName;
    private long parameterCount;
    private int vocabSize;
    private String modelName;
    private String architectureSummary;
    private EvoModelArtifact artifact;

    public ForgeTarget(String path, ForgeTargetType type, boolean valid, String statusMessage) {
        this.path = path != null ? path : "";
        this.type = type != null ? type : ForgeTargetType.TRAINING_DATA;
        this.valid = valid;
        this.statusMessage = statusMessage != null ? statusMessage : "";
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public ForgeTargetType getType() { return type; }
    public void setType(ForgeTargetType type) { this.type = type; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getDisplayName() { return displayName != null ? displayName : path; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public long getParameterCount() { return parameterCount; }
    public void setParameterCount(long parameterCount) { this.parameterCount = parameterCount; }

    public int getVocabSize() { return vocabSize; }
    public void setVocabSize(int vocabSize) { this.vocabSize = vocabSize; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getArchitectureSummary() { return architectureSummary; }
    public void setArchitectureSummary(String architectureSummary) { this.architectureSummary = architectureSummary; }

    public EvoModelArtifact getArtifact() { return artifact; }
    public void setArtifact(EvoModelArtifact artifact) { this.artifact = artifact; }

    @Override
    public String toString() {
        return "ForgeTarget{" +
                "path='" + path + '\'' +
                ", type=" + type +
                ", valid=" + valid +
                ", statusMessage='" + statusMessage + '\'' +
                '}';
    }
}
