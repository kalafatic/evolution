package eu.kalafatic.evolution.controller.orchestration.dream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Normalized representation of a candidate experience discovered from EVO system sources.
 */
public class DreamExperienceCandidate implements Serializable {

    private static final long serialVersionUID = 1L;

    private String candidateId;
    private String category; // NEGATIVE, POSITIVE, KNOWLEDGE, EVENT
    private String source;   // FailureMemory, MemoryService, IterationMemory, FunctionalityInventory
    private String topic;
    private String description;
    private double score;
    private String rationale;
    private double severity; // 0.0 to 1.0
    private int recurrenceCount;
    private boolean unresolved;
    private long timestamp;
    private List<String> evidenceReferences = new ArrayList<>();
    private long lastAnalyzedTime;

    public DreamExperienceCandidate() {
    }

    public DreamExperienceCandidate(String candidateId, String category, String source, String topic, String description,
                                    double score, String rationale, double severity, int recurrenceCount,
                                    boolean unresolved, long timestamp) {
        this.candidateId = candidateId;
        this.category = category;
        this.source = source;
        this.topic = topic;
        this.description = description;
        this.score = score;
        this.rationale = rationale;
        this.severity = severity;
        this.recurrenceCount = recurrenceCount;
        this.unresolved = unresolved;
        this.timestamp = timestamp;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public double getSeverity() {
        return severity;
    }

    public void setSeverity(double severity) {
        this.severity = severity;
    }

    public int getRecurrenceCount() {
        return recurrenceCount;
    }

    public void setRecurrenceCount(int recurrenceCount) {
        this.recurrenceCount = recurrenceCount;
    }

    public boolean isUnresolved() {
        return unresolved;
    }

    public void setUnresolved(boolean unresolved) {
        this.unresolved = unresolved;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }

    public long getLastAnalyzedTime() {
        return lastAnalyzedTime;
    }

    public void setLastAnalyzedTime(long lastAnalyzedTime) {
        this.lastAnalyzedTime = lastAnalyzedTime;
    }
}
