package eu.kalafatic.evolution.controller.orchestration.dream;

import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;

/**
 * Topic selection engine for automatic background DREAM execution.
 * Evaluates existing platform failure memory, project facts, and records to pick a high-signal topic with explicit rationale.
 */
public class DreamTopicSelector {

    public static class SelectedTopic {
        private final String topic;
        private final String rationale;
        private final double score;

        public SelectedTopic(String topic, String rationale, double score) {
            this.topic = topic;
            this.rationale = rationale;
            this.score = score;
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

    public SelectedTopic selectBestTopic(FailureMemory failureMemory) {
        List<SelectedTopic> candidates = new ArrayList<>();

        if (failureMemory != null && !failureMemory.getFingerprints().isEmpty()) {
            failureMemory.getFingerprints().forEach((fp, count) -> {
                double score = 0.5 + Math.min(count * 0.1, 0.4);
                String rationale = "Selected because failure occurred " + count + " times in recent execution runs";
                candidates.add(new SelectedTopic("Recurring Failure: " + fp, rationale, score));
            });
        }

        List<MemoryEntry> entries = MemoryService.getInstance().getAllEntries();
        for (MemoryEntry entry : entries) {
            if (entry.getType() == MemoryType.OBSERVATION || entry.getType() == MemoryType.FACT) {
                if (entry.getContent().toLowerCase().contains("fail") || entry.getContent().toLowerCase().contains("error")) {
                    candidates.add(new SelectedTopic("Memory Observation: " + entry.getContent(),
                        "Selected based on high-importance memory entry with error evidence", 0.7));
                }
            }
        }

        if (candidates.isEmpty()) {
            return new SelectedTopic("System Architecture Optimization",
                "Default fallback topic as no active recurring failures or unanalyzed error memories were found", 0.3);
        }

        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return candidates.get(0);
    }
}
