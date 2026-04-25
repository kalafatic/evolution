package eu.kalafatic.utils.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ObservableProperty<T> {
    private T value;
    private final String name;
    private final List<IModelChangeListener> listeners = new ArrayList<>();

    public ObservableProperty(String name) {
        this(name, null);
    }

    public ObservableProperty(String name, T initialValue) {
        this.name = name;
        this.value = initialValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T newValue) {
        if (!Objects.equals(this.value, newValue)) {
            T oldValue = this.value;
            this.value = newValue;
            fireChange(new ModelChangeEvent(ModelChangeEvent.Type.SET, name, oldValue, newValue));
        }
    }

    public void addChangeListener(IModelChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(IModelChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireChange(ModelChangeEvent event) {
        for (IModelChangeListener listener : new ArrayList<>(listeners)) {
            listener.onChanged(event);
        }
    }

    public String getName() {
        return name;
    }
}
