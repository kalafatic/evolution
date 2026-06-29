package eu.kalafatic.evolution.supervisor.bootstrap;

import java.util.HashMap;
import java.util.Map;

public class SupervisorResponse {
    private boolean success;
    private String message;
    private final Map<String, Object> data = new HashMap<>();

    public SupervisorResponse() {}

    public SupervisorResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
}
