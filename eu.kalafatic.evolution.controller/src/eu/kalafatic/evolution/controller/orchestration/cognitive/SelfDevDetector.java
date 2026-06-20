package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelfDevDetector implements CapabilityDetector {

    private static final Map<String, Double> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("self-dev", 4.0);
        KEYWORDS.put("self-development", 4.0);
        KEYWORDS.put("genome", 3.0);
        KEYWORDS.put("self-evolution", 3.0);
        KEYWORDS.put("recursive", 2.0);
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
                Evidence e = new Evidence(entry.getKey(), entry.getValue(), "SelfDevDetector");
                evidence.add(e);
                totalScore += entry.getValue();
            }
        }

        double confidence = calculateConfidence(totalScore);

        return new CapabilitySignal(CapabilityType.SELF_DEV, totalScore, confidence, SessionIntent.EVOLVING, evidence, "Detected via self-dev keywords");
    }

    private double calculateConfidence(double score) {
        if (score >= 6) return 0.95;
        if (score >= 4) return 0.80;
        if (score >= 2) return 0.50;
        return score / 4.0;
    }
}
