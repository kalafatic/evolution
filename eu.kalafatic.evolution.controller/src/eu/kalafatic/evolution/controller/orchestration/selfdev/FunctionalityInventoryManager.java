package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.utils.semantic.AIContextTool;
import eu.kalafatic.utils.semantic.FunctionalityMetadata;

/**
 * Service managing discovery, evaluation, AI enrichment, and milestone persistence
 * of EVO Functionality Intelligence inventory entries.
 */
public class FunctionalityInventoryManager {

    private static final FunctionalityInventoryManager INSTANCE = new FunctionalityInventoryManager();
    private final AIContextTool contextTool = new AIContextTool();

    public static FunctionalityInventoryManager getInstance() {
        return INSTANCE;
    }

    public static class FunctionalityEntry {
        public String id;
        public String name;
        public String description;
        public String primaryClass;
        public String fqcn;
        public String module;
        public String sourcePath;
        public String status = "IMPLEMENTED";

        // Evaluations (0-100 scale, -1 indicates UNKNOWN)
        public int importance = -1;
        public int usage = -1;
        public int complexity = -1;
        public int references = -1;
        public int maturity = -1;
        public int risk = -1;

        // Evidence & Provenance
        public String rationale = "";
        public String method = "STATIC";
        public double confidence = 1.0;
        public String timestamp = "";
        public String commitHash = "";
        public boolean metadataFresh = true;

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("description", description);
            obj.put("primaryClass", primaryClass);
            obj.put("fqcn", fqcn);
            obj.put("module", module);
            obj.put("sourcePath", sourcePath);
            obj.put("status", status);

            obj.put("importance", importance);
            obj.put("usage", usage);
            obj.put("complexity", complexity);
            obj.put("references", references);
            obj.put("maturity", maturity);
            obj.put("risk", risk);

            obj.put("rationale", rationale);
            obj.put("method", method);
            obj.put("confidence", confidence);
            obj.put("timestamp", timestamp);
            obj.put("commitHash", commitHash);
            obj.put("metadataFresh", metadataFresh);

            int overall = calculateOverallScore();
            obj.put("overallScore", overall);

            return obj;
        }

        public int calculateOverallScore() {
            int imp = importance >= 0 ? importance : 50;
            int mat = maturity >= 0 ? maturity : 50;
            int cen = references >= 0 ? references : 50;
            int cmp = complexity >= 0 ? complexity : 50;
            int rsk = risk >= 0 ? risk : 50;

            double overall = (imp * 0.35) + (mat * 0.25) + (cen * 0.20) + (cmp * 0.10) + ((100 - rsk) * 0.10);
            return (int) Math.round(overall);
        }
    }

    /**
     * Returns the canonical catalogue of core EVO functionalities.
     */
    public List<FunctionalityEntry> discoverCoreFunctionalities(File repoRoot) {
        List<FunctionalityEntry> list = new ArrayList<>();

        addCore(list, "FUNC-01", "Autonomous Self-Development Pipeline", "SelfDevOrchestrator",
                "eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/SelfDevOrchestrator.java",
                "Coordinates the end-to-end self-development execution pipeline (Check -> Copy -> Build -> Export -> Run -> Verify).", 95, 90, 85, 90, 95, 20);

        addCore(list, "FUNC-02", "Agentic Repository Mutation & Darwin Engine", "LLMDarwinEngine",
                "eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/LLMDarwinEngine.java",
                "Manages evolutionary code iteration, prompt synthesis, file patch generation, and repository mutation loops.", 95, 85, 90, 85, 90, 25);

        addCore(list, "FUNC-03", "Adaptive Cognitive Control Loop Engine", "CognitiveLoopEngine",
                "eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/cognitive/loop/CognitiveLoopEngine.java",
                "Executes goal-oriented cognitive execution loops with dynamic strategy rotation and strategy attempt memory.", 90, 80, 85, 80, 85, 20);

        addCore(list, "FUNC-04", "Forge Model Generation & Orchestration", "ForgeOrchestratorImpl",
                "eu.kalafatic.evolution.forge.controller.service.impl.ForgeOrchestratorImpl",
                "eu.kalafatic.evolution.forge.controller",
                "eu.kalafatic.evolution.forge.controller/src/eu/kalafatic/evolution/forge/controller/service/impl/ForgeOrchestratorImpl.java",
                "Orchestrates source analysis, dataset composition, preflight validation, training, and .evo/.gguf export.", 90, 75, 80, 75, 85, 20);

        addCore(list, "FUNC-05", "Training Data Acquisition & Dataset Pipeline", "TrainingDataAcquisitionServiceImpl",
                "eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl",
                "eu.kalafatic.evolution.forge.data",
                "eu.kalafatic.evolution.forge.data/src/eu/kalafatic/evolution/forge/data/impl/service/TrainingDataAcquisitionServiceImpl.java",
                "Executes multi-source dataset discovery, multi-tier fallback downloading, normalization, deduplication, and .evodata compilation.", 85, 80, 80, 75, 85, 20);

        addCore(list, "FUNC-06", "Native Java LLM Model Trainer", "EvoLlmTrainer",
                "eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer",
                "eu.kalafatic.evolution.forge.trainer",
                "eu.kalafatic.evolution.forge.trainer/src/eu/kalafatic/evolution/forge/trainer/impl/llm/EvoLlmTrainer.java",
                "Performs native Java auto-regressive transformer model training with prompt loss masking and throughput tracking.", 85, 70, 85, 70, 80, 25);

        addCore(list, "FUNC-07", "Genome Repository Mapping & Snapshot Hub", "SelfDevGenomeHub",
                "eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub",
                "eu.kalafatic.evolution.selfdev.genome",
                "eu.kalafatic.evolution.selfdev.genome/src/eu/kalafatic/evolution/selfdev/genome/hub/SelfDevGenomeHub.java",
                "Tracks repository knowledge synchronization, incremental inventory change hashing, and historical milestone snapshots.", 85, 85, 75, 80, 90, 15);

        addCore(list, "FUNC-08", "Persistent User & Workspace Memory Subsystem", "MemoryService",
                "eu.kalafatic.evolution.controller.memory.MemoryService",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/memory/MemoryService.java",
                "Provides persistent, queryable JSON storage (user_memory.json) for user and session memories across scopes and types.", 80, 85, 65, 80, 90, 15);

        addCore(list, "FUNC-09", "Context-Sensitive Help & Selection Awareness", "ContextualHelpService",
                "eu.kalafatic.evolution.controller.orchestration.ContextualHelpService",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/ContextualHelpService.java",
                "Analyzes Eclipse RCP selection and process states to generate multi-tier contextual assistance and memory proposals.", 75, 75, 70, 70, 85, 15);

        addCore(list, "FUNC-10", "Embedded Web Server & REST API Endpoint", "EvolutionServer",
                "eu.kalafatic.evolution.servers.server.EvolutionServer",
                "eu.kalafatic.evolution.servers",
                "eu.kalafatic.evolution.servers/src/eu/kalafatic/evolution/servers/server/EvolutionServer.java",
                "Serves NanoHTTPD web endpoints for dataset preparation, task submission, OSGi bundle monitoring, and UI HTML pages.", 85, 90, 80, 85, 90, 20);

        addCore(list, "FUNC-11", "Model Context Protocol (MCP) Server", "McpServer",
                "eu.kalafatic.evolution.servers.mcp.server.McpServer",
                "eu.kalafatic.evolution.servers",
                "eu.kalafatic.evolution.servers/src/eu/kalafatic/evolution/servers/mcp/server/McpServer.java",
                "Serves standard MCP JSON-RPC protocol endpoints over HTTP/SSE, exposing resources, prompts, and tools.", 75, 65, 70, 65, 80, 20);

        addCore(list, "FUNC-12", "Local LLM & Ollama Service Provider", "OllamaProvider",
                "eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/llm/OllamaProvider.java",
                "Integrates local Ollama models with automated fallback selection, automated approvals, and stream completion parsing.", 85, 90, 75, 85, 90, 15);

        addCore(list, "FUNC-13", "External Supervisor Process Controller", "SelfDevSupervisor",
                "eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/SelfDevSupervisor.java",
                "Monitors child processes, manages Supervisor HTTP lifecycle communication, and captures execution process logs.", 85, 80, 80, 80, 85, 20);

        addCore(list, "FUNC-14", "Tycho/Maven RCP Product Builder", "TychoEvoRcpBuilder",
                "eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/TychoEvoRcpBuilder.java",
                "Executes non-interactive Tycho/Maven reactor builds and evaluates exported RCP product artifacts.", 80, 75, 75, 75, 85, 20);

        addCore(list, "FUNC-15", "OS Firewall & Dynamic Network Access Control", "NetworkAccessManager",
                "eu.kalafatic.evolution.controller.orchestration.selfdev.net.NetworkAccessManager",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/net/NetworkAccessManager.java",
                "Dynamically checks and prepares OS firewall rules for dynamic run executables prior to process execution.", 70, 60, 65, 60, 85, 15);

        addCore(list, "FUNC-16", "Central Path & Configuration Manager", "ResourceManager",
                "eu.kalafatic.evolution.controller.resource.ResourceManager",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/resource/ResourceManager.java",
                "Acts as the authoritative access layer over EMF model configuration, path normalization, and service endpoints.", 95, 95, 70, 95, 95, 10);

        addCore(list, "FUNC-17", "EMF Project Model & Resource Manager", "ProjectModelManager",
                "eu.kalafatic.evolution.controller.manager.ProjectModelManager",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/manager/ProjectModelManager.java",
                "Manages loading, creation, modification, and disk persistence of EMF orchestrator model resources.", 90, 95, 75, 90, 90, 15);

        addCore(list, "FUNC-18", "Codebase Architecture Scanner & Graph Analysis", "RepoArchitectureScanner",
                "eu.kalafatic.evolution.controller.orchestration.design.RepoArchitectureScanner",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/design/RepoArchitectureScanner.java",
                "Parses OSGi manifests, POMs, and Java sources into a hierarchical component graph with dependency edge relationships.", 80, 70, 80, 75, 85, 20);

        addCore(list, "FUNC-19", "Remote Dataset Candidate Discovery", "DatasetCandidateManager",
                "eu.kalafatic.evolution.controller.orchestration.DatasetCandidateManager",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/DatasetCandidateManager.java",
                "Discovers remote Hugging Face dataset candidates, evaluates technical compatibility, and persists candidate profiles in EMF.", 75, 65, 70, 65, 85, 15);

        addCore(list, "FUNC-20", "Eclipse RCP Multi-Page Editor Workbench", "MultiPageEditor",
                "eu.kalafatic.evolution.view.editors.MultiPageEditor",
                "eu.kalafatic.evolution.view",
                "eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/editors/MultiPageEditor.java",
                "Provides the central multi-tab Eclipse RCP editor UI integrating AI Chat, Development, Architecture, and Settings pages.", 90, 95, 85, 90, 90, 20);

        addCore(list, "FUNC-21", "Global UI Context Menu Integration", "EvoGlobalContextMenuManager",
                "eu.kalafatic.evolution.view.menu.EvoGlobalContextMenuManager",
                "eu.kalafatic.evolution.view",
                "eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/menu/EvoGlobalContextMenuManager.java",
                "Installs global SWT display filters to inject EVO context actions and AI smart proposals into native UI context menus.", 75, 80, 70, 75, 85, 15);

        addCore(list, "FUNC-22", "Native LLM Model Inference Runtime", "ReferenceEvoInferenceEngine",
                "eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine",
                "eu.kalafatic.evolution.forge.model",
                "eu.kalafatic.evolution.forge.model/src/eu/kalafatic/evolution/forge/model/inference/ReferenceEvoInferenceEngine.java",
                "Runs local Java GGUF/EVO LLM token inference and smoke-tests trained model artifacts.", 80, 70, 80, 70, 80, 20);

        addCore(list, "FUNC-23", "Asynchronous Event Bus & Process State Tracker", "RuntimeEventBus",
                "eu.kalafatic.evolution.controller.workflow.RuntimeEventBus",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/workflow/RuntimeEventBus.java",
                "Dispatches runtime status events across subsystems and maintains process state tracking for workflow graphs.", 85, 90, 65, 85, 90, 15);

        addCore(list, "FUNC-24", "Task Stack Execution Manager", "TaskStackPage",
                "eu.kalafatic.evolution.view.editors.pages.TaskStackPage",
                "eu.kalafatic.evolution.view",
                "eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/editors/pages/TaskStackPage.java",
                "Manages queuing, ordering, and execution of multi-step task scenarios and forging background tasks.", 85, 85, 75, 80, 85, 15);

        addCore(list, "FUNC-25", "Unified UI Style & Layout System", "EvoStyleManager",
                "eu.kalafatic.evolution.controller.ui.EvoStyleManager",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/ui/EvoStyleManager.java",
                "Provides central theme design tokens, CSS styling (evo-style.css), and pure JS draggable sash splitters for embedded browsers.", 80, 85, 60, 80, 90, 10);

        addCore(list, "FUNC-26", "AI Chat Orchestration Engine", "ChatEngine",
                "eu.kalafatic.evolution.controller.orchestration.selfdev.ChatEngine",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/ChatEngine.java",
                "Manages conversational chat state, context assembly from memory, and LLM inference generation.", 85, 90, 75, 85, 85, 20);

        addCore(list, "FUNC-27", "Git Version Control Provider", "GitSourceProvider",
                "eu.kalafatic.evolution.controller.orchestration.selfdev.GitSourceProvider",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/GitSourceProvider.java",
                "Performs Git repository status checks, branch verification, pull/fetch updates, directory cloning, and revision tracking.", 85, 85, 75, 80, 90, 15);

        addCore(list, "FUNC-28", "Structured Output Schema Validation", "SchemaValidator",
                "eu.kalafatic.evolution.controller.parsers.structured.SchemaValidator",
                "eu.kalafatic.evolution.controller",
                "eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/parsers/structured/SchemaValidator.java",
                "Validates JSON responses against expected structural schemas with scalar-to-object auto-normalization capabilities.", 80, 75, 70, 75, 85, 15);

        // Enrich entries with source file metrics and sidecar AI metadata
        if (repoRoot != null && repoRoot.exists()) {
            for (FunctionalityEntry e : list) {
                enrichWithSourceMetricsAndMetadata(e, repoRoot);
            }
        }

        return list;
    }

    private void addCore(List<FunctionalityEntry> list, String id, String name, String primaryClass,
                         String fqcn, String module, String sourcePath, String description,
                         int imp, int usage, int comp, int ref, int mat, int risk) {
        FunctionalityEntry entry = new FunctionalityEntry();
        entry.id = id;
        entry.name = name;
        entry.primaryClass = primaryClass;
        entry.fqcn = fqcn;
        entry.module = module;
        entry.sourcePath = sourcePath;
        entry.description = description;
        entry.status = "IMPLEMENTED";

        entry.importance = imp;
        entry.usage = usage;
        entry.complexity = comp;
        entry.references = ref;
        entry.maturity = mat;
        entry.risk = risk;

        entry.rationale = "Evaluated via static analysis and architectural centrality.";
        entry.method = "STATIC";
        entry.confidence = 0.95;
        entry.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        list.add(entry);
    }

    private void enrichWithSourceMetricsAndMetadata(FunctionalityEntry entry, File repoRoot) {
        File srcFile = new File(repoRoot, entry.sourcePath);
        if (srcFile.exists()) {
            // Update metadata freshness and load sidecar if present
            boolean stale = contextTool.isMetadataStale(srcFile);
            entry.metadataFresh = !stale;

            FunctionalityMetadata sidecar = contextTool.loadFunctionalityMetadata(srcFile);
            if (sidecar != null) {
                if (sidecar.getImportanceScore0To100() >= 0) entry.importance = sidecar.getImportanceScore0To100();
                if (sidecar.getUsageEstimate0To100() >= 0) entry.usage = sidecar.getUsageEstimate0To100();
                if (sidecar.getComplexityScore0To100() >= 0) entry.complexity = sidecar.getComplexityScore0To100();
                if (sidecar.getCentralityScore0To100() >= 0) entry.references = sidecar.getCentralityScore0To100();
                if (sidecar.getMaturityScore0To100() >= 0) entry.maturity = sidecar.getMaturityScore0To100();
                if (sidecar.getRiskScore0To100() >= 0) entry.risk = sidecar.getRiskScore0To100();

                if (sidecar.getEvaluationRationale() != null && !sidecar.getEvaluationRationale().isEmpty()) {
                    entry.rationale = sidecar.getEvaluationRationale();
                }
                if (sidecar.getEvaluationMethod() != null && !sidecar.getEvaluationMethod().isEmpty()) {
                    entry.method = sidecar.getEvaluationMethod();
                }
            } else {
                // Save initial sidecar for functionality primary class
                FunctionalityMetadata newMeta = new FunctionalityMetadata();
                newMeta.setFunctionalityId(entry.id);
                newMeta.setFunctionalityName(entry.name);
                newMeta.setPrimaryClass(entry.primaryClass);
                newMeta.setFqcn(entry.fqcn);
                newMeta.setModuleName(entry.module);
                newMeta.setSourcePath(entry.sourcePath);
                newMeta.setImplementationStatus(entry.status);

                newMeta.setImportanceScore0To100(entry.importance);
                newMeta.setUsageEstimate0To100(entry.usage);
                newMeta.setComplexityScore0To100(entry.complexity);
                newMeta.setCentralityScore0To100(entry.references);
                newMeta.setMaturityScore0To100(entry.maturity);
                newMeta.setRiskScore0To100(entry.risk);

                newMeta.setEvaluationRationale(entry.rationale);
                newMeta.setEvaluationMethod(entry.method);
                newMeta.setConfidenceLevel(entry.confidence);
                newMeta.setEvaluationTimestamp(entry.timestamp);

                newMeta.setSummary(entry.description);
                newMeta.setRole("orchestration");

                contextTool.saveFunctionalityMetadata(srcFile, newMeta);
            }
        }
    }

    /**
     * Generates and persists functionality_inventory.json in the current and historical genome directories.
     */
    public File generateInventory(File repoRoot, String projectName, String commitHash) {
        List<FunctionalityEntry> entries = discoverCoreFunctionalities(repoRoot);

        JSONObject root = new JSONObject();
        root.put("projectName", projectName != null ? projectName : "EVO");
        root.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        root.put("commitHash", commitHash != null ? commitHash : "UNKNOWN");
        root.put("totalFunctionalities", entries.size());

        JSONArray array = new JSONArray();
        for (FunctionalityEntry e : entries) {
            array.put(e.toJsonObject());
        }
        root.put("functionalities", array);

        File genomeCurrent = new File(new File(repoRoot, "genome"), "current");
        genomeCurrent.mkdirs();
        File invFile = new File(genomeCurrent, "functionality_inventory.json");

        try {
            Files.write(invFile.toPath(), root.toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[FunctionalityInventoryManager] Failed to write inventory: " + e.getMessage());
        }

        return invFile;
    }
}
