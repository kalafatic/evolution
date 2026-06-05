package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Lightweight structure for architectural context, rules and focus.
 * Replacing large text loads of architecture.md.
 *
 * @evo:21:A reason=structured-architecture-context
 */
public class ArchitectureContext {
    private List<String> keyRules = new ArrayList<>();
    private String currentFocus;

    public List<String> getKeyRules() {
        return keyRules;
    }

    public void setKeyRules(List<String> keyRules) {
        this.keyRules = keyRules;
    }

    public String getCurrentFocus() {
        return currentFocus;
    }

    public void setCurrentFocus(String currentFocus) {
        this.currentFocus = currentFocus;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("keyRules", new JSONArray(keyRules));
        json.put("currentFocus", currentFocus);
        return json;
    }
}
