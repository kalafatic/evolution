package eu.kalafatic.evolution.supervisor.bootstrap;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SupervisorCommand {
    private final String type;
    private final Map<String, Object> parameters = new HashMap<>();

    @JsonCreator
    public SupervisorCommand(@JsonProperty("type") String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
}
