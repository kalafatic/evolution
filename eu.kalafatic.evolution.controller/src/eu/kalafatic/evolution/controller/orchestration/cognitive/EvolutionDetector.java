package eu.kalafatic.evolution.controller.orchestration.cognitive;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvolutionDetector implements CapabilityDetector {

    private static final Map<String, Double> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("evolve", 3.0);
        KEYWORDS.put("darwin", 3.0);
        KEYWORDS.put("iteration", 2.0);
        KEYWORDS.put("reality model", 3.0);
        KEYWORDS.put("recursive discovery", 3.0);
        KEYWORDS.put("iterationmanager", 3.0);
        KEYWORDS.put("darwinflow", 3.0);
        KEYWORDS.put("kernel", 2.0);
        KEYWORDS.put("mutation", 2.0);
        KEYWORDS.put("fitness", 2.0);
        KEYWORDS.put("selection", 2.0);
        KEYWORDS.put("trajectory", 2.0);
        KEYWORDS.put("gene", 2.0);
        KEYWORDS.put("lineage", 2.0);
        KEYWORDS.put("branch", 1.0);
    }

    @Override
    public CapabilitySignal detect(String prompt) {
        if (prompt == null) return null;
        String lower = prompt.toLowerCase();
        List<Evidence> evidence = new ArrayList<>();
        double totalScore = 0;

        for (Map.Entry<String, Double> entry : KEYWORDS.entrySet()) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(entry.getKey()) + "\\b");
            Matcher m = p.matcher(lower);
            if (m.find()) {
                Evidence e = new Evidence(entry.getKey(), entry.getValue(), "EvolutionDetector");
                evidence.add(e);
                totalScore += entry.getValue();
            }
        }

        double confidence = calculateConfidence(totalScore);

        return new CapabilitySignal(CapabilityType.EVOLUTION, totalScore, confidence, SessionIntent.EVOLVING, evidence, "Detected via evolution keywords");
    }

    private double calculateConfidence(double score) {
        if (score >= 8) return 0.95;
        if (score >= 5) return 0.75;
        if (score >= 3) return 0.50;
        return score / 6.0;
    }
}
