package eu.kalafatic.evolution.controller.orchestration.cognitive;

import eu.kalafatic.evolution.controller.orchestration.ContextAssistResult;
import eu.kalafatic.evolution.controller.orchestration.ConfidenceLevel;

/**
 * Orchestrates the cognitive state transitions and routing.
 */
public class CognitiveStateEngine {
    private final MessageClassifier classifier = new MessageClassifier();
    private final CapabilityScoringEngine scoringEngine = new CapabilityScoringEngine();
    private final CognitiveTrajectoryEngine trajectoryEngine = new CognitiveTrajectoryEngine();

    public void processInteraction(String prompt, SessionCognitiveState state, ContextAssistResult assistResult) {
        // 1. Classify current message
        CapabilitySignal signal = classifier.classify(prompt);

        // 2. Influence by ContextAssist if high confidence
        if (assistResult != null && assistResult.getConfidence() == ConfidenceLevel.HIGH) {
            CapabilityType assistCap = mapToCapability(assistResult.getMode());
            signal = new CapabilitySignal(assistCap, 10.0, signal.getIntent(), "CONTEXT_ASSIST");
        }

        // 3. Update scores and current capability
        scoringEngine.updateScores(state, signal);

        // 4. Record signal and update trajectory
        state.addSignal(signal);
        state.setCurrentIntent(signal.getIntent());
        trajectoryEngine.updateTrajectory(state);

        // 5. Update confidence (can be refined later)
        state.setConfidence(0.85);
    }

    private CapabilityType mapToCapability(eu.kalafatic.evolution.controller.orchestration.PlatformType type) {
        switch (type) {
            case DARWIN_MODE:
            case SELF_DEV_MODE:
                return CapabilityType.EVOLUTION;
            case HYBRID_MANUAL_EXPORT:
                return CapabilityType.ARCHITECTURE;
            case ASSISTED_CODING:
                return CapabilityType.CODE;
            case SIMPLE_CHAT:
            default:
                return CapabilityType.CHAT;
        }
    }
}
