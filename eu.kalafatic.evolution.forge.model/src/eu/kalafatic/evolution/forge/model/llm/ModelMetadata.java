package eu.kalafatic.evolution.forge.model.llm;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Model metadata container for native EVO models.
 */
public class ModelMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String version = "1.0.0";
    private String architectureName = "evo_llm";
    private long createdAt;
    private Map<String, String> attributes = new HashMap<>();

    public ModelMetadata() {
        this.createdAt = System.currentTimeMillis();
    }

    public ModelMetadata(String name, String version, String architectureName) {
        this();
        this.name = name;
        if (version != null) this.version = version;
        if (architectureName != null) this.architectureName = architectureName;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getArchitectureName() { return architectureName; }
    public void setArchitectureName(String architectureName) { this.architectureName = architectureName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttribute(String key, String value) {
        if (key != null) {
            this.attributes.put(key, value);
        }
    }

    public String getAttribute(String key) {
        return this.attributes.get(key);
    }
}
