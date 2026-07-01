package eu.kalafatic.evolution.controller.orchestration.selfdev.adaptive;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * Scores architectural and reasoning patterns negatively if they repeatedly appear in rejected variants.
 */
public class EvolutionaryPenaltyModel {
    private final Map<String, Double> patternPenalties = new HashMap<>();
    private static final double REJECTION_INCREMENT = 0.2;
    private static final double SUCCESS_DECREMENT = 0.1;

    public void updateFromAnalysis(JSONObject analysis) {
        if (analysis == null) return;

        org.json.JSONArray avoid = analysis.optJSONArray("avoidGuidelines");
        if (avoid != null) {
            for (int i = 0; i < avoid.length(); i++) {
                String pattern = avoid.getString(i).toLowerCase();
                patternPenalties.put(pattern, patternPenalties.getOrDefault(pattern, 0.0) + REJECTION_INCREMENT);
            }
        }
    }

    public double getPenalty(String pattern) {
        String lower = pattern.toLowerCase();
        double max = 0.0;
        for (Map.Entry<String, Double> entry : patternPenalties.entrySet()) {
            if (lower.contains(entry.getKey())) {
                max = Math.max(max, entry.getValue());
            }
        }
        return Math.min(max, 1.0);
    }

    public Map<String, Double> getActivePenalties() {
        return new HashMap<>(patternPenalties);
    }
}
