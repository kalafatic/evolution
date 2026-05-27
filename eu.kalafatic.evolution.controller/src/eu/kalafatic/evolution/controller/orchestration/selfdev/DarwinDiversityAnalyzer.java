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
            if (blueprints != null) {
                TrajectoryBlueprint bp = blueprints.stream().filter(b -> b.getId().equals(v.optString("id"))).findFirst().orElse(null);
                if (bp != null && !adheresToBlueprint(v, bp, context)) {
                    context.log("[DIVERSITY] Dropping variant as it violates blueprint: " + v.optString("id"));
                    continue;
                }
            }

            if (isUnique(v, unique)) {
                unique.add(v);
            } else {
                context.log("[DIVERSITY] Dropping redundant trajectory: " + v.optString("strategy"));
            }
        }
        return unique;
    }

    private boolean adheresToBlueprint(JSONObject variant, TrajectoryBlueprint bp, TaskContext context) {
        String strategy = variant.optString("strategy").toLowerCase();
        String philosophy = variant.optString("semantic_justification").toLowerCase();

        // Basic check: Philosophy must mention core blueprint goal/philosophy
        String bpGoal = bp.getGoal().toLowerCase();
        if (computeSimilarity(philosophy, bpGoal) < 0.1 && computeSimilarity(strategy, bpGoal) < 0.1) {
            return false;
        }

        // Forbidden overlap check
        for (String forbidden : bp.getForbiddenOverlaps()) {
            if (strategy.contains(forbidden.toLowerCase()) || philosophy.contains(forbidden.toLowerCase())) {
                context.log("[DIVERSITY] Blueprint Violation: Variant contains forbidden overlap: " + forbidden);
                return false;
            }
        }

        return true;
    }

    private boolean isUnique(JSONObject candidate, List<JSONObject> existing) {
        String cStrategy = candidate.optString("strategy").toLowerCase();
        String cPhilosophy = candidate.optString("semantic_justification").toLowerCase();
        String cTradeoffs = candidate.optString("tradeoffs").toLowerCase();
        String cRisks = candidate.optString("failure_risks").toLowerCase();

        Set<String> cTargets = getActionTargets(candidate);
        Set<String> cSteps = getProjectedSteps(candidate);

        for (JSONObject other : existing) {
            String oStrategy = other.optString("strategy").toLowerCase();
            String oPhilosophy = other.optString("semantic_justification").toLowerCase();
            String oTradeoffs = other.optString("tradeoffs").toLowerCase();
            String oRisks = other.optString("failure_risks").toLowerCase();

            Set<String> oTargets = getActionTargets(other);
            Set<String> oSteps = getProjectedSteps(other);

            // 1. CONCEPTUAL OVERLAP: Check if the engineering philosophy is the same
            double philosophySim = computeSimilarity(cPhilosophy, oPhilosophy);
            if (philosophySim > 0.35) return false; // MANDATORY DIVERGENCE: Philosophies MUST diverge significantly

            // 2. TRADEOFF OVERLAP: Check if they are proposing the same technical compromises
            double tradeoffSim = computeSimilarity(cTradeoffs, oTradeoffs);
            if (tradeoffSim > 0.40) return false;

            // 3. RISK OVERLAP: Check if they identify the same failure modes
            double riskSim = computeSimilarity(cRisks, oRisks);
            if (riskSim > 0.55) return false;

            // 4. ARCHITECTURAL DIRECTION OVERLAP
            // Check if they are targeting different abstraction depths or operational scopes
            double directionSim = computeArchitecturalDirectionSimilarity(candidate, other);
            if (directionSim > 0.55) return false;

            // 5. STRATEGY SIMILARITY: Basic word overlap check
            double strategySim = computeSimilarity(cStrategy, oStrategy);
            if (strategySim > 0.55) return false;

            // 6. OPERATIONAL REDUNDANCY: Even if philosophy is slightly different,
            // if they do EXACTLY the same thing on the same files, they are redundant.
            if (!cTargets.isEmpty() && cTargets.equals(oTargets)) {
                double stepSim = computeJaccard(cSteps, oSteps);
                if (stepSim > 0.6) return false;
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
