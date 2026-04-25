package eu.kalafatic.utils.model;

public class ModelChangeEvent {
    public enum Type {
        SET, ADD, REMOVE
    }

    private final Type type;
    private final Object newValue;
    private final Object oldValue;
    private final String propertyName;

    public ModelChangeEvent(Type type, String propertyName, Object oldValue, Object newValue) {
        this.type = type;
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Type getType() {
        return type;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
