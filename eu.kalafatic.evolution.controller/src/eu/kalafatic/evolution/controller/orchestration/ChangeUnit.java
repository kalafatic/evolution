package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents a "unit of change" in the evolution loop.
 * Wraps files, diff or content, and traceability reason.
 *
 * @evo:21:A reason=introduce-change-unit
 */
public class ChangeUnit {
    private List<String> files = new ArrayList<>();
    private String patch; // Can be a diff or full content
    private String reason; // e.g., @evo reference

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("files", new JSONArray(files));
        json.put("patch", patch);
        json.put("reason", reason);
        return json;
    }
}
