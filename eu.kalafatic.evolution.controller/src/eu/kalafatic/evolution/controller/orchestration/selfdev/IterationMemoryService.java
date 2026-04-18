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
        if (records == null || records.isEmpty()) return "No previous iteration history available.";

        StringBuilder sb = new StringBuilder();
        sb.append("Analysis of ").append(records.size()).append(" recent variants:\n");

        // Analyze trends
        long successfulCount = records.stream().filter(r -> "SUCCESS".equals(r.getResult())).count();
        double successRate = (double) successfulCount / records.size();
        sb.append("- Overall Success Rate: ").append(String.format("%.1f%%", successRate * 100)).append("\n");

        // Recurring Failures
        Map<String, Long> failurePatterns = records.stream()
                .filter(r -> "FAIL".equals(r.getResult()))
                .collect(Collectors.groupingBy(r -> r.getStrategy(), Collectors.counting()));

        List<Map.Entry<String, Long>> frequentFailures = failurePatterns.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .collect(Collectors.toList());

        if (!frequentFailures.isEmpty()) {
            sb.append("- RECURRING FAILURE PATTERNS (CRITICAL: AVOID THESE):\n");
            for (Map.Entry<String, Long> e : frequentFailures) {
                sb.append("  * Pattern: '").append(e.getKey()).append("' (failed ").append(e.getValue()).append(" times)\n");
            }
        }

        // Recent Trajectory (last 5)
        sb.append("- Recent Trajectory: ");
        int lastN = Math.min(5, records.size());
        List<IterationRecord> recent = records.subList(records.size() - lastN, records.size());
        for (IterationRecord r : recent) {
            sb.append("SUCCESS".equals(r.getResult()) ? "📈" : "📉");
        }
        sb.append("\n");

        return sb.toString();
    }
}
