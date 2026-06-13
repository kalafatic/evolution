# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.selfdev.genome/src/eu/kalafatic/evolution/selfdev/genome/repository/

## Domain: general

## Components
* `LocalGenomeRepository.java`: package eu.kalafatic.evolution.selfdev.genome.repository; import java.util.ArrayList; import java.util.HashMap; import java.util.List; import java.util.Map; import java.util.Optional; import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact; public class LocalGenomeRepository implements GenomeRepository { private final Map<String, GenomeArtifact> storage = new HashMap<>(); @Override public void save(GenomeArtifact artifact) { storage.put(artifact.getId(), artifact); } @Override public Optional<GenomeArtifact> findById(String id) { return Optional.ofNullable(storage.get(id)); } @Override public List<GenomeArtifact> findByTopic(String topic) { return storage.values().stream()
* `GenomeRepository.java`: package eu.kalafatic.evolution.selfdev.genome.repository; import java.util.List; import java.util.Optional; import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact; public interface GenomeRepository { void save(GenomeArtifact artifact); Optional<GenomeArtifact> findById(String id); List<GenomeArtifact> findByTopic(String topic); List<GenomeArtifact> findAll(); }
