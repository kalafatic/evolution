package eu.kalafatic.evolution.view.editors.pages;

import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.utils.model.ObservableProperty;

public abstract class BaseViewModel {
    protected final List<ObservableProperty<?>> properties = new ArrayList<>();

    protected void registerProperty(ObservableProperty<?> property) {
        properties.add(property);
    }

    public List<ObservableProperty<?>> getProperties() {
        return properties;
    }

    public abstract void dispose();
}
