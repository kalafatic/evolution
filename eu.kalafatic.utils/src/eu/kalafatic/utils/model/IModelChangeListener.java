package eu.kalafatic.utils.model;

@FunctionalInterface
public interface IModelChangeListener {
    void onChanged(ModelChangeEvent event);
}
