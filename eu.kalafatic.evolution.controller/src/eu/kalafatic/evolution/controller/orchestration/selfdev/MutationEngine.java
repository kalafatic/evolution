package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Mutation Engine specialized for interactive repository mutation and evolution.
 * Inherits core evolutionary state machine from ADarwinEngine and CodingEngine.
 */
public class MutationEngine extends CodingEngine {

    public MutationEngine(TaskContext context, IterationMemoryService memoryService,
            SystemStateSignalProvider stateProvider) {
        super(context, memoryService, stateProvider);
        this.platformType = PlatformType.MUTATION;
    }

    @Override
    protected String getAgentInstructions() {
        return "Role: Mutation Interactive Coding Agent.\n"
                + "Strategy: Repository-level interactive software evolution.\n"
                + "MANDATE:\n"
                + "- Analyze arbitrary Git software repositories.\n"
                + "- Reason about requested bugs, features, and refactorings with the user in conversation.\n"
                + "- Distinguish discussion/analysis from actual file modifications.\n"
                + "- When asked to implement or fix code, inspect relevant files, apply code modifications, run build/tests, and present results.\n"
                + "- Work interactively with the user to refine implementations based on test feedback and user guidance.";
    }
}
