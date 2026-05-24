package eu.kalafatic.evolution.controller.orchestration.selfdev;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;

/**
 * Factory for synthesizing Darwin trajectories when LLM generation fails to provide sufficient diversity.
 */
public class DarwinSyntheticVariantFactory {

    /**
     * Programmatically synthesizes a missing semantic alternative trajectory.
     */
    public JSONObject synthesizeSemanticAlternative(JSONObject reference, String goal) {
        return synthesizeSemanticAlternative(reference, goal, null);
    }

    /**
     * Programmatically synthesizes a missing semantic alternative trajectory, informed by atomic analysis.
     */
    public JSONObject synthesizeSemanticAlternative(JSONObject reference, String goal, AtomicIntentAnalysis analysis) {
        JSONObject alternative = new JSONObject(reference.toString());

        alternative.put("id", "v-synthetic-alt-" + System.currentTimeMillis());
        alternative.put("strategy_type", DarwinStrategyType.PHILOSOPHY_MUTATION.name());

        // Semantic Mutation: Flip the philosophy of the reference
        String refStrategy = reference.optString("strategy", "").toLowerCase();
        if (refStrategy.contains("service") || refStrategy.contains("abstraction")) {
            alternative.put("strategy", "Minimal Atomic Utility: " + goal);
            alternative.put("semantic_justification", "Prioritizes zero-dependency minimalism and direct execution over complex service abstractions.");
            alternative.put("suffix", "alt-minimalist");
        } else {
            alternative.put("strategy", "Reusable Service Abstraction: " + goal);
            alternative.put("semantic_justification", "Prioritizes extensibility, interface-driven design, and long-term reusability over atomic execution.");
            alternative.put("suffix", "alt-extensible");
        }

        alternative.put("tradeoffs", "Balances the previous trajectory by offering a different engineering philosophy.");
        alternative.put("failure_risks", "May introduce more complexity if an abstraction is chosen, or less flexibility if atomic.");
        alternative.put("score", 0.7);

        JSONArray actions = alternative.optJSONArray("actions");
        if (actions != null && actions.length() > 0) {
            JSONObject action = actions.getJSONObject(0);
            action.put("description", "Implement " + alternative.getString("strategy"));

            String target = action.optString("target", ".");
            if (target.equals(".") || target.isEmpty()) {
                if (analysis != null && analysis.getTargetArtifact() != null && !analysis.getTargetArtifact().isEmpty()) {
                    target = analysis.getTargetArtifact();
                } else {
                    String lower = goal.toLowerCase();
                    if (lower.contains("java") || lower.contains("class")) target = "GeneratedClass.java";
                    else if (lower.contains("readme")) target = "README.md";
                }
                action.put("target", target);
            }
        }

        return alternative;
    }

    /**
     * Synthesizes a basic PROBABLE_SURVIVOR variant if none exist.
     */
    public JSONObject synthesizeImplementation(String goal) {
        return synthesizeImplementation(goal, null);
    }

    /**
     * Synthesizes a basic PROBABLE_SURVIVOR variant if none exist, informed by atomic analysis.
     */
    public JSONObject synthesizeImplementation(String goal, AtomicIntentAnalysis analysis) {
        JSONObject impl = new JSONObject();
        impl.put("id", "v-synthetic-probable-" + System.currentTimeMillis());
        impl.put("strategy_type", DarwinStrategyType.PROBABLE_SURVIVOR.name());
        impl.put("strategy", "Direct execution: " + goal);
        impl.put("survival_argument", "Provides a deterministic implementation path for simple/atomic tasks.");
        impl.put("tradeoffs", "Prioritizes immediate results over complex architectural abstraction.");
        impl.put("failure_risks", "Minimal risks for scoped implementation.");
        impl.put("semantic_justification", "Direct probable implementation path.");
        impl.put("suffix", "probable-direct");
        impl.put("score", 0.85);

        String target = "implementation.txt";
        if (analysis != null && analysis.getTargetArtifact() != null && !analysis.getTargetArtifact().isEmpty()) {
            target = analysis.getTargetArtifact();
        } else {
            String lower = goal.toLowerCase();
            if (lower.contains("java") || lower.contains("class")) target = "GeneratedClass.java";
            else if (lower.contains("readme")) target = "README.md";
            else if (lower.contains("script") || lower.contains("sh")) target = "script.sh";
        }

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
