package eu.kalafatic.evolution.selfdev.genome.event;

public interface GenomeEventBus {

    void publish(GenomeEvent event);

    void subscribe(GenomeEventListener listener);
}
