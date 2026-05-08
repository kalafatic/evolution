package eu.kalafatic.evolution.controller.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RuntimeEventBus {
    private static final RuntimeEventBus INSTANCE = new RuntimeEventBus();
    private final List<RuntimeEventListener> listeners = new CopyOnWriteArrayList<>();

    private RuntimeEventBus() {}

    public static RuntimeEventBus getInstance() { return INSTANCE; }

    public void subscribe(RuntimeEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(RuntimeEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(RuntimeEvent event) {
        for (RuntimeEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
