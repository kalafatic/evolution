package eu.kalafatic.evolution.controller.orchestration.selfdev;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;

/**
 * Factory for creating mode-specific Darwin engines.
 */
public class DarwinEngineFactory {
    
    public static IBaseDarwinEngine create(TaskContext context, IterationMemoryService memoryService, 
                                           AiService aiService, SystemStateSignalProvider stateProvider) {
        
        // Get SessionContainer from context
        SessionContainer sessionContainer = null;
        if (context.getOrchestrator() != null) {
            try {
                java.lang.reflect.Method method = context.getOrchestrator().getClass()
                    .getMethod("getSessionContainer");
                Object container = method.invoke(context.getOrchestrator());
                if (container instanceof SessionContainer) {
                    sessionContainer = (SessionContainer) container;
                }
            } catch (Exception e) {
                // SessionContainer not available
            }
        }
        
        // Determine mode
        PlatformMode mode = context.getPlatformMode();
        String modeType = mode != null ? mode.getType().name() : "STANDARD";
        
        // Check mediated mode
        boolean isMediated = context.getBehaviorProfile().hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY) ||
                            "HYBRID_MANUAL_EXPORT".equals(modeType) ||
                            "MEDIATED".equals(modeType);
        
        // Check self-dev mode - using Session and Instructions (most reliable)
        boolean isSelfDev = false;
        
        // Method 1: Check SelfDevSession
        if (context.getOrchestrator() != null && 
            context.getOrchestrator().getSelfDevSession() != null &&
            context.getOrchestrator().getSelfDevSession().getStatus() == SelfDevStatus.RUNNING) {
            isSelfDev = true;
        }
        
        // Method 2: Check prompt instructions for self-iterative mode
        if (!isSelfDev && context.getOrchestrator() != null &&
            context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions().isSelfIterativeMode()) {
            isSelfDev = true;
        }
        
        // Check chat mode
        boolean isChat = context.getBehaviorProfile().hasTrait(BehaviorTrait.REASONING_ATOMIC) ||
                         "CHAT".equals(modeType);
        
        // Create appropriate engine
        if (isMediated) {
            context.log("[DARWIN_FACTORY] Creating Mediated Darwin Engine");
            return new MediatedDarwinEngine(context, memoryService, aiService);
        } else if (isSelfDev) {
            context.log("[DARWIN_FACTORY] Creating Self-Dev Darwin Engine");
            SelfDevSupervisor supervisor = new MavenSelfDevSupervisor(context);
            return new SelfDevDarwinEngine(context, memoryService, supervisor, aiService);
        } else if (isChat) {
            context.log("[DARWIN_FACTORY] Creating Chat Darwin Engine");
            return new ChatDarwinEngine(context, memoryService, aiService);
        } else {
            context.log("[DARWIN_FACTORY] Creating Standard Darwin Engine");
            return new DarwinEngine(context, memoryService, stateProvider, sessionContainer);
        }
    }
}