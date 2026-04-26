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
    private final File projectRoot;
    private final File memoryDir;
    private final ObjectMapper mapper;
    private final List<IterationRecord> records = new ArrayList<>();
    private final Map<String, List<IterationRecord>> errorIndex = new HashMap<>();
    private final FailureMemory failureMemory = new FailureMemory();

    public IterationMemoryService(File projectRoot) {
        this.projectRoot = projectRoot;
        this.memoryDir = new File(projectRoot, "orchestrator/memory");
        if (!memoryDir.exists()) {
            memoryDir.mkdirs();
        }
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        loadRecords();
        loadFromIterationsDir();
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

    private void loadFromIterationsDir() {
        File iterationsDir = new File(projectRoot, "iterations");
        if (!iterationsDir.exists() || !iterationsDir.isDirectory()) return;

        File[] dirs = iterationsDir.listFiles(File::isDirectory);
        if (dirs == null) return;

        for (File dir : dirs) {
            File planFile = new File(dir, "plan.json");
            if (planFile.exists()) {
                try {
                    Map<String, Object> plan = mapper.readValue(planFile, Map.class);
                    IterationRecord record = new IterationRecord();

                    Object iter = plan.get("iteration");
                    if (iter instanceof Number) record.setIteration(((Number) iter).intValue());
                    else if (iter instanceof String) record.setIteration(Integer.parseInt((String) iter));

                    String variant = (String) plan.get("variant");
                    record.setBranch("it-" + record.getIteration() + (variant != null ? "-" + variant : ""));
                    record.setStrategy("Imported from " + planFile.getPath());
                    record.setGoal("Autonomous improvement");
                    record.setResult("SUCCESS");
                    record.setTimestamp(planFile.lastModified());

                    Object files = plan.get("files");
                    if (files instanceof List) {
                        record.setChangedFiles((List<String>) files);
                    }

                    // Check if already loaded from memoryDir to avoid duplicates
                    boolean exists = records.stream().anyMatch(r -> r.getIteration() == record.getIteration() && record.getBranch().equals(r.getBranch()));
                    if (!exists) {
                        records.add(record);
                        indexRecord(record);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load plan.json from " + dir.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void indexRecord(IterationRecord record) {
        if (record.getErrorMessage() != null && !record.getErrorMessage().isEmpty()) {
            String normalizedError = normalizeError(record.getErrorMessage());
            errorIndex.computeIfAbsent(normalizedError, k -> new ArrayList<>()).add(record);
            failureMemory.addFingerprint(normalizedError);
        }
    }

    private String normalizeError(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) return "Unknown";
        String firstLine = errorMessage.split("\n")[0];
        String type = "Unknown";
        if (firstLine.contains("Compilation") || firstLine.contains("COMPILATION ERROR")) type = "compiler";
        else if (firstLine.contains("Test") || firstLine.contains("There are test failures")) type = "test";
        else if (firstLine.contains("Exception") || firstLine.contains("Error")) type = "runtime";

        // Try to find a location
        String location = "Global";
        java.util.regex.Pattern locPattern = java.util.regex.Pattern.compile("([a-zA-Z0-9_]+\\.java:[0-9]+)");
        java.util.regex.Matcher locMatcher = locPattern.matcher(errorMessage);
        if (locMatcher.find()) {
            location = locMatcher.group(1);
        }

        String cause = firstLine.replaceAll("@[a-f0-9]+", "").trim();
        return type + "@" + location + ":" + cause;
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

    public FailureMemory getFailureMemory() {
        return failureMemory;
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
