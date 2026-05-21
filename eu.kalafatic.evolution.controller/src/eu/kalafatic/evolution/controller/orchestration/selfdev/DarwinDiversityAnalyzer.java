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
        Set<String> cSteps = getProjectedSteps(candidate);
        Set<String> cEffects = getExpectedEffects(candidate);

        for (JSONObject other : existing) {
            String oStrategy = other.optString("strategy").toLowerCase();
            Set<String> oTargets = getActionTargets(other);
            Set<String> oSteps = getProjectedSteps(other);
            Set<String> oEffects = getExpectedEffects(other);

            // Simple semantic check
            if (cStrategy.equals(oStrategy)) return false;

            // Action target overlap check
            if (!cTargets.isEmpty() && cTargets.equals(oTargets)) {
                // If they have the exact same file targets, they might be duplicates
                // especially if strategies are similar.
                if (computeSimilarity(cStrategy, oStrategy) > 0.8) return false;
            }

            // Future Trajectory Overlap Check (Convergence vs Diversity)
            double stepSimilarity = computeJaccard(cSteps, oSteps);
            double effectSimilarity = computeJaccard(cEffects, oEffects);

            if (stepSimilarity > 0.7 && effectSimilarity > 0.7) {
                // They are proposing the same future, redundant.
                return false;
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

    private Set<String> getProjectedSteps(JSONObject variant) {
        Set<String> steps = new HashSet<>();
        JSONArray arr = variant.optJSONArray("projected_steps");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) steps.add(arr.getString(i).toLowerCase());
        }
        return steps;
    }

    private Set<String> getExpectedEffects(JSONObject variant) {
        Set<String> effects = new HashSet<>();
        JSONObject hyp = variant.optJSONObject("hypothesis");
        if (hyp != null) {
            JSONArray arr = hyp.optJSONArray("expected_effects");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) effects.add(arr.getString(i).toLowerCase());
            }
        }
        return effects;
    }

    private double computeJaccard(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);

        return (double) intersection.size() / union.size();
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
