package eu.kalafatic.evolution.controller.orchestration.attachments;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight semantic classifier for user requests.
 */
public class TaskIntentClassifier {

    public static Set<TaskIntent> classify(String request) {
        Set<TaskIntent> intents = new HashSet<>();
        String lower = request.toLowerCase();

        // 1. Synonym Expansion & Direct Mapping
        if (matches(lower, "analyze", "investigate", "inspect", "trace", "discovery", "audit", "report")) {
            intents.add(TaskIntent.ANALYSIS);
        }
        if (matches(lower, "debug", "fix", "diagnose", "resolve", "troubleshoot", "issue", "bug", "error", "null", "exception", "stacktrace", "failed", "crash")) {
            intents.add(TaskIntent.DEBUGGING);
            intents.add(TaskIntent.ANALYSIS); // Debugging usually implies analysis
        }
        if (matches(lower, "create", "build", "implement", "generate", "add", "write", "new")) {
            intents.add(TaskIntent.IMPLEMENTATION);
        }
        if (matches(lower, "refactor", "restructure", "reorganize", "cleanup", "improve", "simplify")) {
            intents.add(TaskIntent.REFACTORING);
        }
        if (matches(lower, "architecture", "design", "component", "layer", "structure", "system")) {
            intents.add(TaskIntent.ARCHITECTURE);
        }
        if (matches(lower, "test", "verification", "junit", "coverage", "check", "validate", "assert")) {
            intents.add(TaskIntent.TESTING);
        }
        if (matches(lower, "review", "evaluate", "assess", "critique", "pr", "pull request", "diff")) {
            intents.add(TaskIntent.REVIEW);
        }
        if (matches(lower, "optimize", "performance", "speed", "memory", "efficient", "faster")) {
            intents.add(TaskIntent.OPTIMIZATION);
        }
        if (matches(lower, "explain", "describe", "understand", "what", "how", "tell me")) {
            intents.add(TaskIntent.EXPLANATION);
        }
        if (matches(lower, "plan", "roadmap", "steps", "todo", "task", "strategy")) {
            intents.add(TaskIntent.PLANNING);
        }

        return intents;
    }

    private static boolean matches(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
