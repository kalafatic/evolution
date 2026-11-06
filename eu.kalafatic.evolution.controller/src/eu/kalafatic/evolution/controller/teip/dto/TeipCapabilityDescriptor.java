package eu.kalafatic.evolution.controller.teip.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capability descriptor catalog DTO for TEIP 1.0.
 */
public class TeipCapabilityDescriptor {
    private String protocol = "TEIP";
    private String version = "1.0";
    private String timestamp = java.time.Instant.now().toString();
    private List<String> supportedModes = new ArrayList<>();
    private List<Map<String, Object>> agents = new ArrayList<>();
    private List<Map<String, Object>> models = new ArrayList<>();
    private List<Map<String, Object>> tools = new ArrayList<>();
    private Map<String, Boolean> features = new HashMap<>();

    public TeipCapabilityDescriptor() {
        features.put("asyncExecution", true);
        features.put("eventStreaming", true);
        features.put("cancellation", true);
    }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public List<String> getSupportedModes() { return supportedModes; }
    public void setSupportedModes(List<String> supportedModes) { this.supportedModes = supportedModes != null ? supportedModes : new ArrayList<>(); }

    public List<Map<String, Object>> getAgents() { return agents; }
    public void setAgents(List<Map<String, Object>> agents) { this.agents = agents != null ? agents : new ArrayList<>(); }

    public List<Map<String, Object>> getModels() { return models; }
    public void setModels(List<Map<String, Object>> models) { this.models = models != null ? models : new ArrayList<>(); }

    public List<Map<String, Object>> getTools() { return tools; }
    public void setTools(List<Map<String, Object>> tools) { this.tools = tools != null ? tools : new ArrayList<>(); }

    public Map<String, Boolean> getFeatures() { return features; }
    public void setFeatures(Map<String, Boolean> features) { this.features = features != null ? features : new HashMap<>(); }
}
