package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;

/**
 * Ranker for Darwin trajectories based on structural completeness and architectural divergence.
 */
public class DarwinFitnessRanker {

    /**
     * Ranks variants by fitness score with atomic intent awareness.
     * Centralized fitness orchestration logic.
     */
    public void rank(List<JSONObject> variants, GoalModel goal, SemanticEnvelope envelope, AtomicIntentAnalysis atomic, int generation, EvolutionaryPressureVector pressure) {
        for (JSONObject v : variants) {
            // Stage 1: Goal Alignment (Mandatory Semantic Anchoring)
            double semanticDistance = calculateSemanticDistance(goal, v, envelope);
            v.put("semantic_distance", semanticDistance);
            double goalSatisfaction = 1.0 - semanticDistance;

            // Stage 2: Pure Quality Score
            FitnessRecord fitness = scoreQuality(v, generation, atomic);
            fitness.setGoalSatisfaction(goalSatisfaction);

            // STABILIZATION: Goal Alignment accounts for 80% of the total fitness score.
            // This ensures semantic continuity and prevents over-engineering from surviving if it drifts from the goal.
            double qualityScore = fitness.getTotalScore();
            double finalScore = (0.8 * goalSatisfaction) + (0.2 * qualityScore);

            fitness.setTotalScore(finalScore);
            v.put("fitness_record", fitness);

            // Stage 3: Evolutionary Pressure (Scope, Pressure Vector)
            finalScore = applyPressure(v, finalScore, atomic, pressure);

            // Stage 4: Constraint Gates
            finalScore = applyGates(v, finalScore, atomic);

            v.put("score", finalScore);
        }

        // Stage 5: Selection (Sorting)
        sortSelection(variants);
    }

    @Deprecated
    public void rank(List<JSONObject> variants, AtomicIntentAnalysis atomic, int generation, EvolutionaryPressureVector pressure) {
        rank(variants, null, null, atomic, generation, pressure);
    }

    private FitnessRecord scoreQuality(JSONObject variant, int generation, AtomicIntentAnalysis atomic) {
        return calculateFitnessRecord(variant, generation, null, atomic);
    }

    public double calculateSemanticDistance(GoalModel goal, JSONObject variant, SemanticEnvelope envelope) {
        if (goal == null) return 0.5; // Default if goal is missing during legacy ranking

        String strategy = variant.optString("strategy", "").toLowerCase();
        String philosophy = variant.optString("semantic_anchor", "").toLowerCase();
        String primaryAction = goal.getPrimaryAction().toLowerCase();

        // Technical keywords that are often semantically identical for the same goal
        String[] identicalTechnicalConcepts = {"static", "instance", "constructor", "overloads", "logger", "system.out", "varargs", "library", "utility"};

        double distance = 0.0;

        // 1. Mandatory Concepts Check (Goal Relative)
        if (envelope != null && !envelope.getMandatoryConcepts().isEmpty()) {
            int missed = 0;
            for (String concept : envelope.getMandatoryConcepts()) {
                String c = concept.toLowerCase();
                // If it's a technical variety keyword, we are lenient
                boolean isTechnicalVariety = false;
                for (String tech : identicalTechnicalConcepts) {
                    if (c.contains(tech)) { isTechnicalVariety = true; break; }
                }

                if (!strategy.contains(c) && !philosophy.contains(c)) {
                    if (!isTechnicalVariety) missed++;
                }
            }
            distance += (double) missed / envelope.getMandatoryConcepts().size() * 0.4;
        }

        // 2. Exact Match or Intent Overlap
        if (strategy.contains(primaryAction) || philosophy.contains(primaryAction)) {
            distance += 0.0; // Perfect intent match
        } else {
            // 3. Keyword Overlap (Weighted toward intent keywords)
            String[] keywords = primaryAction.split(" ");
            int matches = 0;
            int significantKeywords = 0;
            for (String k : keywords) {
                if (k.length() <= 3) continue;

                boolean isTechnical = false;
                for (String tech : identicalTechnicalConcepts) {
                    if (k.equalsIgnoreCase(tech)) { isTechnical = true; break; }
                }

                if (!isTechnical) {
                    significantKeywords++;
                    if (strategy.contains(k) || philosophy.contains(k)) {
                        matches++;
                    }
                }
            }
            double overlap = significantKeywords > 0 ? (double) matches / significantKeywords : 1.0;
            distance += (1.0 - overlap) * 0.6;
        }

        // 4. Forbidden Regions Check (Architectural Inflation)
        if (envelope != null && !envelope.getForbiddenRegions().isEmpty()) {
            for (String region : envelope.getForbiddenRegions()) {
                String r = region.toLowerCase();
                if (strategy.contains(r) || philosophy.contains(r)) {
                    distance += 0.8; // Heavy penalty for architectural inflation
                }
            }
        }

        return Math.min(1.0, distance);
    }

    private double applyPressure(JSONObject variant, double score, AtomicIntentAnalysis atomic, EvolutionaryPressureVector pressure) {
        // SCOPE PRESSURE: Smoother adaptive pressure for over-engineering
        double scopeRatio = calculateScopeRatio(variant, atomic);
        variant.put("scope_ratio", scopeRatio);

        if (scopeRatio > 1.1) {
            // Smoother sigmoid-like penalty: starts slow, accelerates
            double penalty = 1.0 / (1.0 + Math.exp(5.0 * (scopeRatio - 2.0)));
            score *= penalty;
            variant.put("scope_penalty", 1.0 - penalty);
        }

        if (pressure != null) {
            score += (pressure.getTotalPressure() * 0.05);
        }

        return Math.min(1.0, score);
    }

    private double applyGates(JSONObject variant, double score, AtomicIntentAnalysis atomic) {
        double scopeRatio = variant.optDouble("scope_ratio", 1.0);

        // Massive over-engineering gate
        if (scopeRatio > 4.0) {
            variant.put("rejected_reason", "Massive scope inflation: " + String.format("%.2f", scopeRatio));
            variant.put("fitness_gate", "REJECTED_SCOPE_INFLATION");
            return 0.0;
        }

        // Fallback exclusion
        String id = variant.optString("id", "");
        if (id.contains("fallback-")) {
            variant.put("isControlArtifact", true);
            return Math.min(score, 0.05);
        }

        return score;
    }

    private void sortSelection(List<JSONObject> variants) {
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

        // 1. Genome B: File Selection & Context Optimization (4-16 files)
        JSONArray files = med.optJSONArray("selected_files");
        if (files != null) {
            int count = files.length();
            if (count >= 4 && count <= 16) {
                medScore += 0.3; // Reward ideal range (Genome B health)
            } else if (count > 0 && count < 4) {
                medScore -= 0.2; // Penalize insufficient context
            } else if (count > 16) {
                medScore -= Math.min(0.6, (count - 16) * 0.1); // Stronger penalty for context bloat
            } else if (count == 0) {
                medScore -= 0.8; // Heavy penalty for empty packages
            }
        }

        // 2. Genome A: Prompt Quality & Implementation Probability
        String prompt = med.optString("prompt");
        if (prompt.length() > 200) medScore += 0.1;
        if (prompt.toLowerCase().contains("reasoning") || prompt.toLowerCase().contains("constraints")) medScore += 0.05;
        if (prompt.toLowerCase().contains("architecture") || prompt.toLowerCase().contains("workflow")) medScore += 0.05;

        // 3. Information Density (Understanding / Context Ratio)
        String summary = med.optString("architecture_summary");
        if (summary.length() > 100) medScore += 0.1;
        if (files != null && files.length() > 0) {
            double ratio = (double) summary.length() / files.length();
            if (ratio > 80) medScore += 0.15; // High signal per file
            else if (ratio < 20) medScore -= 0.1; // Redundant or low-signal context
        }

        // 4. Dependency & Workflow Coverage
        String deps = med.optString("dependencies").toLowerCase();
        if (deps.length() > 50) medScore += 0.05;
        if (deps.contains("interface") || deps.contains("api") || deps.contains("entry")) medScore += 0.05;

        String instructions = med.optString("execution_instructions").toLowerCase();
        if (instructions.length() > 100) medScore += 0.05;
        if (instructions.contains("implement") || instructions.contains("verify")) medScore += 0.05;

        // 5. Semantic Authority (Structural Description vs Generic Labels)
        String lowerSummary = summary.toLowerCase();
        if (lowerSummary.contains("java") || lowerSummary.contains("maven") || lowerSummary.contains("project")) {
            medScore -= 0.1; // Discourage generic technology labels
        }
        if (lowerSummary.contains("coordinator") || lowerSummary.contains("topology") || lowerSummary.contains("bottleneck") || lowerSummary.contains("orchestration")) {
            medScore += 0.15; // Reward high-signal architectural concepts
        }

        // 6. Hallucination Risk (Self-Evaluation Merit)
        String evaluation = med.optString("evaluation").toLowerCase();
        if (evaluation.contains("missing") || evaluation.contains("gap") || evaluation.contains("uncertainty")) {
            medScore += 0.1; // Reward honesty about architectural gaps (reduces hallucination risk)
        }

        return medScore;
    }
}
