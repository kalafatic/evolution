package eu.kalafatic.evolution.controller.teip.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check response DTO for TEIP 1.0.
 */
public class TeipHealthResponse {
    private String protocol = "TEIP";
    private String version = "1.0";
    private String status = "UP";
    private String application = "EVOLUTION";
    private String appVersion = "2.6.5";
    private String timestamp = java.time.Instant.now().toString();
    private boolean ready = true;
    private Map<String, Object> details = new HashMap<>();

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details != null ? details : new HashMap<>(); }
}
