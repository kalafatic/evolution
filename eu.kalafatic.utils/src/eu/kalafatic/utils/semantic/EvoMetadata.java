package eu.kalafatic.utils.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal AI Metadata POJO for all artifact types.
 */
public class EvoMetadata {
    private String id;
    private String path;
    private String domain;
    private String role;
    private String purpose;
    private List<String> inputs = new ArrayList<>();
    private List<String> outputs = new ArrayList<>();
    private String stability = "EVOLVING";
    private String evolutionaryImpact = "MEDIUM";
    private boolean stale = false;
    private Map<String, Object> customAttributes = new HashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public List<String> getInputs() { return inputs; }
    public void setInputs(List<String> inputs) { this.inputs = inputs; }

    public List<String> getOutputs() { return outputs; }
    public void setOutputs(List<String> outputs) { this.outputs = outputs; }

    public String getStability() { return stability; }
    public void setStability(String stability) { this.stability = stability; }

    public String getEvolutionaryImpact() { return evolutionaryImpact; }
    public void setEvolutionaryImpact(String evolutionaryImpact) { this.evolutionaryImpact = evolutionaryImpact; }

    public Map<String, Object> getCustomAttributes() { return customAttributes; }
    public void setCustomAttributes(Map<String, Object> customAttributes) { this.customAttributes = customAttributes; }

    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }
}
