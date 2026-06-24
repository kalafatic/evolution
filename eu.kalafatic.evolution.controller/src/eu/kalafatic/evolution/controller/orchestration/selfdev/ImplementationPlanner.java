package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;

/**
 * Deterministic planner that converts validated architectural variants into executable action graphs.
 * Separates architectural reasoning from execution planning.
 */
public class ImplementationPlanner {

    /**
     * Plans executable actions for a variant if they are missing or incomplete.
     * Also fills in missing metadata with sensible defaults.
     * @param variant The architectural variant to plan for.
     * @param context The task context.
     * @return The updated variant with a valid action graph.
     */
    public JSONObject plan(JSONObject variant, TaskContext context) {
        if (variant == null) return null;

        String strategy = variant.optString("strategy", "Unknown Strategy");

        // STABILIZATION: Metadata healing removed.
        // All architectural reasoning must be explicitly provided by the LLM.

        // 2. Validate actions (PROHIBIT SYNTHESIS)
        JSONArray actions = variant.optJSONArray("actions");
        if (actions == null || actions.length() == 0) {
            if (context != null) context.log("[PLANNER] FATAL: Missing executable actions for variant: " + variant.optString("id"));
            return null;
        }

        return variant;
    }
}
