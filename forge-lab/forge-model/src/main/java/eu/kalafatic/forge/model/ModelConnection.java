package eu.kalafatic.forge.model;

public interface ModelConnection {
    String getFromSubModelId();
    void setFromSubModelId(String id);
    String getToSubModelId();
    void setToSubModelId(String id);
    ConnectionType getConnectionType();
    void setConnectionType(ConnectionType type);
    SubModel getSource();
    void setSource(SubModel source);
    SubModel getTarget();
    void setTarget(SubModel target);
}
