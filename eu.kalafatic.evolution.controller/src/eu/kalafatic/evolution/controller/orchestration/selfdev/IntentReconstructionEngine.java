package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.EvolutionPhaseMachine;
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

        if (context.getProjectRoot() == null || !context.getProjectRoot().exists()) {
            context.log("[INTENT_RECONSTRUCTION] [ERROR] Target path is invalid or unresolvable: " + context.getProjectRoot());
            EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
            res.setSuccess(false);
            res.setDecision(SelfDevDecision.STOP);
            return res;
        }

        if (explorationQueue.isEmpty() && exploredPaths.isEmpty()) {
            initialSeed();
        }

        int filesToRead = 10;
        List<File> batch = selectNextBatch(filesToRead);

        if (batch.isEmpty()) {
            context.log("[INTENT_RECONSTRUCTION] No more files to explore or termination reached.");
            context.getOrchestrationState().setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.FINAL_SYNTHESIS));
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

        boolean converged = checkTermination();
        if (converged) {
            context.getOrchestrationState().setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.FINAL_SYNTHESIS));
            context.log("[INTENT_RECONSTRUCTION] Termination condition met. Reconstruction converged.");
        } else {
            context.getOrchestrationState().setCurrentPhase(EvolutionPhaseMachine.toLegacyString(EvolutionPhase.INTENT_EXPANSION));
        }

        EvaluationResult res = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        res.setSuccess(true);
        res.setDecision(converged ? SelfDevDecision.STOP : SelfDevDecision.CONTINUE);

        return res;
    }

    /**
     * Step 0: The "Entry Point" Seed.
     * Scans directory tree and scores folders to find the best starting point.
     */
    private void initialSeed() {
        context.log("[INTENT_RECONSTRUCTION] Step 0: Initial Seeding...");
        File root = context.getProjectRoot();
        if (root != null && root.exists()) {
            explorationQueue.add(new ExplorationNode(root, 100.0, 0));
            scoreDirectory(root, 0);
        }
    }

    private void scoreDirectory(File dir, int depth) {
        if (depth > 5) return; // Allow deeper directory scans

        File[] children = dir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isDirectory()) {
                String name = child.getName();
                if (isExcluded(name)) continue;
                String absPath = child.getAbsolutePath();

                double score = 10.0;
                String lower = name.toLowerCase();

                // Software / Java
                if (lower.equals("src") || lower.equals("main") || lower.equals("java") || lower.equals("app")) score += 50;
                if (lower.contains("controller") || lower.contains("service") || lower.contains("repository") || lower.contains("api") || lower.contains("core") || lower.contains("model")) score += 30;

                // Web
                if (lower.contains("components") || lower.contains("pages") || lower.contains("routes") || lower.contains("views") || lower.contains("public") || lower.contains("styles") || lower.contains("scripts")) score += 30;

                // Media / Audio / Video
                if (lower.contains("audio") || lower.contains("music") || lower.contains("tracks") || lower.contains("songs") || lower.contains("video") || lower.contains("media") || lower.contains("images") || lower.contains("photos")) score += 30;

                // Documents / Presentations / Knowledge
                if (lower.contains("docs") || lower.contains("documentation") || lower.contains("slides") || lower.contains("presentation") || lower.contains("papers") || lower.contains("books") || lower.contains("articles")) score += 30;

                // Datasets
                if (lower.contains("data") || lower.contains("datasets") || lower.contains("records") || lower.contains("samples") || lower.contains("corpus")) score += 30;

                // Identify SUBSYSTEMS based on package depth and naming
                if (depth >= 1 && !lower.equals("java") && !lower.equals("main") && !lower.equals("src")) {
                    String subId = "subsystem:" + child.getName();
                    if (designModel.getComponents().stream().noneMatch(c -> subId.equalsIgnoreCase(c.getId()))) {
                        ComponentRecord sub = new ComponentRecord();
                        sub.setId(subId);
                        sub.setName(child.getName());
                        sub.setType("SUBSYSTEM");
                        sub.setDescription("Discovered structural subsystem: " + child.getName());
                        designModel.getComponents().add(sub);
                    }
                }

                if (!exploredPaths.contains(absPath)) {
                    explorationQueue.add(new ExplorationNode(child, score, depth));
                }
                scoreDirectory(child, depth + 1);
            }
        }
    }

    /**
     * Step 1: The "Peek".
     * Selects files from the exploration queue based on filename heuristics without discarding unqueued files.
     */
    private List<File> selectNextBatch(int limit) {
        List<File> batch = new ArrayList<>();

        while (batch.size() < limit && !explorationQueue.isEmpty()) {
            ExplorationNode node = explorationQueue.poll();
            if (node.file == null || !node.file.exists()) continue;

            if (node.file.isDirectory()) {
                List<File> filesInFolder = peekFolder(node.file);
                List<File> unqueuedFiles = new ArrayList<>();

                for (File f : filesInFolder) {
                    if (batch.size() < limit) {
                        batch.add(f);
                        exploredPaths.add(f.getAbsolutePath());
                    } else {
                        unqueuedFiles.add(f);
                    }
                }

                // FIX: Re-queue remaining files that exceeded the batch size limit
                if (!unqueuedFiles.isEmpty()) {
                    for (File remainingFile : unqueuedFiles) {
                        if (!exploredPaths.contains(remainingFile.getAbsolutePath())) {
                            explorationQueue.add(new ExplorationNode(remainingFile, node.score * 0.95, node.depth));
                        }
                    }
                }

                // Add subdirectories to queue if not already explored or queued
                File[] subdirs = node.file.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File sd : subdirs) {
                        if (!isExcluded(sd.getName()) && !exploredPaths.contains(sd.getAbsolutePath())) {
                            explorationQueue.add(new ExplorationNode(sd, node.score * 0.9, node.depth + 1));
                            exploredPaths.add(sd.getAbsolutePath()); // Track queued directories to prevent duplicates
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
        if (file == null || !file.exists()) return 0;
        String name = file.getName();
        String lower = name.toLowerCase();

        // Manifests & Entry points across domains
        if (lower.equalsIgnoreCase("readme.md") || lower.equals("package.json") || lower.equals("pom.xml") || lower.equals("index.html") || lower.equals("manifest.json") || lower.equals("build.gradle") || lower.contains("architecture")) return 10;
        if (lower.contains("application") || lower.contains("main") || lower.equals("main.js") || lower.equals("index.js") || lower.equals("app.js") || lower.equals("server.js")) return 10;

        // Web & Software Components
        if (lower.contains("controller") || lower.contains("resource") || lower.contains("router") || lower.contains("component")) return 9;
        if (lower.contains("service") || lower.contains("manager") || lower.contains("handler") || lower.contains("api")) return 8;

        // Presentations & Documents
        if (lower.endsWith(".pptx") || lower.endsWith(".pdf") || lower.endsWith(".odp") || lower.endsWith(".docx") || lower.endsWith(".md")) return 8;

        // Audio & Music Albums
        if (lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.contains("track") || lower.contains("cover") || lower.equalsIgnoreCase("playlist.m3u")) return 8;

        // Datasets
        if (lower.endsWith(".evodata") || lower.endsWith(".parquet") || lower.endsWith(".jsonl") || lower.endsWith(".csv")) return 8;

        // Generic Code & Config
        if (lower.endsWith(".java") || lower.endsWith(".ts") || lower.endsWith(".py") || lower.endsWith(".cpp") || lower.endsWith(".c")) return 7;
        if (lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yml") || lower.endsWith(".yaml")) return 6;

        return 5;
    }

    /**
     * Step 2 & 3: Read Metadata and Update Knowledge Graph safely.
     */
    private void processBatch(List<File> batch) {
        context.log("[INTENT_RECONSTRUCTION] Processing batch of " + batch.size() + " files.");
        for (File file : batch) {
            if (file == null || !file.exists()) continue;

            // Safe handling for large or binary files (> 10MB)
            if (file.length() > 10 * 1024 * 1024) {
                context.log("[INTENT_RECONSTRUCTION] Skipping large file text scanning: " + file.getName() + " (" + file.length() + " bytes)");
                updateDesignModelForLargeFile(file);
                continue;
            }

            try {
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
            } catch (Exception e) {
                context.log("[INTENT_RECONSTRUCTION] Warning: Could not process file " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void updateDesignModelForLargeFile(File file) {
        ComponentRecord record = new ComponentRecord();
        record.setId(file.getAbsolutePath());
        record.setName(file.getName());
        record.setPath(file.getAbsolutePath());
        String lower = file.getName().toLowerCase();

        if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac") || lower.endsWith(".m4a") || lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mkv")) {
            record.setType("MEDIA");
        } else if (lower.endsWith(".evodata") || lower.endsWith(".parquet") || lower.endsWith(".zip") || lower.endsWith(".tar.gz")) {
            record.setType("DATASET");
        } else if (lower.endsWith(".pdf") || lower.endsWith(".pptx") || lower.endsWith(".docx")) {
            record.setType("DOCUMENTATION");
        } else {
            record.setType("LARGE_ARTIFACT");
        }

        record.setDescription("Large artifact (" + (file.length() / (1024 * 1024)) + " MB)");
        designModel.getComponents().add(record);
    }

    private void updateDesignModel(File file, EvoMetadata meta) {
        String compId = (meta != null && meta.getPath() != null) ? meta.getPath() : file.getPath();
        if (designModel.getComponents().stream().anyMatch(c -> compId.equalsIgnoreCase(c.getId()))) {
            return; // Prevent duplicate component entries across iterations
        }

        ComponentRecord record = new ComponentRecord();
        record.setId(compId);
        record.setName(file.getName());

        String name = file.getName();
        String lowerName = name.toLowerCase();

        eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter adapter = null;
        try {
            eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry registry =
                    eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry.createDefaultRegistry();
            eu.kalafatic.evolution.forge.data.api.source.DatasetItem item =
                    new eu.kalafatic.evolution.forge.data.api.source.DatasetItem(true, file.getAbsolutePath(), "FILE");
            adapter = registry.findAdapter(item);
        } catch (Throwable ignored) {}

        // Categorize component type based on structure, dynamic adapter capability, or role
        if (name.endsWith("Controller.java") || name.endsWith("Resource.java") ||
            name.endsWith("Service.java") || name.endsWith("UseCase.java") ||
            name.endsWith("Handler.java") || name.endsWith("Facade.java") ||
            name.endsWith("Application.java") || lowerName.contains("router") ||
            lowerName.contains("controller") || lowerName.contains("app.")) {
            record.setType("USE_CASE");
        } else if ((adapter != null && !(adapter instanceof eu.kalafatic.evolution.forge.data.impl.source.adapters.TextAdapter)
                && !(adapter instanceof eu.kalafatic.evolution.forge.data.impl.source.adapters.FolderAdapter))
                || "data".equalsIgnoreCase(meta.getRole())) {
            record.setType("DATASET");
        } else if ("documentation".equalsIgnoreCase(meta.getRole())) {
            record.setType("DOCUMENTATION");
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

    private void syncDesignModelWithReality() {
        Object rmObj = context.getOrchestrationState().getMetadata().get("targetRealityModel");
        if (!(rmObj instanceof eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel)) {
            return;
        }

        eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel rm =
                (eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) rmObj;

        // Subsystems
        if (rm.getSubsystems() != null) {
            for (eu.kalafatic.evolution.controller.mediation.model.Subsystem sub : rm.getSubsystems()) {
                String subId = "subsystem:" + sub.getName();
                if (designModel.getComponents().stream().noneMatch(c -> subId.equalsIgnoreCase(c.getId()))) {
                    ComponentRecord rec = new ComponentRecord();
                    rec.setId(subId);
                    rec.setName(sub.getName());
                    rec.setType("SUBSYSTEM");
                    rec.setDescription(sub.getPurpose() != null ? sub.getPurpose() : sub.getDescription());
                    designModel.getComponents().add(rec);
                }
            }
        }

        // Use Cases
        if (rm.getUseCases() != null) {
            for (eu.kalafatic.evolution.controller.mediation.model.ArchitecturalUseCase uc : rm.getUseCases()) {
                String ucId = "usecase:" + uc.getId();
                if (designModel.getComponents().stream().noneMatch(c -> ucId.equalsIgnoreCase(c.getId()))) {
                    ComponentRecord rec = new ComponentRecord();
                    rec.setId(ucId);
                    rec.setName(uc.getName());
                    rec.setType("USE_CASE");
                    rec.setDescription(uc.getDescription());
                    rec.getUseCases().add(uc.getName());
                    designModel.getComponents().add(rec);
                }
            }
        }

        // Authority / Entry Points
        if (rm.getArchitecturalAuthorityFiles() != null) {
            for (String file : rm.getArchitecturalAuthorityFiles()) {
                String fId = "authority:" + file;
                if (designModel.getComponents().stream().noneMatch(c -> fId.equalsIgnoreCase(c.getId()))) {
                    ComponentRecord rec = new ComponentRecord();
                    rec.setId(fId);
                    rec.setName(new File(file).getName());
                    rec.setType("ENTRY_POINT");
                    rec.setPath(file);
                    rec.setDescription("Architectural authority / entry point file");
                    designModel.getComponents().add(rec);
                }
            }
        }
    }

    private String buildTargetSummary() {
        syncDesignModelWithReality();

        File root = context.getProjectRoot();
        Object rmObj = context.getOrchestrationState().getMetadata().get("targetRealityModel");
        eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel rm =
                (rmObj instanceof eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel)
                        ? (eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) rmObj
                        : null;

        long useCaseCount = designModel.getComponents().stream().filter(c -> "USE_CASE".equals(c.getType())).count();
        long subsystemCount = designModel.getComponents().stream().filter(c -> "SUBSYSTEM".equals(c.getType()) || "DOMAIN".equals(c.getType())).count();
        long entryPointCount = designModel.getComponents().stream().filter(c -> "ENTRY_POINT".equals(c.getType())).count();
        long docCount = designModel.getComponents().stream().filter(c -> "DOCUMENTATION".equals(c.getType())).count();
        long dataCount = designModel.getComponents().stream().filter(c -> "DATASET".equals(c.getType()) || "DATA".equals(c.getType())).count();

        Object snapshotObj = context.getOrchestrationState().getMetadata().get("mediatedSnapshot");
        String classifications = null;
        if (snapshotObj instanceof eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot) {
            Object metaVal = ((eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot) snapshotObj).getMetadata().get("targetClassifications");
            if (metaVal instanceof String) {
                classifications = (String) metaVal;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Reconstructed Target Purpose & Knowledge:\n");
        sb.append("Target Path: ").append(root != null ? root.getAbsolutePath() : "Unknown").append("\n");

        if (classifications != null && !classifications.isEmpty()) {
            sb.append("Target Classifications: ").append(classifications).append("\n");
        }

        if (rm != null) {
            if (rm.getDomain() != null && !rm.getDomain().isEmpty()) {
                sb.append("Target Domain: ").append(rm.getDomain()).append("\n");
            }
            if (rm.getPurpose() != null && !rm.getPurpose().isEmpty()) {
                sb.append("Apparent Purpose: ").append(rm.getPurpose()).append("\n");
            }
        } else {
            sb.append("Target Nature: Heterogeneous Artifact Repository (Analyzed Components: ")
              .append(designModel.getComponents().size()).append(")\n");
        }

        if (rm != null) {
            if (rm.getSubsystems() != null && !rm.getSubsystems().isEmpty()) {
                sb.append("\nDiscovered Subsystems:\n");
                for (eu.kalafatic.evolution.controller.mediation.model.Subsystem sub : rm.getSubsystems()) {
                    sb.append(" - ").append(sub.getName()).append(" (Confidence: ").append(String.format(Locale.US, "%.2f", sub.getConfidence())).append(")\n");
                    if (sub.getPurpose() != null && !sub.getPurpose().isEmpty()) {
                        sb.append("   Purpose: ").append(sub.getPurpose()).append("\n");
                    }
                    if (sub.getCriticalFiles() != null && !sub.getCriticalFiles().isEmpty()) {
                        sb.append("   Critical Files: ").append(String.join(", ", sub.getCriticalFiles())).append("\n");
                    }
                }
            }

            if (rm.getUseCases() != null && !rm.getUseCases().isEmpty()) {
                sb.append("\nMain Use Cases:\n");
                for (eu.kalafatic.evolution.controller.mediation.model.ArchitecturalUseCase uc : rm.getUseCases()) {
                    sb.append(" - ").append(uc.getName()).append(": ").append(uc.getDescription()).append("\n");
                }
            }

            if (rm.getArchitecturalAuthorityFiles() != null && !rm.getArchitecturalAuthorityFiles().isEmpty()) {
                sb.append("\nEntry Points & Authority Files:\n");
                for (String auth : rm.getArchitecturalAuthorityFiles().stream().limit(10).collect(Collectors.toList())) {
                    sb.append(" - ").append(auth).append("\n");
                }
            }

            if (rm.getExecutionFlows() != null && !rm.getExecutionFlows().isEmpty()) {
                sb.append("\nImportant Workflows:\n");
                for (String flow : rm.getExecutionFlows().stream().limit(10).collect(Collectors.toList())) {
                    sb.append(" - ").append(flow).append("\n");
                }
            }

            if (rm.getContentLandscapeSummary() != null && !rm.getContentLandscapeSummary().isEmpty()) {
                sb.append("\nArchitecture Overview:\n").append(rm.getContentLandscapeSummary()).append("\n");
            }

            if (rm.getKnowledgeGaps() != null && !rm.getKnowledgeGaps().isEmpty()) {
                sb.append("\nRemaining Unknowns & Knowledge Gaps:\n");
                for (eu.kalafatic.evolution.controller.mediation.model.KnowledgeGap gap : rm.getKnowledgeGaps().stream().limit(10).collect(Collectors.toList())) {
                    sb.append(" - [").append(gap.getType()).append("] ").append(gap.getDescription()).append("\n");
                }
            }
        }

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

        if (rm == null || rm.getUseCases().isEmpty()) {
            if (!topComponents.isEmpty()) {
                sb.append("\nDiscovered Core Artifacts:\n");
                for (String comp : topComponents) {
                    sb.append(" - ").append(comp).append("\n");
                }
            }

            if (!discoveredUseCases.isEmpty()) {
                sb.append("\nDiscovered Capability Actions:\n");
                for (String uc : discoveredUseCases) {
                    sb.append(" - ").append(uc).append("\n");
                }
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
