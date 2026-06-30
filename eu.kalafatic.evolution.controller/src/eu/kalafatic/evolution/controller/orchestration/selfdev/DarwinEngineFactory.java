package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Factory for creating the appropriate Darwin engine based on mode.
 */
public class DarwinEngineFactory {

    public static ADarwinEngine createEngine(PlatformType platformType, TaskContext context,
                                              IterationMemoryService memoryService,
                                              SystemStateSignalProvider stateProvider) {
        context.log("[FACTORY] Creating DarwinEngine for mode: " + platformType);
        
        switch (platformType) {
            case SIMPLE_CHAT:
                return new ChatEngine(context, memoryService, stateProvider);
            case ASSISTED_CODING:
                return new CodingEngine(context, memoryService, stateProvider);
            case HYBRID_MANUAL_EXPORT:
                return new MediatedEngine(context, memoryService, stateProvider);
            case SELF_DEV_MODE:
                return new SelfDevelopmentEngine(context, memoryService, stateProvider);
            default:
                context.log("[FACTORY] Unknown mode: " + platformType + ". Defaulting to TASK.");
                return new DarwinEngine(context, memoryService, stateProvider);
        }
    }
    
    public static ADarwinEngine createEngineForIntent(PromptIntentAnalyzer.IntentResult intent,
                                                        TaskContext context,
                                                        IterationMemoryService memoryService,
                                                        SystemStateSignalProvider stateProvider) {
        if (intent.isChat()) {
            return new ChatEngine(context, memoryService, stateProvider);
        } else if (intent.isControl()) {
            return new CodingEngine(context, memoryService, stateProvider);
        } else {
            return new DarwinEngine(context, memoryService, stateProvider);
        }
    }
}