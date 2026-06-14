package eu.kalafatic.forge.model;

public interface EvolutionSnapshot {
    String getId();
    void setId(String id);
    String getFullGraphState();
    void setFullGraphState(String state);
    String getWeightsSnapshotRef();
    void setWeightsSnapshotRef(String ref);
    long getTimestamp();
    void setTimestamp(long timestamp);
    String getMetrics();
    void setMetrics(String metrics);
}
