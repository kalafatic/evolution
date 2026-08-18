package eu.kalafatic.evolution.forge.data.api.collector;

import java.util.ArrayList;
import java.util.List;

/**
 * Result returned by a data collector after execution.
 */
public class CollectionResult {

    public enum Status {
        SUCCESS,
        PARTIAL,
        FAILED
    }

    private CollectorType collectorType;
    private Status status;
    private List<TrainingDataItem> items;
    private String errorMessage;
    private long durationMs;

    public CollectionResult() {
        this.status = Status.SUCCESS;
        this.items = new ArrayList<>();
    }

    public CollectionResult(CollectorType collectorType, Status status, List<TrainingDataItem> items) {
        this.collectorType = collectorType;
        this.status = status;
        this.items = items != null ? items : new ArrayList<>();
    }

    public static CollectionResult success(CollectorType type, List<TrainingDataItem> items, long durationMs) {
        CollectionResult result = new CollectionResult(type, Status.SUCCESS, items);
        result.setDurationMs(durationMs);
        return result;
    }

    public static CollectionResult failure(CollectorType type, String errorMessage, long durationMs) {
        CollectionResult result = new CollectionResult(type, Status.FAILED, new ArrayList<>());
        result.setErrorMessage(errorMessage);
        result.setDurationMs(durationMs);
        return result;
    }

    public CollectorType getCollectorType() {
        return collectorType;
    }

    public void setCollectorType(CollectorType collectorType) {
        this.collectorType = collectorType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<TrainingDataItem> getItems() {
        return items;
    }

    public void setItems(List<TrainingDataItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void addItem(TrainingDataItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String toString() {
        return "CollectionResult{" +
                "collectorType=" + collectorType +
                ", status=" + status +
                ", itemCount=" + (items != null ? items.size() : 0) +
                ", durationMs=" + durationMs +
                (errorMessage != null ? ", errorMessage='" + errorMessage + '\'' : "") +
                '}';
    }
}
