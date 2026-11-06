package eu.kalafatic.evolution.controller.teip.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution request DTO sent by TRYST to EVO.
 */
public class TeipExecutionRequest {
    private String protocol = "TEIP";
    private String version = "1.0";
    private String requestId;
    private String correlationId;
    private String idempotencyKey;
    private String timestamp;
    private Map<String, Object> caller = new HashMap<>();
    private String capability;
    private String mode;
    private String instruction;
    private Map<String, Object> parameters = new HashMap<>();
    private List<String> grantedToolScopes = new ArrayList<>();
    private List<Map<String, Object>> contextReferences = new ArrayList<>();

    public TeipExecutionRequest() {}

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getCaller() { return caller; }
    public void setCaller(Map<String, Object> caller) { this.caller = caller != null ? caller : new HashMap<>(); }

    public String getCapability() { return capability; }
    public void setCapability(String capability) { this.capability = capability; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters != null ? parameters : new HashMap<>(); }

    public List<String> getGrantedToolScopes() { return grantedToolScopes; }
    public void setGrantedToolScopes(List<String> grantedToolScopes) { this.grantedToolScopes = grantedToolScopes != null ? grantedToolScopes : new ArrayList<>(); }

    public List<Map<String, Object>> getContextReferences() { return contextReferences; }
    public void setContextReferences(List<Map<String, Object>> contextReferences) { this.contextReferences = contextReferences != null ? contextReferences : new ArrayList<>(); }
}
