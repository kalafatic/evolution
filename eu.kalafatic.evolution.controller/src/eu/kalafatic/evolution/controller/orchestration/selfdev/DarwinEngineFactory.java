package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Factory for creating the appropriate Darwin engine based on mode.
 */
public class DarwinEngineFactory {

    public static ADarwinEngine createEngine(String mode, TaskContext context,
                                              IterationMemoryService memoryService,
                                              SystemStateSignalProvider stateProvider) {
        context.log("[FACTORY] Creating DarwinEngine for mode: " + mode);
        
        switch (mode.toUpperCase()) {
            case "CHAT":
                return new ChatDarwinEngine(context, memoryService, stateProvider);
            case "TASK":
            case "CODE":
                return new TaskDarwinEngine(context, memoryService, stateProvider);
            case "MEDIATED":
                return new MediatedDarwinEngine(context, memoryService, stateProvider);
            case "SELFDEV":
                return new SelfDevDarwinEngine(context, memoryService, stateProvider);
            default:
                context.log("[FACTORY] Unknown mode: " + mode + ". Defaulting to TASK.");
                return new TaskDarwinEngine(context, memoryService, stateProvider);
        }
    }
    
    public static ADarwinEngine createEngineForIntent(PromptIntentAnalyzer.IntentResult intent,
                                                        TaskContext context,
                                                        IterationMemoryService memoryService,
                                                        SystemStateSignalProvider stateProvider) {
        if (intent.isChat()) {
            return new ChatDarwinEngine(context, memoryService, stateProvider);
        } else if (intent.isControl()) {
            return new TaskDarwinEngine(context, memoryService, stateProvider);
        } else {
            return new TaskDarwinEngine(context, memoryService, stateProvider);
        }
    }
}