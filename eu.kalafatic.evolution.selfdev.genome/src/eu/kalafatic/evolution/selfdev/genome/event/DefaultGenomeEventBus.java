package eu.kalafatic.evolution.selfdev.genome.event;

import java.util.ArrayList;
import java.util.List;

public class DefaultGenomeEventBus implements GenomeEventBus {
    private final List<GenomeEventListener> listeners = new ArrayList<>();

    @Override
    public void publish(GenomeEvent event) {
        for (GenomeEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    @Override
    public void subscribe(GenomeEventListener listener) {
        listeners.add(listener);
    }
}
