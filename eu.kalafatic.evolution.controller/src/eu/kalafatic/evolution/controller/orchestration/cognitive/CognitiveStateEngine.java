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
    private final CognitiveStatePublisher publisher = new CognitiveStatePublisher();

    public void processInteraction(String prompt, SessionCognitiveState state,
                                   eu.kalafatic.evolution.controller.orchestration.TaskContext context,
                                   eu.kalafatic.evolution.controller.orchestration.ContextAssistResult assistResult) {
        // 1. Classify current message
        CapabilitySignal signal = classifier.classify(prompt);

        // 2. Influence by ContextAssist if high confidence
        if (assistResult != null && assistResult.getConfidence() == ConfidenceLevel.HIGH) {
            CapabilityType assistCap = mapToCapability(assistResult.getMode());
            signal = new CapabilitySignal(assistCap, 10.0, signal.getIntent(), "CONTEXT_ASSIST");
        }

        // 3. Update scores and current capability
        scoringEngine.updateScores(state, signal);

        // 4. Record signal and update trajectory metrics
        state.addSignal(signal);
        state.setCurrentIntent(signal.getIntent());
        trajectoryEngine.updateTrajectory(state);

        // 5. Calculate Cognitive Depth
        state.setCognitiveDepth(calculateDepth(state, prompt));

        // 6. Update confidence based on stability and depth
        double baseConfidence = 0.8;
        state.setConfidence(baseConfidence + (state.getTrendStability() * 0.2));

        // 7. Publish meaningful changes
        publisher.publish(context, state);
    }

    private int calculateDepth(SessionCognitiveState state, String prompt) {
        int depth = state.getCognitiveDepth();
        CapabilityType cap = state.getCurrentCapability();

        // Capability Base Depth
        int targetDepth = 1;
        switch (cap) {
            case EVOLUTION: targetDepth = 8; break;
            case ARCHITECTURE: targetDepth = 5; break;
            case CODE: targetDepth = 3; break;
            case CHAT: default: targetDepth = 1; break;
        }

        // Convergence logic: depth moves towards target
        if (depth < targetDepth) {
            depth++;
        } else if (depth > targetDepth && state.getTrendStability() > 0.8) {
            // Only shallow if the trend is stable
            depth--;
        }

        // Complexity boost
        if (prompt.length() > 200 || prompt.contains("{") || prompt.contains("trajectory")) {
            depth = Math.min(10, depth + 1);
        }

        return depth;
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
