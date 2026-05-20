package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

/**
 * Factory for synthesizing Darwin variants when LLM generation fails to provide sufficient diversity.
 */
public class DarwinSyntheticVariantFactory {

    /**
     * Programmatically synthesizes a missing ANALYTICAL variant based on an IMPLEMENTATION variant.
     */
    public JSONObject synthesizeAnalytical(JSONObject implementation, String goal) {
        JSONObject analytical = new JSONObject(implementation.toString());

        analytical.put("id", "v-synthetic-analytical-" + System.currentTimeMillis());
        analytical.put("strategy_type", DarwinStrategyType.ANALYTICAL.name());
        analytical.put("strategy", "Analytical assessment of: " + implementation.optString("strategy"));
        analytical.put("survival_argument", "Ensures structural safety by assessing risks of the primary implementation approach.");
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
     * Synthesizes a basic IMPLEMENTATION variant if none exist.
     */
    public JSONObject synthesizeImplementation(String goal) {
        JSONObject impl = new JSONObject();
        impl.put("id", "v-synthetic-impl-" + System.currentTimeMillis());
        impl.put("strategy_type", DarwinStrategyType.IMPLEMENTATION.name());
        impl.put("strategy", "Base implementation of: " + goal);
        impl.put("survival_argument", "Provides a fallback direct implementation path.");
        impl.put("tradeoffs", "Direct approach, minimal abstraction.");
        impl.put("failure_risks", "Standard implementation risks.");
        impl.put("suffix", "impl-fallback");
        impl.put("score", 0.5);

        JSONArray actions = new JSONArray();
        JSONObject action = new JSONObject();
        action.put("domain", "file");
        action.put("operation", "WRITE");
        action.put("target", "implementation_plan.md");
        action.put("description", "Document the implementation plan for: " + goal);
        actions.put(action);
        impl.put("actions", actions);

        return impl;
    }
}
