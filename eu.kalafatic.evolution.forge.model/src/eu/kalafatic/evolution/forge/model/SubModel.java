package eu.kalafatic.evolution.forge.model;

public interface SubModel {
    String getId();
    void setId(String id);
    SubModelType getType();
    void setType(SubModelType type);
    String getConfig();
    void setConfig(String config);
    String getParameters();
    void setParameters(String parameters);
    String getInputShape();
    void setInputShape(String inputShape);
    String getOutputShape();
    void setOutputShape(String outputShape);
    boolean isFrozen();
    void setFrozen(boolean frozen);
}
