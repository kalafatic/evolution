package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Immutable object representing frozen requirements after clarification.
 * Used to prevent requirement drift during generation.
 */
public final class ConfirmedRequirements {
    private final String goal;
    private final String language;
    private final String framework;
    private final List<String> constraints;
    private final String expectedOutput;
    private final String hash;
    private final int version;
    private final long timestamp;

    public ConfirmedRequirements(String goal, String language, String framework, List<String> constraints, String expectedOutput, int version) {
        this.goal = goal != null ? goal : "";
        this.language = language != null ? language : "";
        this.framework = framework != null ? framework : "";
        this.constraints = constraints != null ? Collections.unmodifiableList(new ArrayList<>(constraints)) : Collections.emptyList();
        this.expectedOutput = expectedOutput != null ? expectedOutput : "";
        this.version = version;
        this.timestamp = System.currentTimeMillis();
        this.hash = calculateHash();
    }

    private ConfirmedRequirements(String goal, String language, String framework, List<String> constraints, String expectedOutput, int version, long timestamp, String hash) {
        this.goal = goal;
        this.language = language;
        this.framework = framework;
        this.constraints = Collections.unmodifiableList(constraints);
        this.expectedOutput = expectedOutput;
        this.version = version;
        this.timestamp = timestamp;
        this.hash = hash;
    }

    public String getGoal() { return goal; }
    public String getLanguage() { return language; }
    public String getFramework() { return framework; }
    public List<String> getConstraints() { return constraints; }
    public String getExpectedOutput() { return expectedOutput; }
    public String getHash() { return hash; }
    public int getVersion() { return version; }
    public long getTimestamp() { return timestamp; }

    private String calculateHash() {
        return Integer.toHexString(Objects.hash(goal, language, framework, constraints, expectedOutput));
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("goal", goal);
        json.put("language", language);
        json.put("framework", framework);
        json.put("constraints", new JSONArray(constraints));
        json.put("expectedOutput", expectedOutput);
        json.put("hash", hash);
        json.put("version", version);
        json.put("timestamp", timestamp);
        return json;
    }

    public static ConfirmedRequirements fromJSON(JSONObject json) {
        if (json == null) return null;

        List<String> constraints = new ArrayList<>();
        JSONArray arr = json.optJSONArray("constraints");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                constraints.add(arr.getString(i));
            }
        }

        return new ConfirmedRequirements(
            json.optString("goal", ""),
            json.optString("language", ""),
            json.optString("framework", ""),
            constraints,
            json.optString("expectedOutput", ""),
            json.optInt("version", 1),
            json.optLong("timestamp", System.currentTimeMillis()),
            json.optString("hash", "")
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- FROZEN REQUIREMENTS (v").append(version).append(", hash:").append(hash).append(") ---\n");
        sb.append("Goal: ").append(goal).append("\n");
        sb.append("Language: ").append(language).append("\n");
        sb.append("Framework: ").append(framework).append("\n");
        if (!constraints.isEmpty()) {
            sb.append("Constraints: ").append(String.join(", ", constraints)).append("\n");
        }
        if (!expectedOutput.isEmpty()) {
            sb.append("Expected Output: ").append(expectedOutput).append("\n");
        }
        return sb.toString();
    }
}
