package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Analyzer to ensure conceptual and architectural diversity among Darwin trajectories.
 */
public class DarwinDiversityAnalyzer {

    /**
     * Filters a list of variants to remove semantic and conceptual duplicates.
     */
    public List<JSONObject> analyze(List<JSONObject> variants, TaskContext context) {
        return analyze(variants, null, context);
    }

    /**
     * Filters a list of variants and ensures they adhere to their blueprints.
     */
    public List<JSONObject> analyze(List<JSONObject> variants, List<TrajectoryBlueprint> blueprints, TaskContext context) {
        if (variants.size() < 2) return variants;

        List<JSONObject> unique = new ArrayList<>();
        for (JSONObject v : variants) {
            // Blueprint Adherence Check
            TrajectoryBlueprint bp = null;
            if (blueprints != null) {
                bp = blueprints.stream().filter(b -> b.getId().equals(v.optString("id"))).findFirst().orElse(null);
                if (bp != null && !adheresToBlueprint(v, bp, context)) {
                    context.log("[DIVERSITY] Blueprint Violation detected for: " + v.optString("id") + ". Rejecting variant.");
                    continue; // REJECT variants that violate blueprint constraints
                }
            }

            if (isUnique(v, unique)) {
                unique.add(v);
            } else {
                if (bp != null) {
                    context.log("[DIVERSITY] MANDATORY BRANCH REDUNDANCY: " + v.optString("id") + " is too similar to siblings. Preserving but flagging.");
                    unique.add(v); // MANDATORY branches must survive
                } else {
                    context.log("[DIVERSITY] Dropping redundant trajectory: " + v.optString("strategy"));
                }
            }
        }
        return unique;
    }

    private boolean adheresToBlueprint(JSONObject variant, TrajectoryBlueprint bp, TaskContext context) {
        String strategy = variant.optString("strategy").toLowerCase();
        String philosophy = variant.optString("semantic_justification").toLowerCase();

        // 1. Philosophy/Goal Alignment Check (Dimension-based)
        JSONObject dimensions = variant.optJSONObject("engineering_dimensions");
        if (dimensions != null) {
            // Check all dimensions defined in the blueprint
            for (java.util.Map.Entry<String, String> entry : bp.getEngineeringDimensions().entrySet()) {
                String dimKey = entry.getKey();
                String bpValue = entry.getValue().toLowerCase();
                String vValue = dimensions.optString(dimKey, "").toLowerCase();
                if (!vValue.isEmpty() && !bpValue.isEmpty() && computeSimilarity(vValue, bpValue) < 0.4) {
                     context.log("[DIVERSITY] Blueprint Violation: Dimension mismatch for " + dimKey + " in " + bp.getId());
                     return false;
                }
            }

            String vPhilosophy = dimensions.optString("philosophy", "").toLowerCase();
            String bpPhilosophy = bp.getPhilosophy().toLowerCase();
            if (computeSimilarity(vPhilosophy, bpPhilosophy) < 0.2) {
                // If the philosophy dimension is wildly off, reject
                context.log("[DIVERSITY] Blueprint Violation: Philosophy mismatch for " + bp.getId());
                return false;
            }
        } else {
            // Fallback for missing dimensions
            String bpGoal = bp.getGoal().toLowerCase();
            if (computeSimilarity(philosophy, bpGoal) < 0.1 && computeSimilarity(strategy, bpGoal) < 0.1) {
                return false;
            }
        }

        // 2. Forbidden overlap check (Structural divergence enforcement)
        for (String forbidden : bp.getForbiddenOverlaps()) {
            if (strategy.contains(forbidden.toLowerCase()) || philosophy.contains(forbidden.toLowerCase())) {
                context.log("[DIVERSITY] Blueprint Violation: Variant contains forbidden overlap: '" + forbidden + "' in " + bp.getId());
                return false;
            }

            // Check dimensions for forbidden overlaps if they represent architectural layers
            if (dimensions != null) {
                for (String dim : dkeys(dimensions)) {
                    if (dimensions.optString(dim).toLowerCase().contains(forbidden.toLowerCase())) {
                        context.log("[DIVERSITY] Blueprint Violation: Dimension '" + dim + "' contains forbidden overlap: " + forbidden);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private java.util.List<String> dkeys(JSONObject obj) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        java.util.Iterator<String> it = obj.keys();
        while (it.hasNext()) keys.add(it.next());
        return keys;
    }

    private boolean isUnique(JSONObject candidate, List<JSONObject> existing) {
        String cStrategy = candidate.optString("strategy").toLowerCase();

        JSONObject cDimensions = candidate.optJSONObject("engineering_dimensions");
        Set<String> cTargets = getActionTargets(candidate);

        for (JSONObject other : existing) {
            JSONObject oDimensions = other.optJSONObject("engineering_dimensions");
            Set<String> oTargets = getActionTargets(other);

            // 0. MEDIATED COGNITION DIVERSITY: Check reasoning focus and file selection
            String cFocus = candidate.optString("reasoning_focus", "").toLowerCase();
            String oFocus = other.optString("reasoning_focus", "").toLowerCase();
            if (!cFocus.isEmpty() && !oFocus.isEmpty()) {
                if (computeSimilarity(cFocus, oFocus) > 0.7) {
                    return false;
                }
            }

            JSONArray cFilesArr = candidate.optJSONArray("selected_files");
            JSONArray oFilesArr = other.optJSONArray("selected_files");
            if (cFilesArr != null && oFilesArr != null) {
                Set<String> cFiles = new HashSet<>();
                Set<String> oFiles = new HashSet<>();
                for (int i = 0; i < cFilesArr.length(); i++) cFiles.add(cFilesArr.getString(i));
                for (int i = 0; i < oFilesArr.length(); i++) oFiles.add(oFilesArr.getString(i));
                if (computeJaccard(cFiles, oFiles) > 0.8) {
                    return false;
                }
            }

            // 1. DIMENSION-BASED COMPARISON: Check for architectural duplication across 9 dimensions
            if (cDimensions != null && oDimensions != null) {
                double dimensionSim = computeDimensionSimilarity(cDimensions, oDimensions);
                // HIGHER DIVERSITY PRESSURE: Reject if dimensions are too similar (above 60%)
                if (dimensionSim > 0.60) return false;
            } else {
                // Fallback to legacy semantic check if dimensions are missing
                String cPhilosophy = candidate.optString("semantic_justification").toLowerCase();
                String oPhilosophy = other.optString("semantic_justification").toLowerCase();
                if (computeSimilarity(cPhilosophy, oPhilosophy) > 0.30) return false;
            }

            // 2. OPERATIONAL REDUNDANCY: Even if wording is different, if they do EXACTLY the same thing
            // on the same files, they are duplicates.
            if (!cTargets.isEmpty() && cTargets.equals(oTargets)) {
                String oStrategy = other.optString("strategy").toLowerCase();
                if (computeSimilarity(cStrategy, oStrategy) > 0.6) return false;
            }
        }
        return true;
    }

    private double computeDimensionSimilarity(JSONObject d1, JSONObject d2) {
        String[] dimensions = {
            "philosophy", "execution_model", "abstraction_depth", "modularity_approach",
            "testing_strategy", "extensibility", "dependency_assumptions", "runtime_behavior", "risk_acceptance"
        };

        double matches = 0;
        for (String dim : dimensions) {
            String v1 = d1.optString(dim, "").toLowerCase();
            String v2 = d2.optString(dim, "").toLowerCase();
            if (v1.equals(v2) && !v1.isEmpty()) {
                matches += 1.0;
            } else if (!v1.isEmpty() && !v2.isEmpty() && computeSimilarity(v1, v2) > 0.6) {
                matches += 0.5;
            }
        }
        return matches / dimensions.length;
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

    private double computeJaccard(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);

        return (double) intersection.size() / union.size();
    }

    private double computeArchitecturalDirectionSimilarity(JSONObject c1, JSONObject c2) {
        // Evaluate diversity in abstraction depth and operational behavior
        double sim = 0.0;

        // Compare strategy types
        if (c1.optString("strategy_type").equals(c2.optString("strategy_type"))) {
            sim += 0.3;
        }

        // Compare expected effects (Short-term vs Long-term focus)
        JSONObject e1 = c1.optJSONObject("expected_effect");
        JSONObject e2 = c2.optJSONObject("expected_effect");
        if (e1 != null && e2 != null) {
            double stSim = computeSimilarity(e1.optString("short_term"), e2.optString("short_term"));
            double ltSim = computeSimilarity(e1.optString("long_term"), e2.optString("long_term"));
            sim += (stSim * 0.2) + (ltSim * 0.2);
        }

        // Compare projected steps (Operational path)
        Set<String> steps1 = getProjectedSteps(c1);
        Set<String> steps2 = getProjectedSteps(c2);
        sim += computeJaccard(steps1, steps2) * 0.3;

        return sim;
    }

    private double computeSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) return 0.0;

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
        // Filter out generic architectural filler words to focus on real semantic tokens
        Set<String> filler = Set.of("architecture", "implementation", "approach", "strategy", "robust", "flexible", "modular", "solution", "engineering", "using", "with", "provide", "provides", "focus", "focuses");

        for (String word : s.split("\\s+")) {
            String clean = word.toLowerCase().replaceAll("[^a-z]", "");
            if (clean.length() > 3 && !filler.contains(clean)) {
                tokens.add(clean);
            }
        }
        return tokens;
    }
}
