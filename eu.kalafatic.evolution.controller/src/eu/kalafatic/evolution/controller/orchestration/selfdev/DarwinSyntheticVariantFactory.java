package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

/**
 * Factory for synthesizing Darwin variants when LLM generation fails to provide sufficient diversity.
 */
public class DarwinSyntheticVariantFactory {

    /**
     * Programmatically synthesizes a missing ANALYTICAL variant.
     */
    public JSONObject synthesizeAnalytical(JSONObject reference, String goal) {
        JSONObject analytical = new JSONObject(reference.toString());

        analytical.put("id", "v-synthetic-analytical-" + System.currentTimeMillis());
        analytical.put("strategy_type", DarwinStrategyType.ANALYTICAL.name());
        analytical.put("strategy", "Analytical assessment of: " + reference.optString("strategy"));
        analytical.put("survival_argument", "Ensures structural safety by assessing risks of the proposed trajectory.");
        analytical.put("tradeoffs", "Prioritizes safety and risk detection over immediate code changes.");
        analytical.put("failure_risks", "Low risk as it primarily performs analysis.");
        analytical.put("suffix", "analytical-fallback");
        analytical.put("score", 0.4);

        JSONArray actions = new JSONArray();
        JSONObject action = new JSONObject();
        action.put("domain", "structure");
        action.put("operation", "ANALYZE");
        action.put("target", ".");
        action.put("description", "Perform a structural risk analysis for the goal: " + goal);
        actions.put(action);
        analytical.put("actions", actions);

        return analytical;
    }

    /**
     * Synthesizes a basic EXPLORATION variant if none exist.
     */
    public JSONObject synthesizeImplementation(String goal) {
        JSONObject impl = new JSONObject();
        impl.put("id", "v-synthetic-exploration-" + System.currentTimeMillis());
        impl.put("strategy_type", DarwinStrategyType.EXPLORATION.name());
        impl.put("strategy", "Direct implementation exploration: " + goal);
        impl.put("survival_argument", "Provides a deterministic implementation path for simple/atomic tasks.");
        impl.put("tradeoffs", "Prioritizes immediate results over complex architectural abstraction.");
        impl.put("failure_risks", "Minimal risks for scoped implementation.");
        impl.put("suffix", "exploration-direct");
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
