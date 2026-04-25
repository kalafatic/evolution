package eu.kalafatic.utils.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ObservableList<T> implements Iterable<T> {
    private final List<T> list = new ArrayList<>();
    private final String name;
    private final List<IModelChangeListener> listeners = new ArrayList<>();

    public ObservableList(String name) {
        this.name = name;
    }

    public void add(T item) {
        list.add(item);
        fireChange(new ModelChangeEvent(ModelChangeEvent.Type.ADD, name, null, item));
    }

    public void remove(T item) {
        if (list.remove(item)) {
            fireChange(new ModelChangeEvent(ModelChangeEvent.Type.REMOVE, name, item, null));
        }
    }

    public void clear() {
        List<T> oldList = new ArrayList<>(list);
        list.clear();
        for (T item : oldList) {
            fireChange(new ModelChangeEvent(ModelChangeEvent.Type.REMOVE, name, item, null));
        }
    }

    public List<T> getList() {
        return new ArrayList<>(list);
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

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    public int size() {
        return list.size();
    }
}
