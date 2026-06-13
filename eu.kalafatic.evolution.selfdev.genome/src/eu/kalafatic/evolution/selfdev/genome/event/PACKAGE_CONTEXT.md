# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.selfdev.genome/src/eu/kalafatic/evolution/selfdev/genome/event/

## Domain: general

## Components
* `GenomeEvent.java`: package eu.kalafatic.evolution.selfdev.genome.event; public class GenomeEvent { private String type; private String artifactId; private String topic; public GenomeEvent(String type, String artifactId, String topic) { this.type = type; this.artifactId = artifactId; this.topic = topic; } public String getType() { return type; } public String getArtifactId() { return artifactId; } public String getTopic() { return topic; } }
* `GenomeEventBus.java`: package eu.kalafatic.evolution.selfdev.genome.event; public interface GenomeEventBus { void publish(GenomeEvent event); void subscribe(GenomeEventListener listener); }
* `GenomeEventListener.java`: package eu.kalafatic.evolution.selfdev.genome.event; public interface GenomeEventListener { void onEvent(GenomeEvent event); }
