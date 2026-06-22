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

        // 2. Synthesize actions if missing or empty
        JSONArray actions = variant.optJSONArray("actions");
        if (actions == null || actions.length() == 0) {
            if (context != null) context.log("[PLANNER] Synthesizing executable actions for variant: " + variant.optString("id"));
            actions = synthesizeActions(variant, context);
            variant.put("actions", actions);
        }

        return variant;
    }

    private JSONArray synthesizeActions(JSONObject variant, TaskContext context) {
        JSONArray actions = new JSONArray();

        AtomicIntentAnalysis atomic = null;
        if (context != null && context.getOrchestrationState() != null) {
            atomic = (AtomicIntentAnalysis) context.getOrchestrationState().getMetadata().get("atomicAnalysis");
        }

        // Strategy A: Use projected_steps if available
        JSONArray steps = variant.optJSONArray("projected_steps");
        if (steps != null && steps.length() > 0) {
            for (int i = 0; i < steps.length(); i++) {
                String step = steps.optString(i);
                if (step != null && !step.isEmpty() && !step.startsWith("Initialize") && !step.startsWith("Materialize")) {
                    actions.put(createActionFromStep(step, atomic));
                }
            }
        }

        // Strategy B: Fallback to Atomic Intent Analysis
        if (actions.length() == 0 && atomic != null && atomic.isAtomic() && atomic.getTargetArtifact() != null && !atomic.getTargetArtifact().isEmpty()) {
            JSONObject action = new JSONObject();
            action.put("domain", "file");
            action.put("operation", "WRITE");
            action.put("target", atomic.getTargetArtifact());
            action.put("description", "Materialize " + variant.optString("strategy") + " into " + atomic.getTargetArtifact());
            actions.put(action);
        }

        // Strategy C: Generic workspace analysis fallback
        if (actions.length() == 0) {
            JSONObject action = new JSONObject();
            action.put("domain", "kernel");
            action.put("operation", "ANALYZE");
            action.put("target", "workspace");
            action.put("description", "Bootstrap " + variant.optString("id") + " architectural strategy: " + variant.optString("strategy"));
            actions.put(action);
        }

        // Strategy D: FINAL FALLBACK GUARANTEE
        if (actions.length() == 0) {
            JSONObject action = new JSONObject();
            action.put("domain", "kernel");
            action.put("operation", "ANALYZE");
            action.put("target", "workspace");
            action.put("description", "Execute architectural intent for " + variant.optString("id"));
            actions.put(action);
        }

        return actions;
    }

    private JSONObject createActionFromStep(String step, AtomicIntentAnalysis atomic) {
        JSONObject action = new JSONObject();
        String lowerStep = step.toLowerCase();

        if (lowerStep.contains("write") || lowerStep.contains("create") || lowerStep.contains("implement") || lowerStep.contains("add")) {
            action.put("domain", "file");
            action.put("operation", "WRITE");
        } else if (lowerStep.contains("delete") || lowerStep.contains("remove")) {
            action.put("domain", "file");
            action.put("operation", "DELETE");
        } else if (lowerStep.contains("test") || lowerStep.contains("verify")) {
            action.put("domain", "test");
            action.put("operation", "RUN");
        } else if (lowerStep.contains("build")) {
            action.put("domain", "build");
            action.put("operation", "BUILD");
        } else {
            action.put("domain", "kernel");
            action.put("operation", "EXECUTE");
        }

        // Try to find a target in the step string or fallback to atomic target
        String target = ".";
        if (atomic != null && atomic.getTargetArtifact() != null && !atomic.getTargetArtifact().isEmpty()) {
            target = atomic.getTargetArtifact();
        }

        action.put("target", target);
        action.put("description", step);
        return action;
    }
}
