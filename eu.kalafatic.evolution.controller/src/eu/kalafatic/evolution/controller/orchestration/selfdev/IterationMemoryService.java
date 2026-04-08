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

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

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
}
