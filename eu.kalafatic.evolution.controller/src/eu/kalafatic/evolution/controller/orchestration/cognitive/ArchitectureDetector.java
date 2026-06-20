package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchitectureDetector implements CapabilityDetector {

    private static final Map<String, Double> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("architecture", 3.0);
        KEYWORDS.put("repository", 2.0);
        KEYWORDS.put("module", 2.0);
        KEYWORDS.put("dependency", 2.0);
        KEYWORDS.put("graph", 2.0);
        KEYWORDS.put("workflow", 2.0);
        KEYWORDS.put("subsystem", 2.0);
        KEYWORDS.put("use case", 2.0);
        KEYWORDS.put("responsibility", 2.0);
        KEYWORDS.put("codebase", 2.0);
        KEYWORDS.put("analyze", 1.0);
        KEYWORDS.put("investigate", 1.0);
        KEYWORDS.put("report", 1.0);
        KEYWORDS.put("summarize", 1.0);
        KEYWORDS.put("discovery", 2.0);
        KEYWORDS.put("landscape", 2.0);
        KEYWORDS.put("artifact", 2.0);
        KEYWORDS.put("mapping", 2.0);
        KEYWORDS.put("structure", 2.0);
        KEYWORDS.put("relationship", 2.0);
        KEYWORDS.put("component", 2.0);
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
                Evidence e = new Evidence(entry.getKey(), entry.getValue(), "ArchitectureDetector");
                evidence.add(e);
                totalScore += entry.getValue();
            }
        }

        double confidence = calculateConfidence(totalScore);
        SessionIntent intent = determineIntent(lower);

        return new CapabilitySignal(CapabilityType.ARCHITECTURE, totalScore, confidence, intent, evidence, "Detected via architecture keywords");
    }

    private double calculateConfidence(double score) {
        if (score >= 8) return 0.95;
        if (score >= 5) return 0.75;
        if (score >= 3) return 0.50;
        return score / 6.0;
    }

    private SessionIntent determineIntent(String lower) {
        if (lower.contains("improve") || lower.contains("refactor")) return SessionIntent.BUILDING;
        if (lower.contains("discover") || lower.contains("explore")) return SessionIntent.LEARNING;
        return SessionIntent.ANALYZING;
    }
}
