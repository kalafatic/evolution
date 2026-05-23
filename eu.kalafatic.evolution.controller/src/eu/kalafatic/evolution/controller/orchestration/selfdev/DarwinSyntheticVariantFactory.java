package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;

/**
 * Factory for synthesizing Darwin variants when LLM generation fails to provide sufficient diversity.
 */
public class DarwinSyntheticVariantFactory {

    /**
     * Programmatically synthesizes a missing SYNTHESIS_HYBRID variant.
     */
    public JSONObject synthesizeAnalytical(JSONObject reference, String goal) {
        JSONObject analytical = new JSONObject(reference.toString());

        analytical.put("id", "v-synthetic-synthesis-" + System.currentTimeMillis());
        analytical.put("strategy_type", DarwinStrategyType.SYNTHESIS_HYBRID.name());
        analytical.put("strategy", "Analytical validation and risk synthesis for: " + goal);
        analytical.put("survival_argument", "Provides a defensive architectural anchor by evaluating the semantic and technical risks of the primary implementation trajectory.");
        analytical.put("tradeoffs", "Prioritizes long-term architectural stability and regression safety over immediate feature velocity.");
        analytical.put("failure_risks", "May delay implementation if severe architectural risks are identified during analysis.");
        analytical.put("suffix", "synthesis-fallback");
        analytical.put("score", 0.4);

        JSONArray actions = new JSONArray();
        JSONObject action = new JSONObject();
        action.put("domain", "structure");
        action.put("operation", "ANALYZE");
        action.put("target", ".");
        action.put("description", "Perform a structural risk and tradeoff analysis for the goal: " + goal);
        actions.put(action);
        analytical.put("actions", actions);

        return analytical;
    }

    /**
     * Synthesizes a basic KEEPER_EVOLUTION variant if none exist.
     */
    public JSONObject synthesizeImplementation(String goal) {
        return synthesizeImplementation(goal, null);
    }

    /**
     * Synthesizes a basic KEEPER_EVOLUTION variant if none exist, informed by atomic analysis.
     */
    public JSONObject synthesizeImplementation(String goal, AtomicIntentAnalysis analysis) {
        JSONObject impl = new JSONObject();
        impl.put("id", "v-atomic-" + System.currentTimeMillis());
        impl.put("strategy_type", DarwinStrategyType.KEEPER_EVOLUTION.name());
        impl.put("strategy", "Direct evolution: " + goal);
        impl.put("survival_argument", "Provides a deterministic implementation path for simple/atomic tasks.");
        impl.put("tradeoffs", "Prioritizes immediate results over complex architectural abstraction.");
        impl.put("failure_risks", "Minimal risks for scoped implementation.");
        impl.put("suffix", "evolution-direct");
        impl.put("score", 0.85);

        // Heuristic Target Resolution
        String target = "implementation.txt";
        String lower = goal.toLowerCase();
        if (lower.contains("java") || lower.contains("class")) target = "GeneratedClass.java";
        else if (lower.contains("readme")) target = "README.md";
        else if (lower.contains("script") || lower.contains("sh")) target = "script.sh";

        JSONArray actions = new JSONArray();
        JSONObject action = new JSONObject();
        action.put("domain", "file");
        action.put("operation", "WRITE");
        action.put("target", target);
        action.put("description", goal);
        actions.put(action);
        impl.put("actions", actions);

        return impl;
    }
}
