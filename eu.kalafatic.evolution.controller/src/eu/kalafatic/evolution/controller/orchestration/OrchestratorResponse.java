package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified response from the orchestrator.
 */
public class OrchestratorResponse {
    private String summary;
    private ResultType resultType;
    private String content;
    private FinalResponse finalResponse;
    private Map<String, Object> metadata = new HashMap<>();
    private List<String> debugLogs = new ArrayList<>();

    public OrchestratorResponse() {}

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public FinalResponse getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(FinalResponse finalResponse) {
        this.finalResponse = finalResponse;
        if (finalResponse != null) {
            this.content = finalResponse.toString();
            this.summary = finalResponse.getSummary();
        }
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<String> getDebugLogs() {
        return debugLogs;
    }

    public void setDebugLogs(List<String> debugLogs) {
        this.debugLogs = debugLogs;
    }

    public boolean isSuccess() {
        return resultType != ResultType.ERROR;
    }
}
