package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.kalafatic.evolution.controller.orchestration.Checkpoint;
import eu.kalafatic.evolution.controller.orchestration.workspace.TrajectoryMemory;
import eu.kalafatic.evolution.controller.supervision.AuditRecord;
import eu.kalafatic.evolution.controller.trajectory.TrajectoryAnalysisRecord;
import eu.kalafatic.evolution.controller.log.Log;


public class IterationMemoryService {
    private File projectRoot;
    private final File memoryDir;
    private final File auditFile;
    private final ObjectMapper mapper;
    private List<IterationRecord> records = new CopyOnWriteArrayList<>();
    private List<TrajectoryAnalysisRecord> trajectoryAnalyses = new CopyOnWriteArrayList<>();
    private Map<String, List<IterationRecord>> errorIndex = new HashMap<>();
    private Map<String, Integer> architectureHotspots = new HashMap<>();
    private final FailureMemory failureMemory = new FailureMemory();
    private final TrajectoryMemory trajectoryMemory = new TrajectoryMemory();
    private final EvolutionMemoryGraph evolutionGraph = new EvolutionMemoryGraph();
    private final EvolutionTree evolutionTree = new EvolutionTree();

    private long lastMemoryDirModified = 0;
    private long lastIterationsDirModified = 0;
    private boolean initialLoadDone = false;

    public IterationMemoryService(File projectRoot) {
        this.projectRoot = findEffectiveRoot(projectRoot);
        this.memoryDir = new File(this.projectRoot, "orchestrator/memory");
        if (!memoryDir.exists()) {
            memoryDir.mkdirs();
        }
        this.auditFile = new File(this.projectRoot, "orchestrator/audit_trail.jsonl");
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        refresh();
        Log.log("[MEMORY] IterationMemoryService initialized with root: " + this.projectRoot.getAbsolutePath() + " (found " + records.size() + " records)");
    }

    public synchronized boolean refresh() {
        long currentMemoryMod = memoryDir.exists() ? memoryDir.lastModified() : 0;
        File iterationsDir = new File(projectRoot, "iterations");
        long currentIterationsMod = iterationsDir.exists() ? iterationsDir.lastModified() : 0;

        if (currentMemoryMod > lastMemoryDirModified || currentIterationsMod > lastIterationsDirModified || !initialLoadDone) {
            List<IterationRecord> newRecords = new CopyOnWriteArrayList<>();
            Map<String, List<IterationRecord>> newErrorIndex = new HashMap<>();

            loadRecordsTo(newRecords, newErrorIndex);
            loadFromIterationsDirTo(newRecords, newErrorIndex);

            this.records = newRecords;
            this.errorIndex = newErrorIndex;
            loadEvolutionTree();

            lastMemoryDirModified = currentMemoryMod;
            lastIterationsDirModified = currentIterationsMod;
            initialLoadDone = true;
            return true;
        }
        return false;
    }

    private void loadRecordsTo(List<IterationRecord> targetRecords, Map<String, List<IterationRecord>> targetErrorIndex) {
        File[] files = memoryDir.listFiles((dir, name) -> name.startsWith("iteration_") && name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    IterationRecord record = mapper.readValue(file, IterationRecord.class);
                    targetRecords.add(record);
                    indexRecordTo(record, targetErrorIndex);
                } catch (IOException e) {
                    // Log error but continue
                    System.err.println("Failed to load iteration record: " + file.getName() + " - " + e.getMessage());
                }
            }
        }
    }

    private File findEffectiveRoot(File startDir) {
        if (startDir == null) return new File(".").getAbsoluteFile().getParentFile();
        File current = startDir.getAbsoluteFile();
        while (current != null) {
            if (new File(current, "iterations").exists() ||
                new File(current, "orchestrator").exists() ||
                new File(current, ".git").exists() ||
                new File(current, "pom.xml").exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        return startDir;
    }

    private void loadFromIterationsDirTo(List<IterationRecord> targetRecords, Map<String, List<IterationRecord>> targetErrorIndex) {
        File iterationsDir = new File(projectRoot, "iterations");
        if (!iterationsDir.exists() || !iterationsDir.isDirectory()) {
            // Try one more time with a relative path if projectRoot failed us
            iterationsDir = new File("iterations");
            if (!iterationsDir.exists()) return;
        }

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
                    boolean exists = targetRecords.stream().anyMatch(r -> r.getIteration() == record.getIteration() && record.getBranch().equals(r.getBranch()));
                    if (!exists) {
                        targetRecords.add(record);
                        indexRecordTo(record, targetErrorIndex);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load plan.json from " + dir.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void indexRecordTo(IterationRecord record, Map<String, List<IterationRecord>> targetErrorIndex) {
        if (record.getErrorMessage() != null && !record.getErrorMessage().isEmpty()) {
            String normalizedError = normalizeError(record.getErrorMessage());
            targetErrorIndex.computeIfAbsent(normalizedError, k -> new ArrayList<>()).add(record);
            failureMemory.addFingerprint(normalizedError);
            failureMemory.recordStrategyFailure(record.getStrategy());
        }

        // Hotspot tracking (P1)
        if (record.getChangedFiles() != null) {
            for (String file : record.getChangedFiles()) {
                architectureHotspots.put(file, architectureHotspots.getOrDefault(file, 0) + 1);
            }
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
        Log.log("[MEMORY] saveRecord: ID=" + record.getBranchId() + ", Result=" + record.getResult() + ", Strategy=" + record.getStrategy());
        records.add(record);
        indexRecordTo(record, errorIndex);
        String fileName = String.format("iteration_%d_%d.json", record.getIteration(), record.getTimestamp());
        File file = new File(memoryDir, fileName);
        try {
            mapper.writeValue(file, record);
        } catch (IOException e) {
            System.err.println("Failed to save iteration record: " + e.getMessage());
        }
    }

    /**
     * Persists a TrajectoryAnalysisRecord to the structured evolutionary memory.
     */
    public void saveTrajectoryAnalysis(TrajectoryAnalysisRecord record) {
        Log.log("[MEMORY] saveTrajectoryAnalysis: Branch=" + record.getBranchId() + ", Fitness=" + record.getFitnessScore());
        trajectoryAnalyses.add(record);
        String fileName = String.format("trajectory_%s_%s_%d.json",
            record.getIterationId(), record.getBranchId(), record.getTimestamp());
        File file = new File(memoryDir, fileName);
        try {
            mapper.writeValue(file, record);
        } catch (IOException e) {
            System.err.println("Failed to save trajectory analysis record: " + e.getMessage());
        }
    }

    /**
     * Appends an AuditRecord to the immutable audit trail.
     */
    public synchronized void appendAuditRecord(AuditRecord record) {
        try {
            String json = mapper.writeValueAsString(record);
            java.nio.file.Files.writeString(auditFile.toPath(), json + "\n",
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to append audit record: " + e.getMessage());
        }
    }

    /**
     * Ensures all pending records are flushed to disk and synchronized with the OS.
     */
    public void flush() {
        Log.log("[MEMORY] Flushing memory service buffers to disk.");
        // Jackson mapper.writeValue calls in this class are currently direct-to-file,
        // but we ensure memory directory metadata is updated.
        if (memoryDir.exists()) {
            memoryDir.setLastModified(System.currentTimeMillis());
        }
    }

    /**
     * Retrieves active lineage iteration records (winners of previous iterations).
     */
    public List<IterationRecord> getActiveLineage() {
        return records.stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .collect(Collectors.toList());
    }

    /**
     * Saves a full runtime checkpoint of the current evolutionary state for restart recovery.
     */
    public void saveCheckpoint(Checkpoint checkpoint) {
        if (checkpoint == null || checkpoint.getSessionId() == null) return;

        File checkpointFile = new File(memoryDir, "checkpoint_" + checkpoint.getSessionId() + ".json");
        checkpoint.setTimestamp(System.currentTimeMillis());
        try {
            mapper.writeValue(checkpointFile, checkpoint);
            Log.log("[MEMORY] Full runtime checkpoint saved for session: " + checkpoint.getSessionId() + ", Phase=" + checkpoint.getCurrentPhase());
        } catch (IOException e) {
            System.err.println("Failed to save checkpoint: " + e.getMessage());
        }
    }

    /**
     * Loads the last saved checkpoint for a session.
     */
    public Checkpoint loadCheckpoint(String sessionId) {
        File checkpointFile = new File(memoryDir, "checkpoint_" + sessionId + ".json");
        if (!checkpointFile.exists()) return null;
        try {
            Checkpoint checkpoint = mapper.readerFor(Checkpoint.class).readValue(checkpointFile);
            validateCheckpoint(checkpoint);
            return checkpoint;
        } catch (Exception e) {
            Log.log("[MEMORY] Failed to load checkpoint: " + e.getMessage() + ". Starting fresh.");
            return null;
        }
    }

    private void validateCheckpoint(Checkpoint checkpoint) {
        if (checkpoint.getSessionId() == null) {
            throw new RuntimeException("Invalid checkpoint: Missing sessionId");
        }
        if (checkpoint.getCurrentPhase() == null) {
            Log.log("[MEMORY] Warning: Checkpoint missing currentPhase. Defaulting to INTENT_EXPANSION.");
            checkpoint.setCurrentPhase(eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants.PHASE_INTENT_EXPANSION);
        }
        if (checkpoint.getTimestamp() == 0) {
            throw new RuntimeException("Invalid checkpoint: Missing timestamp");
        }
    }

    /**
     * Retrieves all trajectory analysis records.
     */
    public List<TrajectoryAnalysisRecord> getTrajectoryAnalyses() {
        if (!trajectoryAnalyses.isEmpty()) {
            return new ArrayList<>(trajectoryAnalyses);
        }
        List<TrajectoryAnalysisRecord> analyses = new ArrayList<>();
        File[] files = memoryDir.listFiles((dir, name) -> name.startsWith("trajectory_") && name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    analyses.add(mapper.readValue(file, TrajectoryAnalysisRecord.class));
                } catch (IOException e) {
                    System.err.println("Failed to load trajectory analysis: " + file.getName());
                }
            }
        }
        return analyses;
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

    public Map<String, Integer> getArchitectureHotspots() {
        return architectureHotspots;
    }

    public TrajectoryMemory getTrajectoryMemory() {
        return trajectoryMemory;
    }

    public EvolutionMemoryGraph getEvolutionGraph() {
        return evolutionGraph;
    }

    public EvolutionTree getEvolutionTree() {
        return evolutionTree;
    }

    private void loadEvolutionTree() {
        File treeFile = new File(memoryDir, "evolution_tree.json");
        if (treeFile.exists()) {
            try {
                Map<String, EvolutionNode> nodes = mapper.readValue(treeFile,
                    mapper.getTypeFactory().constructMapType(HashMap.class, String.class, EvolutionNode.class));
                evolutionTree.setNodes(nodes);

                // Reconstruct root and current winner
                nodes.values().forEach(node -> {
                    if (node.getParentId() == null) evolutionTree.setRootId(node.getId());
                    if ("ACTIVE".equals(node.getStatus())) evolutionTree.setCurrentWinnerId(node.getId());
                });

                Log.log("[MEMORY] EvolutionTree loaded with " + nodes.size() + " nodes.");
            } catch (IOException e) {
                System.err.println("Failed to load evolution tree: " + e.getMessage());
            }
        }
    }

    public synchronized void saveEvolutionTree() {
        File treeFile = new File(memoryDir, "evolution_tree.json");
        try {
            mapper.writeValue(treeFile, evolutionTree.getNodes());
            Log.log("[MEMORY] EvolutionTree persisted to disk.");
        } catch (IOException e) {
            System.err.println("Failed to save evolution tree: " + e.getMessage());
        }
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

        // Activation State Analysis
        long activeCount = records.stream().filter(r -> "ACTIVE".equals(r.getActivationState())).count();
        sb.append("- Explicitly Activated Lineage Branches: ").append(activeCount).append("\n");

        return sb.toString();
    }
}
