package eu.kalafatic.evolution.controller.orchestration.intent;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Interface for classifying user requests as atomic or strategic.
 */
public interface AtomicIntentClassifier {
    /**
     * Analyzes the given request to determine if it is an atomic task.
     * @param request The user's prompt.
     * @param context The current task context.
     * @return An AtomicIntentAnalysis object containing the results.
     */
    AtomicIntentAnalysis analyze(String request, TaskContext context);
}
