package eu.kalafatic.evolution.controller.orchestration.dream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FunctionalityInventoryManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;

/**
 * Topic selection engine for automatic background DREAM execution.
 * Evaluates existing platform failure memory, project facts, iteration records, and genome capabilities
 * to rank and pick a high-signal experience candidate with explicit rationale.
 */
public class DreamTopicSelector {

    public static final double MINIMUM_SIGNIFICANCE_THRESHOLD = 0.40;
    public static final long DEFAULT_COOLDOWN_MS = 600000L; // 10 minutes

    private static final Map<String, Long> ANALYZED_CANDIDATE_TIMESTAMPS = new ConcurrentHashMap<>();

    public static class SelectedTopic {
        private final DreamExperienceCandidate candidate;
        private final String topic;
        private final String rationale;
        private final double score;

        public SelectedTopic(DreamExperienceCandidate candidate, String topic, String rationale, double score) {
            this.candidate = candidate;
            this.topic = topic;
            this.rationale = rationale;
            this.score = score;
        }

        public SelectedTopic(String topic, String rationale, double score) {
            this(null, topic, rationale, score);
        }

        public DreamExperienceCandidate getCandidate() {
            return candidate;
        }

        public String getTopic() {
            return topic;
        }

        public String getRationale() {
            return rationale;
        }

        public double getScore() {
            return score;
        }
    }

    public static void recordCandidateAnalyzed(String candidateId) {
        if (candidateId != null && !candidateId.trim().isEmpty()) {
            ANALYZED_CANDIDATE_TIMESTAMPS.put(candidateId, System.currentTimeMillis());
        }
    }

    public static void clearCandidateCooldowns() {
        ANALYZED_CANDIDATE_TIMESTAMPS.clear();
    }

    public SelectedTopic selectBestTopic(FailureMemory failureMemory) {
        return selectBestTopic(failureMemory, null);
    }

    public SelectedTopic selectBestTopic(FailureMemory failureMemory, IterationMemoryService iterationMemoryService) {
        List<DreamExperienceCandidate> candidates = discoverCandidates(failureMemory, iterationMemoryService);
        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        DreamExperienceCandidate top = candidates.get(0);

        if (top.getScore() < MINIMUM_SIGNIFICANCE_THRESHOLD) {
            return null;
        }

        return new SelectedTopic(top, top.getTopic(), top.getRationale(), top.getScore());
    }

    public List<DreamExperienceCandidate> discoverCandidates(FailureMemory failureMemory, IterationMemoryService iterationMemoryService) {
        List<DreamExperienceCandidate> candidates = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Source 1: FailureMemory
        if (failureMemory != null && !failureMemory.getFingerprints().isEmpty()) {
            failureMemory.getFingerprints().forEach((fp, count) -> {
                String candidateId = "failure-memory:" + fp;
                long lastAnalyzed = ANALYZED_CANDIDATE_TIMESTAMPS.getOrDefault(candidateId, 0L);

                double recurrenceBonus = Math.min(count * 0.1, 0.4);
                double baseScore = 0.5 + recurrenceBonus;
                String rationale = "Recurring failure (" + count + " occurrences)";

                if (now - lastAnalyzed < DEFAULT_COOLDOWN_MS) {
                    baseScore *= 0.3; // Defer/downrank due to cooldown
                    rationale += " [In cooldown - previously analyzed]";
                }

                DreamExperienceCandidate candidate = new DreamExperienceCandidate(
                    candidateId, "NEGATIVE", "FailureMemory", "Recurring Failure: " + fp,
                    "Failure fingerprint observed " + count + " times in execution runs",
                    baseScore, rationale, 0.8, count, true, now
                );
                candidate.setLastAnalyzedTime(lastAnalyzed);
                candidate.getEvidenceReferences().add("Fingerprint: " + fp);
                candidates.add(candidate);
            });
        }

        // Source 2: MemoryService entries
        try {
            List<MemoryEntry> entries = MemoryService.getInstance().getAllEntries();
            for (MemoryEntry entry : entries) {
                String contentLower = entry.getContent().toLowerCase();
                boolean isError = contentLower.contains("fail") || contentLower.contains("error") || contentLower.contains("exception");
                boolean isSuccess = contentLower.contains("success") || contentLower.contains("solved") || contentLower.contains("resolved");

                if (entry.getType() == MemoryType.OBSERVATION || entry.getType() == MemoryType.FACT || entry.getType() == MemoryType.EXPERIENCE) {
                    if (isError || isSuccess || entry.getImportance() == MemoryImportance.CRITICAL || entry.getImportance() == MemoryImportance.HIGH) {
                        String candidateId = "memory:" + entry.getId();
                        long lastAnalyzed = ANALYZED_CANDIDATE_TIMESTAMPS.getOrDefault(candidateId, 0L);

                        double baseScore = isError ? 0.65 : (isSuccess ? 0.55 : 0.45);
                        if (entry.getImportance() == MemoryImportance.CRITICAL) baseScore += 0.2;
                        else if (entry.getImportance() == MemoryImportance.HIGH) baseScore += 0.1;

                        String category = isError ? "NEGATIVE" : (isSuccess ? "POSITIVE" : "KNOWLEDGE");
                        String rationale = "Selected from Memory (" + entry.getType() + ", Importance=" + entry.getImportance() + ")";

                        if (now - lastAnalyzed < DEFAULT_COOLDOWN_MS) {
                            baseScore *= 0.3;
                            rationale += " [In cooldown]";
                        }

                        DreamExperienceCandidate candidate = new DreamExperienceCandidate(
                            candidateId, category, "MemoryService", "Memory Observation: " + entry.getContent(),
                            entry.getContent(), baseScore, rationale, isError ? 0.7 : 0.3, 1, isError, entry.getUpdatedAt()
                        );
                        candidate.setLastAnalyzedTime(lastAnalyzed);
                        candidate.getEvidenceReferences().add("Memory ID: " + entry.getId());
                        candidates.add(candidate);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Source 3: IterationMemoryService
        if (iterationMemoryService != null && iterationMemoryService.getRecords() != null) {
            Map<String, Integer> iterationFailures = new HashMap<>();
            for (IterationRecord rec : iterationMemoryService.getRecords()) {
                if (rec != null && ("FAIL".equalsIgnoreCase(rec.getResult()) || rec.getErrorMessage() != null)) {
                    String goal = rec.getGoal() != null ? rec.getGoal() : "Unknown goal";
                    iterationFailures.put(goal, iterationFailures.getOrDefault(goal, 0) + 1);
                }
            }

            iterationFailures.forEach((goal, count) -> {
                String candidateId = "iteration-failure:" + goal.hashCode();
                long lastAnalyzed = ANALYZED_CANDIDATE_TIMESTAMPS.getOrDefault(candidateId, 0L);

                double baseScore = 0.6 + Math.min(count * 0.1, 0.3);
                String rationale = "Failed iteration goal (" + count + " attempts)";

                if (now - lastAnalyzed < DEFAULT_COOLDOWN_MS) {
                    baseScore *= 0.3;
                    rationale += " [In cooldown]";
                }

                DreamExperienceCandidate candidate = new DreamExperienceCandidate(
                    candidateId, "NEGATIVE", "IterationMemoryService", "Failed Iteration Goal: " + goal,
                    "Goal failed across " + count + " iterations",
                    baseScore, rationale, 0.75, count, true, now
                );
                candidate.setLastAnalyzedTime(lastAnalyzed);
                candidate.getEvidenceReferences().add("Goal: " + goal);
                candidates.add(candidate);
            });
        }

        // Source 4: FunctionalityInventoryManager (High Risk/Maintainability capabilities)
        try {
            FunctionalityInventoryManager manager = FunctionalityInventoryManager.getInstance();
            if (manager != null) {
                List<FunctionalityInventoryManager.FunctionalityEntry> entries = manager.discoverCoreFunctionalities(null);
                for (FunctionalityInventoryManager.FunctionalityEntry entry : entries) {
                    if (entry != null) {
                        int riskScore = entry.risk;
                        int centrality = entry.references;

                        if (riskScore >= 60 || centrality >= 70) {
                            String candidateId = "functionality:" + entry.id;
                            long lastAnalyzed = ANALYZED_CANDIDATE_TIMESTAMPS.getOrDefault(candidateId, 0L);

                            double baseScore = 0.4 + (riskScore / 200.0) + (centrality / 200.0);
                            String rationale = "High maintainability risk (" + riskScore + ") or centrality (" + centrality + ") capability";

                            if (now - lastAnalyzed < DEFAULT_COOLDOWN_MS) {
                                baseScore *= 0.3;
                                rationale += " [In cooldown]";
                            }

                            DreamExperienceCandidate candidate = new DreamExperienceCandidate(
                                candidateId, "KNOWLEDGE", "FunctionalityInventory", "Capability Focus: " + entry.name,
                                "Class: " + entry.primaryClass + ", Module: " + entry.module,
                                baseScore, rationale, riskScore / 100.0, 1, false, now
                            );
                            candidate.setLastAnalyzedTime(lastAnalyzed);
                            candidate.getEvidenceReferences().add("Class: " + entry.primaryClass);
                            candidates.add(candidate);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return candidates;
    }
}
