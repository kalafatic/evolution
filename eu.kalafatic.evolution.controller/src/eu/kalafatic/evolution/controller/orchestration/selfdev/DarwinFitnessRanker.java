package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;

/**
 * Ranker for Darwin trajectories based on structural completeness and architectural divergence.
 */
public class DarwinFitnessRanker {

    /**
     * Ranks variants by fitness score.
     */
    public void rank(List<JSONObject> variants) {
        rank(variants, (AtomicIntentAnalysis)null, 0, null);
    }

    /**
     * Ranks variants by fitness score with optional atomic priority.
     */
    public void rank(List<JSONObject> variants, boolean isAtomicRound) {
        rank(variants, isAtomicRound, 0, null);
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
        AtomicIntentAnalysis synthetic = null;
        if (isAtomicRound) {
            synthetic = new AtomicIntentAnalysis();
            synthetic.setAtomic(true);
            synthetic.getComplexityVector().determinismConfidence = 0.95;
        }
        rank(variants, synthetic, generation, pressure);
    }

    /**
     * Ranks variants by fitness score with atomic intent awareness.
     */
    public void rank(List<JSONObject> variants, AtomicIntentAnalysis atomic, int generation, EvolutionaryPressureVector pressure) {
        boolean isAtomicRound = atomic != null && atomic.isAtomic() && atomic.getComplexityVector().determinismConfidence > 0.8;
        for (JSONObject v : variants) {
            FitnessRecord fitness = calculateFitnessRecord(v, generation, pressure, atomic);
            double score = fitness.getTotalScore();
            v.put("fitness_record", fitness); // Store for wrapping later

            // SCOPE PRESSURE: Gradual penalty for over-engineering
            double scopeRatio = calculateScopeRatio(v, atomic);
            if (scopeRatio > 1.2) {
                double penalty = Math.max(0.1, 1.0 - (scopeRatio - 1.2) * 0.5);
                score *= penalty;
                v.put("scope_penalty", 1.0 - penalty);
            }

            // Hard scope gate
            if (scopeRatio > 3.0) {
                score = 0.0;
                v.put("rejected_reason", "Scope ratio exceeded: " + String.format("%.2f", scopeRatio));
                v.put("fitness_gate", "REJECTED_SCOPE_INFLATION");
            }

            // Fallback exclusion
            String id = v.optString("id", "");
            if (id.contains("fallback-")) {
                score = Math.min(score, 0.05); // Scaffolding only, cannot win
                v.put("isControlArtifact", true);
            }

            if (isAtomicRound && DarwinStrategyType.PROBABLE_SURVIVOR.name().equals(v.optString("strategy_type"))) {
                if (score > 0) {
                    score = Math.max(score, 0.95);
                }
            }

            v.put("score", score);
            v.put("scope_ratio", scopeRatio);
        }

        variants.sort(Comparator.comparingDouble((JSONObject v) -> v.optDouble("score")).reversed());
    }

    private FitnessRecord calculateFitnessRecord(JSONObject variant, int generation, EvolutionaryPressureVector pressure, AtomicIntentAnalysis atomic) {
        FitnessRecord fr = new FitnessRecord();

        // 1. Correctness (40%) - structural completeness & action specificity
        double correctness = 0.5;
        if (variant.has("tradeoffs")) correctness += 0.1;
        if (variant.has("failure_risks")) correctness += 0.1;
        JSONArray actions = variant.optJSONArray("actions");
        if (actions != null && actions.length() > 0) {
            correctness += 0.2;
            boolean hasTarget = false;
            for (int i = 0; i < actions.length(); i++) {
                if (!actions.getJSONObject(i).optString("target", ".").equals(".")) hasTarget = true;
            }
            if (hasTarget) correctness += 0.1;
        }
        correctness = Math.min(1.0, correctness);
        fr.setImplementationCompleteness(correctness);

        // 2. Simplicity (20%) - anti-complexity
        double simplicity = 1.0;
        if (actions != null && actions.length() > 4) simplicity -= 0.2;
        JSONObject dims = variant.optJSONObject("engineering_dimensions");
        if (dims != null) {
            if ("high".equalsIgnoreCase(dims.optString("abstraction_depth"))) simplicity -= 0.3;
            if ("service".equalsIgnoreCase(dims.optString("execution_model"))) simplicity -= 0.2;
        }
        simplicity = Math.max(0.0, simplicity);

        // 3. Extensibility (20%)
        double extensibility = 0.5;
        if (dims != null) {
            String ext = dims.optString("extensibility");
            if ("high".equalsIgnoreCase(ext)) extensibility = 1.0;
            else if ("medium".equalsIgnoreCase(ext)) extensibility = 0.7;
        }

        // 4. Performance (10%)
        double performance = 0.5;
        if (dims != null) {
            if ("async".equalsIgnoreCase(dims.optString("runtime_behavior"))) performance = 0.8;
            if ("reactive".equalsIgnoreCase(dims.optString("execution_model"))) performance = 0.9;
        }

        // 5. Maintainability (10%)
        double maintainability = 0.5;
        if (dims != null) {
            if ("modular".equalsIgnoreCase(dims.optString("modularity_approach"))) maintainability = 0.9;
        }
        fr.setArchitecturalQuality((0.2 * extensibility) + (0.1 * performance) + (0.1 * maintainability) + (0.2 * simplicity));

        double total = (0.4 * correctness) + (0.2 * simplicity) + (0.2 * extensibility) + (0.1 * performance) + (0.1 * maintainability);

        if (pressure != null) {
            total += (pressure.getTotalPressure() * 0.05);
        }

        if (variant.has("mediation_candidate")) {
            total += calculateMediationFitness(variant.optJSONObject("mediation_candidate")) * 0.1;
        }

        fr.setGoalSatisfaction(atomic != null && atomic.isAtomic() ? 0.9 : 0.7);
        fr.setTotalScore(Math.min(1.0, total));

        return fr;
    }

    private double calculateScopeRatio(JSONObject variant, AtomicIntentAnalysis atomic) {
        if (atomic == null) return 1.0;

        double problemComplexity = 1.0;
        if (!atomic.isAtomic()) problemComplexity += 1.0;
        if (atomic.isMultiStep()) problemComplexity += 1.0;

        double solutionComplexity = 1.0;
        JSONArray actions = variant.optJSONArray("actions");
        if (actions != null) {
            solutionComplexity += (actions.length() * 0.5);
        }

        JSONObject dims = variant.optJSONObject("engineering_dimensions");
        if (dims != null) {
            if ("high".equalsIgnoreCase(dims.optString("abstraction_depth"))) solutionComplexity += 2.0;
            if ("modular".equalsIgnoreCase(dims.optString("modularity_approach"))) solutionComplexity += 1.0;
            if ("service".equalsIgnoreCase(dims.optString("execution_model"))) solutionComplexity += 2.0;
            if ("reactive".equalsIgnoreCase(dims.optString("execution_model"))) solutionComplexity += 1.5;
        }

        return solutionComplexity / problemComplexity;
    }

    private double calculateMediationFitness(JSONObject med) {
        if (med == null) return 0.0;
        double medScore = 0.0;

        // 1. File Selection Constraint (4-16 files)
        JSONArray files = med.optJSONArray("selected_files");
        if (files != null) {
            int count = files.length();
            if (count >= 4 && count <= 16) {
                medScore += 0.2; // Reward ideal range
            } else if (count > 0 && count < 4) {
                medScore -= 0.15; // Penalize insufficient context
            } else if (count > 16) {
                medScore -= Math.min(0.5, (count - 16) * 0.05); // Penalize bloat
            } else if (count == 0) {
                medScore -= 0.5; // Fatal penalty for empty packages
            }
        }

        // 2. Information Density (Abstract completeness)
        if (med.optString("architecture_summary").length() > 50) medScore += 0.05;
        if (med.optString("dependencies").length() > 30) medScore += 0.05;
        if (med.optString("execution_instructions").length() > 50) medScore += 0.05;
        if (med.optString("prompt").length() > 100) medScore += 0.05;

        // 3. Significance Merit (Self-justification of importance)
        String evaluation = med.optString("evaluation").toLowerCase();
        if (evaluation.contains("density") || evaluation.contains("centrality") || evaluation.contains("influence") || evaluation.contains("connectivity")) {
            medScore += 0.1; // Reward candidates that justify selection via emergent properties
        }

        // 4. Divergence Penalty (Penalize generic technology labels in favor of structural descriptions)
        String summary = med.optString("architecture_summary").toLowerCase();
        if (summary.contains("java project") || summary.contains("arduino sketch") || summary.contains("spring boot") || summary.contains("maven") || summary.contains("gradle")) {
            medScore -= 0.1; // Discourage hardcoded technology labels (Low Signal)
        }
        if (summary.contains("coordinator") || summary.contains("entry") || summary.contains("topology") || summary.contains("bottleneck") || summary.contains("flow") || summary.contains("core")) {
            medScore += 0.1; // Reward structural/topological descriptions (High Signal)
        }

        // 5. Ratio Optimization (Understanding / Context)
        JSONArray selectedFiles = med.optJSONArray("selected_files");
        if (selectedFiles != null && selectedFiles.length() > 0) {
            double ratio = (double) med.optString("architecture_summary").length() / selectedFiles.length();
            if (ratio > 50) medScore += 0.1; // High information density per file
        }

        return medScore;
    }
}
