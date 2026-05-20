package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Analyzer to ensure semantic diversity among Darwin variants.
 */
public class DarwinDiversityAnalyzer {

    /**
     * Filters a list of variants to remove semantic duplicates.
     */
    public List<JSONObject> analyze(List<JSONObject> variants, TaskContext context) {
        if (variants.size() < 2) return variants;

        List<JSONObject> unique = new ArrayList<>();
        for (JSONObject v : variants) {
            if (isUnique(v, unique)) {
                unique.add(v);
            } else {
                context.log("[DIVERSITY] Dropping duplicate variant: " + v.optString("strategy"));
            }
        }
        return unique;
    }

    private boolean isUnique(JSONObject candidate, List<JSONObject> existing) {
        String cStrategy = candidate.optString("strategy").toLowerCase();
        Set<String> cTargets = getActionTargets(candidate);

        for (JSONObject other : existing) {
            String oStrategy = other.optString("strategy").toLowerCase();
            Set<String> oTargets = getActionTargets(other);

            // Simple semantic check
            if (cStrategy.equals(oStrategy)) return false;

            // Action target overlap check
            if (!cTargets.isEmpty() && cTargets.equals(oTargets)) {
                // If they have the exact same file targets, they might be duplicates
                // especially if strategies are similar.
                if (computeSimilarity(cStrategy, oStrategy) > 0.8) return false;
            }
        }
        return true;
    }

    private Set<String> getActionTargets(JSONObject variant) {
        Set<String> targets = new HashSet<>();
        JSONArray actions = variant.optJSONArray("actions");
        if (actions != null) {
            for (int i = 0; i < actions.length(); i++) {
                targets.add(actions.getJSONObject(i).optString("target"));
            }
        }
        return targets;
    }

    private double computeSimilarity(String s1, String s2) {
        // Very basic Jaccard similarity for now
        Set<String> w1 = tokenize(s1);
        Set<String> w2 = tokenize(s2);
        if (w1.isEmpty() || w2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(w1);
        intersection.retainAll(w2);

        Set<String> union = new HashSet<>(w1);
        union.addAll(w2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String s) {
        Set<String> tokens = new HashSet<>();
        for (String word : s.split("\\s+")) {
            if (word.length() > 3) tokens.add(word);
        }
        return tokens;
    }
}
