package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.dream.DreamEngine;

/**
 * Factory for creating the appropriate Darwin engine based on mode.
 */
public class DarwinEngineFactory {

    public static ADarwinEngine createEngine(PlatformType platformType, TaskContext context,
                                              IterationMemoryService memoryService,
                                              SystemStateSignalProvider stateProvider) {
        context.log("[FACTORY] Creating DarwinEngine for mode: " + platformType);

        ADarwinEngine engine;
        switch (platformType) {
            case FORGE:
                engine = new LLMDarwinEngine(context, memoryService, stateProvider);
                break;
            case SIMPLE_CHAT:
                engine = new ChatEngine(context, memoryService, stateProvider);
                break;
            case ASSISTED_CODING: //return new DarwinEngine(context, memoryService, stateProvider);
                engine = new CodingEngine(context, memoryService, stateProvider);
                break;
            case DEVELOPER:
                engine = new DeveloperEngine(context, memoryService, stateProvider);
                break;
            case HYBRID_MANUAL_EXPORT:
                engine = new MediatedEngine(context, memoryService, stateProvider);
                break;
            case SELF_DEV_MODE:
                engine = new SelfDevelopmentEngine(context, memoryService, stateProvider);
                break;
            case INTENT_RECONSTRUCTION:
                engine = new IntentReconstructionEngine(context, memoryService, stateProvider);
                break;
            case MUTATION:
                engine = new MutationEngine(context, memoryService, stateProvider);
                break;
            case DREAM_MODE:
                engine = new DreamEngine(context, memoryService, stateProvider);
                break;
            case DARWIN_MODE:
                engine = new DarwinEngine(context, memoryService, stateProvider);
                break;
            default:
                context.log("[FACTORY] Unknown mode: " + platformType + ". Defaulting to TASK.");
                engine = new DarwinEngine(context, memoryService, stateProvider);
                break;
        }
        context.log("[DARWIN-FACTORY] sessionId=" + context.getSessionId()
                + " resolvedMode=" + platformType
                + " engine=" + engine.getClass().getSimpleName());
        return engine;
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
