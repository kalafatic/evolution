package eu.kalafatic.evolution.controller.orchestration;

import java.util.HashSet;
import java.util.Set;
import eu.kalafatic.evolution.controller.orchestration.attachments.TaskIntent;
import eu.kalafatic.evolution.controller.orchestration.intent.AtomicIntentAnalysis;
import eu.kalafatic.evolution.controller.orchestration.intent.HybridAtomicIntentClassifier;

/**
 * Unified service for intent detection and semantic analysis.
 */
public class IntentService {

    private final AiService aiService;

    public IntentService(AiService aiService) {
        this.aiService = aiService;
    }

    public void analyze(String request, TaskContext context) {
        OrchestrationState state = context.getOrchestrationState();
        state.setRawInput(request);

        // 1. Keyword-based intent classification (TaskIntents)
        Set<TaskIntent> intents = classify(request);
        state.setTaskIntents(intents);
        state.addDiagnostic("Classified intents: " + intents);

        // 2. Atomic intent analysis
        HybridAtomicIntentClassifier atomicClassifier = new HybridAtomicIntentClassifier(aiService);
        AtomicIntentAnalysis atomicAnalysis = atomicClassifier.analyze(request, context);
        state.getMetadata().put("atomicAnalysis", atomicAnalysis);
        state.addDiagnostic("Atomic analysis: atomic=" + atomicAnalysis.isAtomic() + ", confidence=" + atomicAnalysis.getConfidence());
    }

    public static Set<TaskIntent> classify(String request) {
        Set<TaskIntent> intents = new HashSet<>();
        String lower = request.toLowerCase();

        if (matches(lower, "analyze", "investigate", "inspect", "trace", "discovery", "audit", "report", "summarize")) {
            intents.add(TaskIntent.ANALYSIS);
        }
        if (matches(lower, "debug", "fix", "diagnose", "resolve", "troubleshoot", "issue", "bug", "error", "null", "exception", "stacktrace", "failed", "crash")) {
            intents.add(TaskIntent.DEBUGGING);
            intents.add(TaskIntent.ANALYSIS);
        }
        if (matches(lower, "create", "build", "implement", "generate", "add", "write", "new")) {
            intents.add(TaskIntent.IMPLEMENTATION);
        }
        if (matches(lower, "refactor", "restructure", "reorganize", "cleanup", "improve", "simplify")) {
            intents.add(TaskIntent.REFACTORING);
        }
        if (matches(lower, "architecture", "design", "component", "layer", "structure", "system", "relationship", "dependency", "lifecycle", "workflow", "event", "sync")) {
            intents.add(TaskIntent.ARCHITECTURE);
            intents.add(TaskIntent.ANALYSIS);
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
