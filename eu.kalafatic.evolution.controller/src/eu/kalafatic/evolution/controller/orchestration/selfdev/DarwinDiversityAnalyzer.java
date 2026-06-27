package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

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
     * Filters a list of variants and ensures they adhere to their blueprints using vector-based scoring.
     */
    public List<JSONObject> analyze(List<JSONObject> variants, List<TrajectoryBlueprint> blueprints, double strictness, double modelCapability, TaskContext context) {
        if (variants.isEmpty()) return variants;

        double effectiveStrictness = strictness * modelCapability;
        context.log(String.format("[DIVERSITY] Starting vector analysis. Strictness: %.2f, Model Capability: %.2f, Effective Threshold: %.2f",
                strictness, modelCapability, effectiveStrictness));

        List<JSONObject> unique = new ArrayList<>();
        for (JSONObject v : variants) {
            DiversityResultType result = evaluate(v, blueprints, unique, effectiveStrictness, context);

            if (result == DiversityResultType.ACCEPTED || result == DiversityResultType.ACCEPTED_WITH_WARNINGS) {
                unique.add(v);
                v.put("diversity_result", result.name());
            } else {
                context.log("[DIVERSITY] Dropping redundant or invalid trajectory: " + v.optString("id"));
            }
        }
        return unique;
    }

    private DiversityResultType evaluate(JSONObject v, List<TrajectoryBlueprint> blueprints, List<JSONObject> unique, double threshold, TaskContext context) {
        // 1. Schema Validation (Hard failure - Architecture is STRICT)
        if (v.optJSONArray("actions") == null || v.optJSONArray("actions").length() == 0) {
            context.log("[DIVERSITY] SCHEMA FATAL: Trajectory " + v.optString("id") + " has no executable actions.");
            return DiversityResultType.REJECTED_FATAL;
        }

        // 2. Blueprint Adherence Check
        TrajectoryBlueprint bp = null;
        if (blueprints != null) {
            bp = blueprints.stream().filter(b -> b.getId().equals(v.optString("id"))).findFirst().orElse(null);
            if (bp != null) {
                DiversityResultType adherence = checkBlueprintAdherence(v, bp, threshold, context);
                // Fatal rejections only for corrupted or exact duplicate state
                if (adherence == DiversityResultType.REJECTED_FATAL) return DiversityResultType.REJECTED_FATAL;

                double diversity = calculateDiversity(v, unique);
                if (diversity < 0.05) { // EXACT DUPLICATE (dist near 0)
                    context.log(String.format("[DIVERSITY] COLLAPSE FATAL: %s is an exact evolutionary duplicate (dist=%.2f).", v.optString("id"), diversity));
                    return DiversityResultType.REJECTED_FATAL;
                }

                if (diversity < threshold) {
                    context.log(String.format("[DIVERSITY] REJECTED_SIBLING_OVERLAP: %s (dist=%.2f) below threshold %.2f.", v.optString("id"), diversity, threshold));
                    return DiversityResultType.REJECTED_FATAL;
                }

                return adherence;
            }
        }

        double diversity = calculateDiversity(v, unique);
        if (diversity < threshold) {
             context.log(String.format("[DIVERSITY] COLLAPSE FATAL: %s diversity (dist=%.2f) insufficient (threshold=%.2f).", v.optString("id"), diversity, threshold));
             return DiversityResultType.REJECTED_FATAL;
        }

        return DiversityResultType.ACCEPTED;
    }

    private DiversityResultType checkBlueprintAdherence(JSONObject variant, TrajectoryBlueprint bp, double threshold, TaskContext context) {
        TrajectoryVector vVector = mapToVector(variant);
        double dist = vVector.distance(bp.getTargetVector());
        // Gradient warnings instead of binary fatal mismatch
        boolean hasWarnings = dist > threshold;

        context.log(String.format("[DIVERSITY] Vector Adherence for %s: dist=%.2f (threshold=%.2f)", bp.getId(), dist, threshold));

        if (dist > 0.95) {
            context.log("[DIVERSITY] CORRUPTION FATAL: Trajectory " + bp.getId() + " has zero architectural alignment with its blueprint.");
            return DiversityResultType.REJECTED_FATAL;
        }

        String strategy = variant.optString("strategy").toLowerCase();
        String philosophy = variant.optString("semantic_justification").toLowerCase();
        JSONObject dimensions = variant.optJSONObject("engineering_dimensions");

        // 2. Forbidden overlap check (Structural divergence enforcement)
        for (String forbidden : bp.getForbiddenOverlaps()) {
            if (strategy.contains(forbidden.toLowerCase()) || philosophy.contains(forbidden.toLowerCase())) {
                context.log("[DIVERSITY] Blueprint FATAL Violation: Variant contains forbidden overlap: '" + forbidden + "' in " + bp.getId());
                return DiversityResultType.REJECTED_FATAL;
            }

            if (dimensions != null) {
                for (String dim : dkeys(dimensions)) {
                    if (dimensions.optString(dim).toLowerCase().contains(forbidden.toLowerCase())) {
                        context.log("[DIVERSITY] Blueprint FATAL Violation: Dimension '" + dim + "' contains forbidden overlap: " + forbidden);
                        return DiversityResultType.REJECTED_FATAL;
                    }
                }
            }
        }

        return hasWarnings ? DiversityResultType.ACCEPTED_WITH_WARNINGS : DiversityResultType.ACCEPTED;
    }

    private java.util.List<String> dkeys(JSONObject obj) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        java.util.Iterator<String> it = obj.keys();
        while (it.hasNext()) keys.add(it.next());
        return keys;
    }

    public double calculateDiversity(JSONObject candidate, List<JSONObject> existing) {
        if (existing.isEmpty()) return 1.0;

        TrajectoryVector cVector = mapToVector(candidate);
        double minDistance = 1.0;

        for (JSONObject other : existing) {
            TrajectoryVector oVector = mapToVector(other);
            double dist = cVector.distance(oVector);

            // 1. Penalize for operational redundancy (same target files)
            if (getActionTargets(candidate).equals(getActionTargets(other))) {
                dist *= 0.4; // Heavier penalty for same targets
            }

            // 2. Penalize for cosmetic similarity (naming)
            double semanticSim = computeSimilarity(candidate.optString("strategy"), other.optString("strategy"));
            if (semanticSim > 0.8) {
                dist *= 0.3; // Harder penalty for cosmetic similarity
            }

            // 3. Structural overlap (projected steps)
            double stepSim = computeJaccard(getProjectedSteps(candidate), getProjectedSteps(other));
            if (stepSim > 0.6) {
                dist *= 0.5;
            }

            // 4. Engineering Dimension Collapse Check
            double dimSim = computeDimensionSimilarity(candidate.optJSONObject("engineering_dimensions"), other.optJSONObject("engineering_dimensions"));
            if (dimSim > 0.8) {
                dist *= 0.2; // Fatal collapse if engineering dimensions are near identical
            }

            if (dist < minDistance) minDistance = dist;
        }

        return minDistance;
    }

    private TrajectoryVector mapToVector(JSONObject variant) {
        TrajectoryVector v = new TrajectoryVector();
        JSONObject dims = variant.optJSONObject("engineering_dimensions");
        if (dims == null) return v;

        v.setModularity(mapDimension(dims.optString("modularity_approach")));
        v.setResilience(mapDimension(dims.optString("resilience_strategy")));
        v.setArchitecturalDepth(mapDimension(dims.optString("abstraction_depth")));
        v.setServiceOrientation(mapDimension(dims.optString("execution_model")));
        v.setPersistence(mapDimension(dims.optString("persistence_orientation")));
        v.setDeterminism(mapDimension(dims.optString("runtime_behavior")));
        v.setExtensibility(mapDimension(dims.optString("extensibility")));
        v.setCoupling(1.0 - mapDimension(dims.optString("dependency_assumptions"))); // High internal deps => low coupling coefficient
        v.setAbstraction(mapDimension(dims.optString("abstraction_depth")));
        v.setRiskAcceptance(mapDimension(dims.optString("risk_acceptance")));

        return v;
    }

    private double mapDimension(String value) {
        if (value == null) return 0.5;
        String val = value.toLowerCase();

        // High intensity signals
        if (matches(val, "high", "service", "modular", "extensible", "persistent", "async", "experimental", "reactive", "external")) return 0.9;

        // Low intensity signals
        if (matches(val, "low", "monolithic", "atomic", "none", "conservative", "deterministic", "smoke", "internal", "direct")) return 0.1;

        // Medium
        if (matches(val, "medium", "standard", "balanced", "hybrid")) return 0.5;

        return 0.5;
    }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private double computeDimensionSimilarity(JSONObject d1, JSONObject d2) {
        if (d1 == null || d2 == null) return 0.0;
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
        Set<String> filler = Set.of("architecture", "implementation", "approach", "robust", "flexible", "solution", "engineering", "using", "with", "provide", "provides", "focuses");

        for (String word : s.split("\\s+")) {
            String clean = word.toLowerCase().replaceAll("[^a-z]", "");
            if (clean.length() >= 3 && !filler.contains(clean)) {
                tokens.add(clean);
            }
        }
        return tokens;
    }
}
