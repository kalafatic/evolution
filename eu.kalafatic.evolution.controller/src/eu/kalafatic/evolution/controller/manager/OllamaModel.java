package eu.kalafatic.evolution.controller.manager;

/**
 * Represents a model available in Ollama.
 */
public class OllamaModel {
    private String name;
    private long size;
    private String modifiedAt;

    public OllamaModel(String name, long size) {
        this(name, size, "");
    }

    public OllamaModel(String name, long size, String modifiedAt) {
        this.name = name;
        this.size = size;
        this.modifiedAt = modifiedAt;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }
}
