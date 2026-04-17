package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


public class IterationMemoryService {
    private final File memoryDir;
    private final ObjectMapper mapper;
    private final List<IterationRecord> records = new ArrayList<>();
    private final Map<String, List<IterationRecord>> errorIndex = new HashMap<>();

    public IterationMemoryService(File projectRoot) {
        this.memoryDir = new File(projectRoot, "orchestrator/memory");
        if (!memoryDir.exists()) {
            memoryDir.mkdirs();
        }
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        loadRecords();
    }

    private void loadRecords() {
        File[] files = memoryDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    IterationRecord record = mapper.readValue(file, IterationRecord.class);
                    records.add(record);
                    indexRecord(record);
                } catch (IOException e) {
                    // Log error but continue
                    System.err.println("Failed to load iteration record: " + file.getName() + " - " + e.getMessage());
                }
            }
        }
    }

    private void indexRecord(IterationRecord record) {
        if (record.getErrorMessage() != null && !record.getErrorMessage().isEmpty()) {
            String normalizedError = normalizeError(record.getErrorMessage());
            errorIndex.computeIfAbsent(normalizedError, k -> new ArrayList<>()).add(record);
        }
    }

    private String normalizeError(String errorMessage) {
        // Simple normalization: first line or exception name
        String firstLine = errorMessage.split("\n")[0];
        return firstLine.replaceAll("@[a-f0-9]+", "").trim();
    }

    public void saveRecord(IterationRecord record) {
        records.add(record);
        indexRecord(record);
        String fileName = String.format("iteration_%d_%d.json", record.getIteration(), record.getTimestamp());
        File file = new File(memoryDir, fileName);
        try {
            mapper.writeValue(file, record);
        } catch (IOException e) {
            System.err.println("Failed to save iteration record: " + e.getMessage());
        }
    }

    public List<IterationRecord> findByError(String error) {
        String normalized = normalizeError(error);
        return errorIndex.getOrDefault(normalized, new ArrayList<>());
    }

    public List<IterationRecord> findSuccessfulPatterns(String goal) {
        return records.stream()
                .filter(r -> "SUCCESS".equals(r.getResult()))
                .filter(r -> r.getGoal() != null && r.getGoal().toLowerCase().contains(goal.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<IterationRecord> getRecords() {
        return records;
    }

    public Map<String, List<IterationRecord>> getErrorIndex() {
        return errorIndex;
    }

    public String getHistoryAnalysis() {
        if (records.isEmpty()) {
            return "No previous iteration history available.";
        }

        StringBuilder analysis = new StringBuilder();
        analysis.append("History Analysis:\n");

        List<IterationRecord> successful = records.stream()
                .filter(r -> "SUCCESS".equals(r.getResult()))
                .collect(Collectors.toList());

        List<IterationRecord> failed = records.stream()
                .filter(r -> "FAIL".equals(r.getResult()))
                .collect(Collectors.toList());

        if (!successful.isEmpty()) {
            analysis.append("- Successful Strategies:\n");
            successful.stream().map(IterationRecord::getStrategy).distinct().forEach(s -> analysis.append("  * ").append(s).append("\n"));
        }

        if (!failed.isEmpty()) {
            analysis.append("- Failed Strategies (Avoid these):\n");
            failed.stream().map(IterationRecord::getStrategy).distinct().forEach(s -> analysis.append("  * ").append(s).append("\n"));

            analysis.append("- Common Errors:\n");
            failed.stream().map(IterationRecord::getErrorMessage)
                .filter(e -> e != null && !e.isEmpty())
                .map(this::normalizeError)
                .distinct()
                .forEach(e -> analysis.append("  * ").append(e).append("\n"));
        }

        Map<String, PatternStats> stats = getPatternStats();
        if (!stats.isEmpty()) {
            analysis.append("- Pattern Success Rates:\n");
            stats.forEach((p, s) -> analysis.append("  * ").append(p).append(": ")
                .append(String.format("%.1f%% (%d/%d)", s.getSuccessRate() * 100, s.successCount, s.totalCount())).append("\n"));
        }

        return analysis.toString();
    }

    public Map<String, PatternStats> getPatternStats() {
        Map<String, PatternStats> stats = new HashMap<>();
        for (IterationRecord r : records) {
            if (r.getStrategy() == null) continue;
            String pattern = r.getStrategy().toLowerCase().split(" ")[0]; // Very simple pattern extraction
            PatternStats s = stats.computeIfAbsent(pattern, k -> new PatternStats());
            if ("SUCCESS".equals(r.getResult())) s.successCount++;
            else s.failureCount++;
        }
        return stats;
    }

    public static class PatternStats {
        public int successCount;
        public int failureCount;
        public double getSuccessRate() {
            int total = totalCount();
            return total == 0 ? 0 : (double) successCount / total;
        }
        public int totalCount() { return successCount + failureCount; }
    }
}
