package eu.kalafatic.evolution.controller.orchestration.cognitive;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-stage cognitive analysis pipeline based on evidence accumulation.
 */
public class CognitiveAnalysisPipeline {

    private final List<CapabilityDetector> detectors = new ArrayList<>();
    private static final double CONFIDENCE_THRESHOLD = 0.4;

    public CognitiveAnalysisPipeline() {
        detectors.add(new CodeDetector());
        detectors.add(new ArchitectureDetector());
        detectors.add(new EvolutionDetector());
        detectors.add(new SelfDevDetector());
    }

    public CapabilityAnalysis analyze(String prompt) {
        CapabilityAnalysis analysis = new CapabilityAnalysis();

        // 1. Tokenization / Normalization is implicitly handled by detectors (lowercase/regex)

        // 2. Run all independent detectors
        for (CapabilityDetector detector : detectors) {
            CapabilitySignal signal = detector.detect(prompt);
            if (signal != null) {
                analysis.addCandidate(signal);
            }
        }

        // 3. Winner Selection with Fallback to CHAT
        CapabilitySignal winner = selectWinner(analysis);
        if (winner == null || winner.getConfidence() < CONFIDENCE_THRESHOLD) {
            // Default to CHAT if no strong candidate
            winner = createChatFallback(prompt);
            analysis.addCandidate(winner);
        }

        analysis.setWinner(winner);

        return analysis;
    }

    private CapabilitySignal selectWinner(CapabilityAnalysis analysis) {
        return analysis.getBestCandidate();
    }

    private CapabilitySignal createChatFallback(String prompt) {
        List<Evidence> evidence = new ArrayList<>();
        evidence.add(new Evidence("fallback", 1.0, "CognitiveAnalysisPipeline"));

        SessionIntent intent = SessionIntent.LEARNING;
        // Simple heuristic for chat intent
        if (prompt != null && (prompt.toLowerCase().contains("hi") || prompt.toLowerCase().contains("hello"))) {
            intent = SessionIntent.LEARNING; // Greetings are learning/interactive
        }

        return new CapabilitySignal(
            CapabilityType.CHAT,
            1.0,
            1.0,
            intent,
            evidence,
            "Fallback to CHAT (default state)"
        );
    }
}
