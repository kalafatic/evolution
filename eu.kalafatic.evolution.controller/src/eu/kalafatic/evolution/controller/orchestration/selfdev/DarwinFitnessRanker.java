package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Ranker for Darwin trajectories based on structural completeness and architectural divergence.
 */
public class DarwinFitnessRanker {

    /**
     * Ranks variants by fitness score.
     */
    public void rank(List<JSONObject> variants) {
        rank(variants, false, 0);
    }

    /**
     * Ranks variants by fitness score with optional atomic priority.
     */
    public void rank(List<JSONObject> variants, boolean isAtomicRound) {
        rank(variants, isAtomicRound, 0);
    }

    /**
     * Ranks variants by fitness score with optional atomic priority.
     */
    public void rank(List<JSONObject> variants, boolean isAtomicRound, int generation) {
        rank(variants, isAtomicRound, generation, null);
    }

    /**
     * Ranks variants by fitness score with pressure awareness.
     */
    public void rank(List<JSONObject> variants, boolean isAtomicRound, int generation, EvolutionaryPressureVector pressure) {
        for (JSONObject v : variants) {
            double score = calculateFitness(v, generation, pressure);
            if (isAtomicRound && DarwinStrategyType.PROBABLE_SURVIVOR.name().equals(v.optString("strategy_type"))) {
                score = Math.max(score, 0.95);
            }

            v.put("score", score);
        }

        variants.sort(Comparator.comparingDouble((JSONObject v) -> v.optDouble("score")).reversed());
    }

    private double calculateFitness(JSONObject variant, int generation, EvolutionaryPressureVector pressure) {
        double score = 0.4; // Base

        if (pressure != null) {
            // Adjust base score based on pressure intensity
            score += (pressure.getTotalPressure() * 0.1);
        }

        // 0. Specialized Mediation Fitness (High Density focus)
        if (variant.has("mediation_candidate")) {
            score += calculateMediationFitness(variant.optJSONObject("mediation_candidate"));
        }

        // 1. Structural Completeness
        if (variant.has("tradeoffs") && variant.optString("tradeoffs").length() > 20) score += 0.1;
        if (variant.has("failure_risks") && variant.optString("failure_risks").length() > 20) score += 0.1;
        if (variant.has("semantic_justification") && variant.optString("semantic_justification").length() > 20) score += 0.1;
        if (variant.has("expected_effect")) score += 0.05;

        // 2. Action Specificity
        JSONArray actions = variant.optJSONArray("actions");
        if (actions != null && actions.length() > 0) {
            score += Math.min(0.2, actions.length() * 0.05);

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

        // 3. Trajectory Type weighting (Balanced with generational pressure)
        String type = variant.optString("strategy_type");
        if (DarwinStrategyType.PROBABLE_SURVIVOR.name().equals(type)) score += 0.05;
        if (DarwinStrategyType.PHILOSOPHY_MUTATION.name().equals(type)) score += 0.05;
        if (DarwinStrategyType.MAXIMAL_DIVERGENCE.name().equals(type)) score += 0.04;

        // Increase value of stabilization in later generations (Convergence Pressure)
        if (DarwinStrategyType.STABILIZATION_RECOVERY.name().equals(type)) {
            score += 0.03 + (generation * 0.02);
        }

        return Math.min(1.0, score);
    }

    private double calculateMediationFitness(JSONObject med) {
        if (med == null) return 0.0;
        double medScore = 0.0;

        // 1. File Selection Count (Sweet spot: 4-16 files)
        JSONArray files = med.optJSONArray("selected_files");
        if (files != null) {
            int count = files.length();
            if (count >= 4 && count <= 16) {
                medScore += 0.2; // Reward ideal range
            } else if (count > 0 && count < 4) {
                medScore += 0.05; // Penalize too small
            } else if (count > 16) {
                medScore -= Math.min(0.3, (count - 16) * 0.02); // Penalize bloat aggressively
            }
        }

        // 2. Information Density (Completeness of summaries)
        if (med.optString("architecture_summary").length() > 50) medScore += 0.05;
        if (med.optString("dependencies").length() > 30) medScore += 0.05;
        if (med.optString("execution_instructions").length() > 50) medScore += 0.05;
        if (med.optString("prompt").length() > 100) medScore += 0.05;

        // 3. Signal to Noise (Self-evaluation)
        String evaluation = med.optString("evaluation").toLowerCase();
        if (evaluation.contains("density") || evaluation.contains("signal") || evaluation.contains("distilled")) {
            medScore += 0.05;
        }

        return medScore;
    }
}
