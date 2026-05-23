package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Ranker for Darwin variants based on structural completeness and architectural awareness.
 */
public class DarwinFitnessRanker {

    /**
     * Ranks variants by fitness score.
     */
    public void rank(List<JSONObject> variants) {
        rank(variants, false);
    }

    /**
     * Ranks variants by fitness score with optional atomic priority.
     */
    public void rank(List<JSONObject> variants, boolean isAtomicRound) {
        for (JSONObject v : variants) {
            double score = calculateFitness(v);
            if (isAtomicRound && DarwinStrategyType.KEEPER_EVOLUTION.name().equals(v.optString("strategy_type"))) {
                score = Math.max(score, 0.95);
            }
            v.put("score", score);
        }

        variants.sort(Comparator.comparingDouble((JSONObject v) -> v.optDouble("score")).reversed());
    }

    private double calculateFitness(JSONObject variant) {
        double score = 0.5; // Base

        // 1. Structural Completeness
        if (variant.has("tradeoffs") && !variant.optString("tradeoffs").isEmpty()) score += 0.1;
        if (variant.has("failure_risks") && !variant.optString("failure_risks").isEmpty()) score += 0.1;
        if (variant.has("expected_effect")) score += 0.05;

        // 2. Action Specificity
        JSONArray actions = variant.optJSONArray("actions");
        if (actions != null && actions.length() > 0) {
            score += Math.min(0.2, actions.length() * 0.05);

            // Reward specific targets (not just '.')
            boolean specific = false;
            for (int i = 0; i < actions.length(); i++) {
                String target = actions.getJSONObject(i).optString("target");
                if (target != null && !target.equals(".") && !target.isEmpty()) {
                    specific = true;
                    break;
                }
            }
            if (specific) score += 0.05;
        }

        // 3. Strategy Role weighting (History-Aware)
        String type = variant.optString("strategy_type");
        if (DarwinStrategyType.KEEPER_EVOLUTION.name().equals(type)) score += 0.05; // Prefer evolving the winner
        if (DarwinStrategyType.SYNTHESIS_HYBRID.name().equals(type)) score += 0.03; // Slight preference for synthesis

        return Math.min(1.0, score);
    }
}
