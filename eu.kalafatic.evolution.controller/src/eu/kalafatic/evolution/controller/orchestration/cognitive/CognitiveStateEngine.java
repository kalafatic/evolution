package eu.kalafatic.evolution.controller.orchestration.cognitive;

import eu.kalafatic.evolution.controller.orchestration.ConfidenceLevel;

/**
 * Orchestrates the cognitive state transitions and routing.
 */
public class CognitiveStateEngine {
    private final CognitiveAnalysisPipeline pipeline = new CognitiveAnalysisPipeline();
    private final CapabilityScoringEngine scoringEngine = new CapabilityScoringEngine();
    private final CognitiveTrajectoryEngine trajectoryEngine = new CognitiveTrajectoryEngine();
    private final CognitiveStatePublisher publisher = new CognitiveStatePublisher();

    public void processInteraction(String prompt, SessionCognitiveState state,
                                   eu.kalafatic.evolution.controller.orchestration.TaskContext context,
                                   eu.kalafatic.evolution.controller.orchestration.ContextAssistResult assistResult) {
        // 1. Analyze current message through the cognitive pipeline
        CapabilityAnalysis analysis = pipeline.analyze(prompt);
        CapabilitySignal signal = analysis.getWinner();

        // 2. Influence by ContextAssist if high confidence
        if (assistResult != null && assistResult.getConfidence() == ConfidenceLevel.HIGH) {
            CapabilityType assistCap = mapToCapability(assistResult.getMode());
            signal = new CapabilitySignal(assistCap, 10.0, 1.0, signal.getIntent(), null, "CONTEXT_ASSIST");
        }

        // 3. Update scores and current capability
        scoringEngine.updateScores(state, signal);

        // 4. Record signal and update trajectory metrics
        state.addSignal(signal);
        state.setCurrentIntent(signal.getIntent());
        trajectoryEngine.updateTrajectory(state);

        // 5. Calculate Cognitive Depth
        int newDepth = calculateDepth(state, prompt);
        state.setCognitiveDepth(newDepth);

        // 6. Update confidence based on stability and depth
        double baseConfidence = 0.8;
        state.setConfidence(baseConfidence + (state.getTrendStability() * 0.2));

        // 7. Publish meaningful changes
        publisher.publish(context, state);
    }

    /**
     * Maps Cognitive Depth (1-10) to Evolution Intensity (1-4).
     * Intensity 1: Simple Chat
     * Intensity 2: Assisted Coding
     * Intensity 3: Architecture Design
     * Intensity 4: Recursive Evolution (Self-Dev)
     */
    public static int getEvolutionIntensity(int depth) {
        if (depth <= 2) return 1;
        if (depth <= 4) return 2;
        if (depth <= 7) return 3;
        return 4;
    }

    private int calculateDepth(SessionCognitiveState state, String prompt) {
        int depth = state.getCognitiveDepth();
        CapabilityType cap = state.getCurrentCapability();

        // Capability Base Depth
        int targetDepth = 1;
        switch (cap) {
            case SELF_DEV: targetDepth = 10; break;
            case EVOLUTION: targetDepth = 9; break;
            case ARCHITECTURE: targetDepth = 6; break;
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

        // Complexity boost: Long prompts or structured content increase depth
        if (prompt.length() > 300 || prompt.contains("{") || (prompt.contains("trajectory") && prompt.length() > 50)) {
            depth = Math.min(10, depth + 1);
        }

        // Contextual pressure: if we are already in an evolution, maintain high depth
        if (cap == CapabilityType.EVOLUTION && depth < 8) {
            depth = 8;
        }

        return depth;
    }

    private CapabilityType mapToCapability(eu.kalafatic.evolution.controller.orchestration.PlatformType type) {
        switch (type) {
            case DARWIN_MODE:
            case SELF_DEV_MODE:
                return CapabilityType.EVOLUTION;
            case HYBRID_MANUAL_EXPORT:
                return CapabilityType.MEDIATED;
            case ASSISTED_CODING:
                return CapabilityType.CODE;
            case SIMPLE_CHAT:
            default:
                return CapabilityType.CHAT;
        }
    }
}
