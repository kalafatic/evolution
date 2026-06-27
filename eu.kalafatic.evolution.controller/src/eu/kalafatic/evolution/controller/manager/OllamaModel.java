package eu.kalafatic.evolution.controller.manager;

/**
 * Represents a model available in Ollama.
 */
public class OllamaModel {
    private String name;
    private long size;

    public OllamaModel(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }
}
