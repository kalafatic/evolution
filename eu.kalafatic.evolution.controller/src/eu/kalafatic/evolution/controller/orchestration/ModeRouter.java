package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PlatformMode;
import eu.kalafatic.evolution.model.orchestration.PlatformType;
import eu.kalafatic.evolution.model.orchestration.AutonomyLevel;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

/**
 * Routes execution based on detected or assigned PlatformMode.
 */
public class ModeRouter {

    /**
     * Detects or assigns PlatformMode based on user input and orchestrator state.
     */
    public PlatformMode route(String prompt, Orchestrator orchestrator) {
        if (prompt == null) prompt = "";
        String lowerPrompt = prompt.toLowerCase();

        // 1. Explicit user selection (overrides everything)
        if (lowerPrompt.contains("mode: chat")) {
            return createSimpleChatMode();
        } else if (lowerPrompt.contains("mode: assisted")) {
            return createAssistedCodingMode();
        } else if (lowerPrompt.contains("mode: darwin")) {
            return createDarwinMode();
        } else if (lowerPrompt.contains("mode: self-dev")) {
            return createSelfDevMode();
        }

        // 2. Map from existing model flags
        if (orchestrator != null) {
            if (orchestrator.isDarwinMode()) {
                return createDarwinMode();
            }
            if (orchestrator.isSelfIterativeMode()) {
                return createSelfDevMode();
            }
            if (orchestrator.isIterativeMode()) {
                return createAssistedCodingMode();
            }
        }

        // 3. Default to SIMPLE_CHAT if unclear
        return createSimpleChatMode();
    }

    private PlatformMode createSimpleChatMode() {
        PlatformMode mode = OrchestrationFactory.eINSTANCE.createPlatformMode();
        mode.setType(PlatformType.SIMPLE_CHAT);
        mode.setAutonomyLevel(AutonomyLevel.LOW);
        mode.setIterationLimit(1);
        mode.setAllowSelfModify(false);
        return mode;
    }

    private PlatformMode createAssistedCodingMode() {
        PlatformMode mode = OrchestrationFactory.eINSTANCE.createPlatformMode();
        mode.setType(PlatformType.ASSISTED_CODING);
        mode.setAutonomyLevel(AutonomyLevel.LOW);
        mode.setIterationLimit(2);
        mode.setAllowSelfModify(false);
        return mode;
    }

    private PlatformMode createDarwinMode() {
        PlatformMode mode = OrchestrationFactory.eINSTANCE.createPlatformMode();
        mode.setType(PlatformType.DARWIN_MODE);
        mode.setAutonomyLevel(AutonomyLevel.MEDIUM);
        mode.setIterationLimit(3);
        mode.setAllowSelfModify(false);
        return mode;
    }

    private PlatformMode createSelfDevMode() {
        PlatformMode mode = OrchestrationFactory.eINSTANCE.createPlatformMode();
        mode.setType(PlatformType.SELF_DEV_MODE);
        mode.setAutonomyLevel(AutonomyLevel.HIGH);
        mode.setIterationLimit(5);
        mode.setAllowSelfModify(true);
        // Restrict self-modification to a defined set of modules/directories for safety.
        // For Evo, we allow most things but can exclude core infrastructure if needed.
        mode.getAllowedPaths().add("eu.kalafatic.evolution.controller/src");
        mode.getAllowedPaths().add("eu.kalafatic.evolution.view/src");
        mode.getAllowedPaths().add("eu.kalafatic.evolution.model/src");
        return mode;
    }
}
