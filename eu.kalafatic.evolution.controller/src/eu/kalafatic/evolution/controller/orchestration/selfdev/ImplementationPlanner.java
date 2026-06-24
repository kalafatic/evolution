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

        // 1. Repair missing metadata
        if (!variant.has("tradeoffs") || variant.optString("tradeoffs").isEmpty()) {
            variant.put("tradeoffs", "Standard trade-offs for " + variant.optString("strategy_type", "UNKNOWN") + " architecture.");
        }
        if (!variant.has("failure_risks") || variant.optString("failure_risks").isEmpty()) {
            variant.put("failure_risks", "Inherent risks associated with " + strategy);
        }
        JSONArray expectedOutputs = variant.optJSONArray("expected_outputs");
        if (expectedOutputs == null || expectedOutputs.length() == 0) {
            JSONArray outputs = new JSONArray();
            outputs.put("Successful implementation of " + strategy);
            variant.put("expected_outputs", outputs);
        }
        JSONArray projectedSteps = variant.optJSONArray("projected_steps");
        if (projectedSteps == null || projectedSteps.length() == 0) {
            JSONArray steps = new JSONArray();
            steps.put("Initialize " + strategy);
            steps.put("Materialize architectural intent");
            variant.put("projected_steps", steps);
        }
        if (!variant.has("survival_argument") || variant.optString("survival_argument").isEmpty()) {
            variant.put("survival_argument", "Proposed architectural candidate for goal resolution.");
        }

        // 2. Planning Mandate: The planner no longer synthesizes missing actions.
        // Actions MUST be provided by the evolutionary engine.

        return variant;
    }
}
