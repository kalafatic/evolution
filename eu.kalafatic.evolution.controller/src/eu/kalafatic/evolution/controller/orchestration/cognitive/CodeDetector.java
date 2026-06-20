package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeDetector implements CapabilityDetector {

    private static final Map<String, Double> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("java", 2.0);
        KEYWORDS.put("class", 2.0);
        KEYWORDS.put("method", 2.0);
        KEYWORDS.put("compile", 2.0);
        KEYWORDS.put("bug", 2.0);
        KEYWORDS.put("exception", 2.0);
        KEYWORDS.put("spring", 2.0);
        KEYWORDS.put("maven", 2.0);
        KEYWORDS.put("create", 2.0);
        KEYWORDS.put("fix", 2.0);
        KEYWORDS.put("add", 1.0);
        KEYWORDS.put("run", 1.0);
        KEYWORDS.put("test", 1.0);
        KEYWORDS.put("generate", 2.0);
        KEYWORDS.put("write", 1.0);
        KEYWORDS.put("refactor", 2.0);
        KEYWORDS.put("modify", 1.0);
        KEYWORDS.put("delete", 1.0);
        KEYWORDS.put("check", 1.0);
        KEYWORDS.put("implement", 2.0);
        KEYWORDS.put("build", 1.0);
        KEYWORDS.put("improve", 1.0);
        KEYWORDS.put("update", 1.0);
        KEYWORDS.put("change", 1.0);
        KEYWORDS.put("script", 2.0);
        KEYWORDS.put("sql", 2.0);
        KEYWORDS.put("xml", 2.0);
        KEYWORDS.put("yaml", 2.0);
        KEYWORDS.put("json", 2.0);
        KEYWORDS.put("logic", 1.0);
        KEYWORDS.put("function", 2.0);
        KEYWORDS.put("variable", 2.0);
        KEYWORDS.put("type", 1.0);
        KEYWORDS.put("code", 2.0);
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
                Evidence e = new Evidence(entry.getKey(), entry.getValue(), "CodeDetector");
                evidence.add(e);
                totalScore += entry.getValue();
            }
        }

        double confidence = calculateConfidence(totalScore);
        SessionIntent intent = determineIntent(lower);

        return new CapabilitySignal(CapabilityType.CODE, totalScore, confidence, intent, evidence, "Detected via code keywords");
    }

    private double calculateConfidence(double score) {
        if (score >= 8) return 0.95;
        if (score >= 5) return 0.75;
        if (score >= 3) return 0.50;
        return score / 6.0;
    }

    private SessionIntent determineIntent(String lower) {
        if (lower.contains("fix") || lower.contains("bug") || lower.contains("error") || lower.contains("exception")) {
            return SessionIntent.TROUBLESHOOTING;
        } else if (lower.contains("explain") || lower.contains("how to") || lower.contains("tell me about") || lower.contains("what is")) {
            return SessionIntent.LEARNING;
        }
        return SessionIntent.BUILDING;
    }
}
