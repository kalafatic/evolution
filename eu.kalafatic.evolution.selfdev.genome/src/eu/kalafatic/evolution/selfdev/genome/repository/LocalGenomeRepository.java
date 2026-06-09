package eu.kalafatic.evolution.selfdev.genome.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact;

public class LocalGenomeRepository implements GenomeRepository {

    private final Map<String, GenomeArtifact> storage = new HashMap<>();

    @Override
    public void save(GenomeArtifact artifact) {
        storage.put(artifact.getId(), artifact);
    }

    @Override
    public Optional<GenomeArtifact> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<GenomeArtifact> findByTopic(String topic) {
        return storage.values().stream()
                .filter(a -> topic.equals(a.getTopic()))
                .toList();
    }

    @Override
    public List<GenomeArtifact> findAll() {
        return new ArrayList<>(storage.values());
    }
}
