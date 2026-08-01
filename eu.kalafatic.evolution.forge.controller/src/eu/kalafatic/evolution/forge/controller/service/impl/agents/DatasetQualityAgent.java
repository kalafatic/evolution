package eu.kalafatic.evolution.forge.controller.service.impl.agents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatasetQualityAgent {

    public enum Recommendation {
        ACCEPT,
        REVIEW,
        REJECT
    }

    public static class QualityReport {
        private final TrainingRecord record;
        private final Recommendation recommendation;
        private final String reason;

        public QualityReport(TrainingRecord record, Recommendation recommendation, String reason) {
            this.record = record;
            this.recommendation = recommendation;
            this.reason = reason;
        }

        public TrainingRecord getRecord() {
            return record;
        }

        public Recommendation getRecommendation() {
            return recommendation;
        }

        public String getReason() {
            return reason;
        }
    }

    public List<QualityReport> evaluate(List<TrainingRecord> records) {
        List<QualityReport> reports = new ArrayList<>();
        Set<String> uniqueResponses = new HashSet<>();

        for (TrainingRecord r : records) {
            // Rule 1: Source grounding check (Deterministic)
            if (r.getSourcePath() == null || r.getSourcePath().isEmpty()) {
                reports.add(new QualityReport(r, Recommendation.REJECT, "Missing source grounding path."));
                continue;
            }
            if (r.getEvidence() == null || r.getEvidence().trim().isEmpty()) {
                reports.add(new QualityReport(r, Recommendation.REJECT, "Missing direct source evidence."));
                continue;
            }

            // Rule 2: Basic clarity/length check
            String response = r.getResponse() != null ? r.getResponse().trim() : "";
            if (response.length() < 10) {
                reports.add(new QualityReport(r, Recommendation.REJECT, "Response is too short or clear text is lacking."));
                continue;
            }

            // Rule 3: Duplication detection
            String normalizedResponse = response.toLowerCase().replaceAll("\\s+", " ");
            if (uniqueResponses.contains(normalizedResponse)) {
                reports.add(new QualityReport(r, Recommendation.REJECT, "Duplicate training sample response content."));
                continue;
            }
            uniqueResponses.add(normalizedResponse);

            // Rule 4: Ambiguity or low value indicators
            if (response.contains("unknown") || response.contains("unspecified") || response.contains("not found in content")) {
                reports.add(new QualityReport(r, Recommendation.REVIEW, "Ambiguous answer, potential missing metadata."));
                continue;
            }

            // Rule 5: Hallucination risk checks (if evidence matches nothing in response/instruction, etc. - simplified)
            if (r.getInstruction() != null && r.getInstruction().toLowerCase().contains("hallucinate")) {
                reports.add(new QualityReport(r, Recommendation.REJECT, "Potential hallucination risk detected."));
                continue;
            }

            // Safe ACCEPT
            reports.add(new QualityReport(r, Recommendation.ACCEPT, "High-quality source-grounded training record."));
        }
        return reports;
    }
}
