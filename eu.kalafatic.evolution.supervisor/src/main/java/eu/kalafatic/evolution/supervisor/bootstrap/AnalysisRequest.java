package eu.kalafatic.evolution.supervisor.bootstrap;

import java.util.HashMap;
import java.util.Map;

public class AnalysisRequest {
    private final Map<String, String> inputs = new HashMap<>();

    public void addInput(String source, String content) {
        inputs.put(source, content);
    }

    public Map<String, String> getInputs() {
        return inputs;
    }
}
