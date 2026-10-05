package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.EvoMetadata;

/**
 * Intent Reconstruction Engine - The "Curiosity-Driven Surgeon".
 * Implements an iterative, depth-first traversal algorithm to reconstruct
 * project intent and architecture without full scans.
 */
public class IntentReconstructionEngine extends ADarwinEngine {

    private final DesignModel designModel = new DesignModel();
    private final Set<String> exploredPaths = new HashSet<>();
    private final Map<String, Integer> packageDiscoveryCount = new HashMap<>();
    private final PriorityQueue<ExplorationNode> explorationQueue = new PriorityQueue<>(
            Comparator.comparingDouble(ExplorationNode::getScore).reversed());
    private final AIContextTool contextTool = new AIContextTool();
    private final MetadataAgent metadataAgent = new MetadataAgent();

    public IntentReconstructionEngine(TaskContext context, IterationMemoryService memoryService,
                                     SystemStateSignalProvider stateProvider) {
        super(context, memoryService, stateProvider, PlatformType.INTENT_RECONSTRUCTION);
        this.designModel.setName("Reconstructed Design Model - " + context.getSessionId());
    }

    @Override
    public EvaluationResult runDarwinIteration(TaskContext context, IterationManager manager) throws Exception {
        context.log("[INTENT_RECONSTRUCTION] Starting Curiosity-Driven Surgeon iteration...");

        if (explorationQueue.isEmpty() && exploredPaths.isEmpty()) {
            initialSeed();
        }

        int filesToRead = 10;
        List<File> batch = selectNextBatch(filesToRead);

        if (batch.isEmpty()) {
            context.log("[INTENT_RECONSTRUCTION] No more files to explore or termination reached.");
            EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(true);
            res.setDecision(SelfDevDecision.STOP);
            return res;
        }

        processBatch(batch);

        // Update Orchestration State with current findings and target purpose summary
        String targetSummary = buildTargetSummary();
        context.getOrchestrationState().getMetadata().put("reconstructedDesignModel", designModel);
        context.getOrchestrationState().getMetadata().put("intentTargetSummary", targetSummary);

        EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        res.setSuccess(true);
        res.setDecision(checkTermination() ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE);

        if (res.getDecision() == SelfDevDecision.STOP) {
            context.log("[INTENT_RECONSTRUCTION] Termination condition met. Reconstruction converged.");
        }

        return res;
    }

    /**
     * Step 0: The "Entry Point" Seed.
     * Scans directory tree and scores folders to find the best starting point.
     */
    private void initialSeed() {
        context.log("[INTENT_RECONSTRUCTION] Step 0: Initial Seeding...");
        File root = context.getProjectRoot();
        scoreDirectory(root, 0);
    }

    private void scoreDirectory(File dir, int depth) {
        if (depth > 3) return; // Limit depth for initial scan

        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isDirectory()) {
                String name = child.getName();
                if (isExcluded(name)) continue;

                double score = 0;
                if (name.equals("src")) score += 50;
                if (name.equals("main")) score += 50;
                if (name.equals("java")) score += 100;
                if (name.equals("api") || name.equals("core") || name.equals("model")) score += 30;
                if (name.contains("controller") || name.contains("service") || name.contains("repository")) score += 20;

                // Identify SUBSYSTEMS based on package depth and naming
                if (depth >= 2 && !name.equals("java") && !name.equals("main") && !name.equals("src")) {
                    ComponentRecord sub = new ComponentRecord();
                    sub.setId("subsystem:" + child.getName());
                    sub.setName(child.getName());
                    sub.setType("SUBSYSTEM");
                    sub.setDescription("Architectural subsystem discovered at depth " + depth);
                    designModel.getComponents().add(sub);
                }

                explorationQueue.add(new ExplorationNode(child, score, depth));
                scoreDirectory(child, depth + 1);
            }
        }
    }

    /**
     * Step 1: The "Peek".
     * Selects files from the exploration queue based on filename heuristics.
     */
    private List<File> selectNextBatch(int limit) {
        List<File> batch = new ArrayList<>();
        List<ExplorationNode> pendingFolders = new ArrayList<>();

        while (batch.size() < limit && !explorationQueue.isEmpty()) {
            ExplorationNode node = explorationQueue.poll();
            if (node.file.isDirectory()) {
                List<File> filesInFolder = peekFolder(node.file);
                for (File f : filesInFolder) {
                    if (batch.size() < limit) {
                        batch.add(f);
                        exploredPaths.add(f.getAbsolutePath());
                    } else {
                        // If we hit limit, we might want to put this folder back or handle it
                        // For now, let's just add the files we can and stop
                    }
                }
                // Also add subfolders to queue
                File[] subdirs = node.file.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File sd : subdirs) {
                        if (!isExcluded(sd.getName())) {
                            explorationQueue.add(new ExplorationNode(sd, node.score * 0.9, node.depth + 1));
                        }
                    }
                }
            } else if (!exploredPaths.contains(node.file.getAbsolutePath())) {
                batch.add(node.file);
                exploredPaths.add(node.file.getAbsolutePath());
            }
        }
        return batch;
    }

    private List<File> peekFolder(File folder) {
        File[] files = folder.listFiles(File::isFile);
        if (files == null) return Collections.emptyList();

        return Arrays.stream(files)
                .filter(f -> !f.getName().endsWith(".ai.json") && !exploredPaths.contains(f.getAbsolutePath()))
                .sorted(Comparator.comparingDouble(this::scoreFileName).reversed())
                .collect(Collectors.toList());
    }

    private double scoreFileName(File file) {
        String name = file.getName();
        if (name.contains("Application") || name.contains("Main") || name.equals("main.js") || name.equals("index.js")) return 10;
        if (name.equalsIgnoreCase("README.md") || name.contains("ARCHITECTURE") || name.equals("package.json") || name.equals("pom.xml")) return 10;
        if (name.contains("Controller") || name.contains("Resource") || name.contains("Router")) return 9;
        if (name.contains("Service") || name.contains("Manager") || name.contains("Handler")) return 8;
        if (name.contains("Repository") || name.contains("Dao") || name.contains("Model")) return 7;
        if (name.contains("Config") || name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".yml")) return 6;
        if (name.endsWith(".md") || name.endsWith(".txt")) return 5;
        if (name.contains("Util")) return 1;
        return 5;
    }

    /**
     * Step 2 & 3: Read Metadata and Update Knowledge Graph.
     */
    private void processBatch(List<File> batch) {
        context.log("[INTENT_RECONSTRUCTION] Processing batch of " + batch.size() + " files.");
        for (File file : batch) {
            EvoMetadata meta = contextTool.loadMetadata(file);
            if (meta == null) {
                // Generate metadata if missing (lazy generation)
                metadataAgent.generate(file.getParentFile()); // Simplification: generate for parent
                meta = contextTool.loadMetadata(file);
            }

            if (meta != null) {
                updateDesignModel(file, meta);
                discoverNewHooks(meta);
            }
        }
    }

    private void updateDesignModel(File file, EvoMetadata meta) {
        ComponentRecord record = new ComponentRecord();
        record.setId(meta.getPath());
        record.setName(file.getName());

        String name = file.getName();
        String lowerName = name.toLowerCase();

        // Categorize component type based on structure or document nature
        if (name.endsWith("Controller.java") || name.endsWith("Resource.java") ||
            name.endsWith("Service.java") || name.endsWith("UseCase.java") ||
            name.endsWith("Handler.java") || name.endsWith("Facade.java") ||
            name.endsWith("Application.java") || lowerName.endsWith("router.js") ||
            lowerName.endsWith("controller.js") || lowerName.endsWith("app.js")) {
            record.setType("USE_CASE");
        } else if (lowerName.endsWith(".md") || lowerName.endsWith(".txt") || "documentation".equalsIgnoreCase(meta.getRole())) {
            record.setType("DOCUMENTATION");
        } else if (lowerName.endsWith(".json") || lowerName.endsWith(".csv") || lowerName.endsWith(".evodata") || "data".equalsIgnoreCase(meta.getRole())) {
            record.setType("DATASET");
        } else if (file.isDirectory() || "subsystem".equalsIgnoreCase(meta.getRole())) {
            record.setType("SUBSYSTEM");
        } else {
            record.setType(meta.getRole() != null ? meta.getRole().toUpperCase() : "COMPONENT");
        }

        record.setDescription(meta.getSummary());
        record.setPath(meta.getPath());
        record.setImportanceScore(meta.getImportanceScore());

        // Extract method or topic intent
        if (meta.getSummary() != null) {
            String summary = meta.getSummary().toLowerCase();
            if (summary.contains("create") || summary.contains("post") || summary.contains("save")) record.getUseCases().add("Command: Create/Save");
            if (summary.contains("find") || summary.contains("get") || summary.contains("search")) record.getUseCases().add("Query: Find/Search");
            if (summary.contains("delete") || summary.contains("remove")) record.getUseCases().add("Command: Delete/Remove");
            if (summary.contains("update") || summary.contains("put") || summary.contains("patch")) record.getUseCases().add("Command: Update");
            if (summary.contains("orchestrat") || summary.contains("workflow")) record.getUseCases().add("Process: Orchestration/Workflow");
            if (summary.contains("chat") || summary.contains("conversation")) record.getUseCases().add("Interaction: Chat/Messaging");
            if (summary.contains("model") || summary.contains("train") || summary.contains("infer")) record.getUseCases().add("AI/ML: Training/Inference");
        }

        designModel.getComponents().add(record);

        // Add relationships from metadata
        if (meta.getDependencyLinks() != null) {
            for (String dep : meta.getDependencyLinks()) {
                RelationshipRecord rel = new RelationshipRecord();
                rel.setFrom(record.getId());
                rel.setTo(dep);
                rel.setType("DEPENDS_ON");
                designModel.getRelationships().add(rel);
            }
        }
    }

    private void discoverNewHooks(EvoMetadata meta) {
        // Step 3 & 4: Update the Knowledge Graph & Discover New "Hooks"
        if (meta.getDependencyLinks() != null) {
            for (String link : meta.getDependencyLinks()) {
                // Centrality Score: Track how often a package is imported
                packageDiscoveryCount.put(link, packageDiscoveryCount.getOrDefault(link, 0) + 1);

                // Find folders matching this package and boost their score in the queue
                boostPackageScore(link);
            }
        }
    }

    private void boostPackageScore(String packageName) {
        // Novelty/Centrality Score boosting logic
        // We find nodes in the exploration queue that match this package and increase their score
        for (ExplorationNode node : explorationQueue) {
            String path = node.file.getAbsolutePath().replace(File.separatorChar, '.');
            if (path.contains(packageName)) {
                node.score += 15; // Boost based on discovery

                // Re-sort the queue (by removing and adding back)
                explorationQueue.remove(node);
                explorationQueue.add(node);
                break;
            }
        }
    }

    private boolean checkTermination() {
        // Convergence: reaching certain depth or saturation of core packages
        if (exploredPaths.size() > 50) return true; // Safety cap
        if (explorationQueue.isEmpty()) return true;

        // Diminishing returns: top score in queue is too low
        ExplorationNode next = explorationQueue.peek();
        if (next != null && next.score < 5) return true;

        return false;
    }

    private boolean isExcluded(String name) {
        return name.startsWith(".") || name.equals("target") || name.equals("build")
                || name.equals("node_modules") || name.equals("bin");
    }

    private String buildTargetSummary() {
        File root = context.getProjectRoot();
        long useCaseCount = designModel.getComponents().stream().filter(c -> "USE_CASE".equals(c.getType())).count();
        long subsystemCount = designModel.getComponents().stream().filter(c -> "SUBSYSTEM".equals(c.getType()) || "DOMAIN".equals(c.getType())).count();
        long docCount = designModel.getComponents().stream().filter(c -> "DOCUMENTATION".equals(c.getType())).count();
        long dataCount = designModel.getComponents().stream().filter(c -> "DATASET".equals(c.getType()) || "DATA".equals(c.getType())).count();

        List<String> topComponents = designModel.getComponents().stream()
                .filter(c -> c.getName() != null && !c.getName().isEmpty())
                .limit(10)
                .map(c -> c.getName() + " (" + c.getType() + (c.getDescription() != null && !c.getDescription().isEmpty() ? ": " + limitString(c.getDescription(), 80) : "") + ")")
                .collect(Collectors.toList());

        List<String> discoveredUseCases = designModel.getComponents().stream()
                .flatMap(c -> c.getUseCases().stream())
                .distinct()
                .limit(10)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("Reconstructed Target Purpose & Knowledge:\n");
        sb.append("Target Path: ").append(root != null ? root.getAbsolutePath() : "Unknown").append("\n");

        if (useCaseCount > 0 || subsystemCount > 0) {
            sb.append("Target Nature: Structured Codebase (Subsystems: ").append(subsystemCount).append(", Use Cases/Endpoints: ").append(useCaseCount).append(")\n");
        } else if (docCount > 0 || dataCount > 0) {
            sb.append("Target Nature: Knowledge/Document Repository (Docs: ").append(docCount).append(", Datasets: ").append(dataCount).append(")\n");
        } else {
            sb.append("Target Nature: Heterogeneous Artifact Repository (Analyzed Components: ").append(designModel.getComponents().size()).append(")\n");
        }

        if (!topComponents.isEmpty()) {
            sb.append("Discovered Core Artifacts:\n");
            for (String comp : topComponents) {
                sb.append(" - ").append(comp).append("\n");
            }
        }

        if (!discoveredUseCases.isEmpty()) {
            sb.append("Discovered Capability Actions:\n");
            for (String uc : discoveredUseCases) {
                sb.append(" - ").append(uc).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private String limitString(String str, int max) {
        if (str == null) return "";
        String trimmed = str.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= max) return trimmed;
        return trimmed.substring(0, max) + "...";
    }

    @Override
    public String getMode() {
        return "INTENT_RECONSTRUCTION";
    }

    @Override
    public CapabilityType getCapabilityType() {
        return CapabilityType.INTENT_RECONSTRUCTION;
    }

    private static class ExplorationNode {
        File file;
        double score;
        int depth;

        ExplorationNode(File file, double score, int depth) {
            this.file = file;
            this.score = score;
            this.depth = depth;
        }

        double getScore() { return score; }
    }
}
