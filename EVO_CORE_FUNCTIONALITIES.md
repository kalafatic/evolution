# EVO — Core Functionality Discovery and Primary Java Class Mapping

## 1. Executive Summary

This document presents an evidence-based discovery and technical mapping of the core implemented capabilities of the **EVO (Evolution)** platform. EVO is an OSGi-based native AI development environment built on top of the Eclipse Rich Client Platform (RCP) and Tycho/Maven infrastructure. It integrates local and remote LLM inference, autonomous dataset compilation, native LLM training, agentic repository mutation (Darwin), self-guided workspace self-development (Self-Dev), and structural genome mapping.

All mappings in this catalogue have been verified by inspecting the production Java source code, module manifests (`MANIFEST.MF`), EMF model definitions (`evolution.ecore`), and active workflow execution paths.

---

## 2. Core Functionality Catalogue

The following table lists the 28 core functionalities of the EVO platform, ordered from the primary architectural execution capabilities to secondary supporting capabilities.

| # | Functionality | Primary Java Class | Module / Package | Short Description | Status |
| :- | :------------ | :----------------- | :--------------- | :---------------- | :----- |
| 1 | Autonomous Self-Development Pipeline | `SelfDevOrchestrator` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.selfdev` | Coordinates the end-to-end self-development execution pipeline (Check → Copy → Build → Export → Run → Verify). | IMPLEMENTED |
| 2 | Agentic Repository Mutation & Darwin Engine | `LLMDarwinEngine` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.selfdev` | Manages evolutionary code iteration, prompt synthesis, file patch generation, and repository mutation loops. | IMPLEMENTED |
| 3 | Adaptive Cognitive Control Loop Engine | `CognitiveLoopEngine` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.cognitive.loop` | Executes goal-oriented cognitive execution loops with dynamic strategy rotation and strategy attempt memory. | IMPLEMENTED |
| 4 | Forge Model Generation & Orchestration | `ForgeOrchestratorImpl` | `eu.kalafatic.evolution.forge.controller`<br>`eu.kalafatic.evolution.forge.controller.service.impl` | Orchestrates source analysis, dataset composition, preflight validation, training, and `.evo`/`.gguf` export. | IMPLEMENTED |
| 5 | Training Data Acquisition & Dataset Pipeline | `TrainingDataAcquisitionServiceImpl` | `eu.kalafatic.evolution.forge.data`<br>`eu.kalafatic.evolution.forge.data.impl.service` | Executes multi-source dataset discovery, multi-tier fallback downloading, normalization, deduplication, and `.evodata` compilation. | IMPLEMENTED |
| 6 | Native Java LLM Model Trainer | `EvoLlmTrainer` | `eu.kalafatic.evolution.forge.trainer`<br>`eu.kalafatic.evolution.forge.trainer.impl.llm` | Performs native Java auto-regressive transformer model training with prompt loss masking and throughput tracking. | IMPLEMENTED |
| 7 | Genome Repository Mapping & Snapshot Hub | `SelfDevGenomeHub` | `eu.kalafatic.evolution.selfdev.genome`<br>`eu.kalafatic.evolution.selfdev.genome.hub` | Tracks repository knowledge synchronization, incremental inventory change hashing, and historical milestone snapshots. | IMPLEMENTED |
| 8 | Persistent User & Workspace Memory Subsystem | `MemoryService` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.memory` | Provides persistent, queryable JSON storage (`user_memory.json`) for user and session memories across scopes and types. | IMPLEMENTED |
| 9 | Context-Sensitive Help & Selection Awareness | `ContextualHelpService` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration` | Analyzes Eclipse RCP selection and process states to generate multi-tier contextual assistance and memory proposals. | IMPLEMENTED |
| 10 | Embedded Web Server & REST API Endpoint | `EvolutionServer` | `eu.kalafatic.evolution.servers`<br>`eu.kalafatic.evolution.servers.server` | Serves NanoHTTPD web endpoints for dataset preparation, task submission, OSGi bundle monitoring, and UI HTML pages. | IMPLEMENTED |
| 11 | Model Context Protocol (MCP) Server | `McpServer` | `eu.kalafatic.evolution.servers`<br>`eu.kalafatic.evolution.servers.mcp.server` | Serves standard MCP JSON-RPC protocol endpoints over HTTP/SSE, exposing resources, prompts, and tools. | IMPLEMENTED |
| 12 | Local LLM & Ollama Service Provider | `OllamaProvider` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.llm` | Integrates local Ollama models with automated fallback selection, automated approvals, and stream completion parsing. | IMPLEMENTED |
| 13 | External Supervisor Process Controller | `SelfDevSupervisor` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.selfdev` | Monitors child processes, manages Supervisor HTTP lifecycle communication, and captures execution process logs. | IMPLEMENTED |
| 14 | Tycho/Maven RCP Product Builder | `TychoEvoRcpBuilder` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.selfdev` | Executes non-interactive Tycho/Maven reactor builds and evaluates exported RCP product artifacts (`evolution-*.zip`). | IMPLEMENTED |
| 15 | OS Firewall & Dynamic Network Access Control | `NetworkAccessManager` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.selfdev.net` | Dynamically checks and prepares OS firewall rules for dynamic run executables prior to process execution. | IMPLEMENTED |
| 16 | Central Path & Configuration Manager | `ResourceManager` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.resource` | Acts as the authoritative access layer over EMF model configuration, path normalization, and service endpoints. | IMPLEMENTED |
| 17 | EMF Project Model & Resource Manager | `ProjectModelManager` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.manager` | Manages loading, creation, modification, and disk persistence of EMF orchestrator model resources (`evo_config.xml`). | IMPLEMENTED |
| 18 | Codebase Architecture Scanner & Graph Analysis | `RepoArchitectureScanner` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.design` | Parses OSGi manifests, POMs, and Java sources into a hierarchical component graph with dependency edge relationships. | IMPLEMENTED |
| 19 | Remote Dataset Candidate Discovery | `DatasetCandidateManager` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration` | Discovers remote Hugging Face dataset candidates, evaluates technical compatibility, and persists candidate profiles in EMF. | IMPLEMENTED |
| 20 | Eclipse RCP Multi-Page Editor Workbench | `MultiPageEditor` | `eu.kalafatic.evolution.view`<br>`eu.kalafatic.evolution.view.editors` | Provides the central multi-tab Eclipse RCP editor UI integrating AI Chat, Development, Architecture, and Settings pages. | IMPLEMENTED |
| 21 | Global UI Context Menu Integration | `EvoGlobalContextMenuManager` | `eu.kalafatic.evolution.view`<br>`eu.kalafatic.evolution.view.menu` | Installs global SWT display filters to inject EVO context actions and AI smart proposals into native UI context menus. | IMPLEMENTED |
| 22 | Native LLM Model Inference Runtime | `ReferenceEvoInferenceEngine` | `eu.kalafatic.evolution.forge.model`<br>`eu.kalafatic.evolution.forge.model.inference` | Runs local Java GGUF/EVO LLM token inference and smoke-tests trained model artifacts. | IMPLEMENTED |
| 23 | Asynchronous Event Bus & Process State Tracker | `RuntimeEventBus` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.workflow` | Dispatches runtime status events across subsystems and maintains process state tracking for workflow graphs. | IMPLEMENTED |
| 24 | Task Stack Execution Manager | `TaskStackPage` | `eu.kalafatic.evolution.view`<br>`eu.kalafatic.evolution.view.editors.pages` | Manages queuing, ordering, and execution of multi-step task scenarios and forging background tasks. | IMPLEMENTED |
| 25 | Unified UI Style & Layout System | `EvoStyleManager` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.ui` | Provides central theme design tokens, CSS styling (`evo-style.css`), and pure JS draggable sash splitters for embedded browsers. | IMPLEMENTED |
| 26 | AI Chat Orchestration Engine | `ChatEngine` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.selfdev` | Manages AI conversation state, processes user prompts, appends memory/context, and interacts with LLM providers. | IMPLEMENTED |
| 27 | Git Version Control Provider | `GitSourceProvider` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.orchestration.selfdev` | Performs Git repository status checks, branch verification, pull/fetch updates, directory cloning, and revision tracking. | IMPLEMENTED |
| 28 | Structured Output Schema Validation | `SchemaValidator` | `eu.kalafatic.evolution.controller`<br>`eu.kalafatic.evolution.controller.parsers.structured` | Validates JSON responses against expected structural schemas with scalar-to-object auto-normalization capabilities. | IMPLEMENTED |

---

## 3. Primary Java Class Mapping & Source Evidence

The following section provides source location details, key methods, and architectural responsibilities that justify each mapping in the catalogue.

### 1. Autonomous Self-Development Pipeline
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/SelfDevOrchestrator.java`
* **Key Responsibility / Method:** `runTasks(List<ISelfDevTask> tasks, SelfDevContext context)` — Executes ordered self-development tasks, enforces lifecycle invariants across Git check, reactor copy, build, export, startup, and verification stages.

### 2. Agentic Repository Mutation & Darwin Engine
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.selfdev.LLMDarwinEngine`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/LLMDarwinEngine.java`
* **Key Responsibility / Method:** `mutate(...)`, `solveIteratively(...)` — Drives autonomous repository adaptation, prompt synthesis, file patch application, and multi-pass cognitive iterations.

### 3. Adaptive Cognitive Control Loop Engine
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.cognitive.loop.CognitiveLoopEngine`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/cognitive/loop/CognitiveLoopEngine.java`
* **Key Responsibility / Method:** `solve(SessionContainer session, CognitiveGoal goal, ...)` — Evaluates goals against world state, dynamically selects untried capabilities/strategies, and manages strategy attempt memory.

### 4. Forge Model Generation & Orchestration
* **Primary Java Class:** `eu.kalafatic.evolution.forge.controller.service.impl.ForgeOrchestratorImpl`
* **Source Location:** `eu.kalafatic.evolution.forge.controller/src/eu/kalafatic/evolution/forge/controller/service/impl/ForgeOrchestratorImpl.java`
* **Key Responsibility / Method:** `executeJob(ForgeJob job)` — Coordinates input source analysis, dataset composition, hyperparameter preflight validation, native training, model validation, and artifact export.

### 5. Training Data Acquisition & Dataset Pipeline
* **Primary Java Class:** `eu.kalafatic.evolution.forge.data.impl.service.TrainingDataAcquisitionServiceImpl`
* **Source Location:** `eu.kalafatic.evolution.forge.data/src/eu/kalafatic/evolution/forge/data/impl/service/TrainingDataAcquisitionServiceImpl.java`
* **Key Responsibility / Method:** `acquireDataset(TrainingDataAcquisitionRequest request)` — Executes multi-tier source discovery (Rows API → Parquet API → Hub Tree API), bounded retries, normalization, deduplication, and `.evodata` output compilation.

### 6. Native Java LLM Model Trainer
* **Primary Java Class:** `eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer`
* **Source Location:** `eu.kalafatic.evolution.forge.trainer/src/eu/kalafatic/evolution/forge/trainer/impl/llm/EvoLlmTrainer.java`
* **Key Responsibility / Method:** `train(TrainingSample[] samples, TrainingConfig config, ProgressListener listener)` — Performs native auto-regressive transformer optimization with prompt loss masking and throughput metrics.

### 7. Genome Repository Mapping & Snapshot Hub
* **Primary Java Class:** `eu.kalafatic.evolution.selfdev.genome.hub.SelfDevGenomeHub`
* **Source Location:** `eu.kalafatic.evolution.selfdev.genome/src/eu/kalafatic/evolution/selfdev/genome/hub/SelfDevGenomeHub.java`
* **Key Responsibility / Method:** `updateGenome(...)`, `synchronizeGenomeState(...)` — Integrates SHA-256 inventory scanning, architectural changes, milestone markdown generation (`milestone_v1.md`), and historical snapshot persistence.

### 8. Persistent User & Workspace Memory Subsystem
* **Primary Java Class:** `eu.kalafatic.evolution.controller.memory.MemoryService`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/memory/MemoryService.java`
* **Key Responsibility / Method:** `addEntry(...)`, `queryMemories(...)`, `getInstance()` — Provides thread-safe CRUD and persistent workspace JSON backing (`user_memory.json`) for memories categorized by scope, type, and source.

### 9. Context-Sensitive Help & Selection Awareness
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.ContextualHelpService`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/ContextualHelpService.java`
* **Key Responsibility / Method:** `resolveHelp(IWorkbenchPart part, ISelection selection)` — Generates 3-level contextual assistance (Selection, Process Awareness, Cognitive Failure Remedies) driven by RCP selection events and active process state.

### 10. Embedded Web Server & REST API Endpoint
* **Primary Java Class:** `eu.kalafatic.evolution.servers.server.EvolutionServer`
* **Source Location:** `eu.kalafatic.evolution.servers/src/eu/kalafatic/evolution/servers/server/EvolutionServer.java`
* **Key Responsibility / Method:** `serve(IHTTPSession session)` — NanoHTTPD REST server handling `/forge/dataset/prepare`, `/task`, `/server/osgi`, `/server/project/create`, and UI HTML rendering.

### 11. Model Context Protocol (MCP) Server
* **Primary Java Class:** `eu.kalafatic.evolution.servers.mcp.server.McpServer`
* **Source Location:** `eu.kalafatic.evolution.servers/src/eu/kalafatic/evolution/servers/mcp/server/McpServer.java`
* **Key Responsibility / Method:** `start()`, `handleRpcRequest(...)` — Implements the Model Context Protocol JSON-RPC standard over HTTP/SSE, exposing resources, tools, and prompts.

### 12. Local LLM & Ollama Service Provider
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/llm/OllamaProvider.java`
* **Key Responsibility / Method:** `chat(...)`, `findWorkingFallbackModel()` — Manages local Ollama HTTP communications, automatic model fallback selection in non-interactive modes, and timeout exception classification.

### 13. External Supervisor Process Controller
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/SelfDevSupervisor.java`
* **Key Responsibility / Method:** `startSupervisor()`, `runTask(...)` — Manages external supervisor process execution, port offsets in DEBUG mode (+10 offset), HTTP REST triggers, and log file generation.

### 14. Tycho/Maven RCP Product Builder
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/TychoEvoRcpBuilder.java`
* **Key Responsibility / Method:** `build(...)`, `findExactExportedProduct(...)` — Executes non-interactive batch Maven/Tycho builds, verifies product naming invariants (`evolution-*.zip`), and extracts OSGi bundle manifests.

### 15. OS Firewall & Dynamic Network Access Control
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.selfdev.net.NetworkAccessManager`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/net/NetworkAccessManager.java`
* **Key Responsibility / Method:** `prepareAccess(Path executable, List<Integer> ports)` — Inspects and configures OS firewall rules (e.g. via PowerShell on Windows) prior to launching dynamically built run executables.

### 16. Central Path & Configuration Manager
* **Primary Java Class:** `eu.kalafatic.evolution.controller.resource.ResourceManager`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/resource/ResourceManager.java`
* **Key Responsibility / Method:** `getEvoGitRepository()`, `findService(...)` — Single authoritative configuration facade resolving workspace paths, OSGi service definitions, and network port settings without filesystem guessing.

### 17. EMF Project Model & Resource Manager
* **Primary Java Class:** `eu.kalafatic.evolution.controller.manager.ProjectModelManager`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/manager/ProjectModelManager.java`
* **Key Responsibility / Method:** `loadResource(...)`, `saveResource(...)` — Handles EMF model lifecycle, orchestrator configuration loading (`evo_config.xml`), and safe persistence without triggering Eclipse undo manager assertion failures.

### 18. Codebase Architecture Scanner & Graph Analysis
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.design.RepoArchitectureScanner`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/design/RepoArchitectureScanner.java`
* **Key Responsibility / Method:** `scanRepository(File repoRoot)` — Recursively parses manifests, POMs, and Java files into a structural model of packages, classes, interfaces, and cross-bundle dependency edges.

### 19. Remote Dataset Candidate Discovery
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.DatasetCandidateManager`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/DatasetCandidateManager.java`
* **Key Responsibility / Method:** `discoverCandidates(DatasetSearchRequest request)` — Queries Hugging Face catalog endpoints, scores technical compatibility deterministically (0–100), and updates EMF model bindings.

### 20. Eclipse RCP Multi-Page Editor Workbench
* **Primary Java Class:** `eu.kalafatic.evolution.view.editors.MultiPageEditor`
* **Source Location:** `eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/editors/MultiPageEditor.java`
* **Key Responsibility / Method:** `addPages()`, `doSave(IProgressMonitor monitor)` — Central Eclipse RCP editor coordinating tab views (Chat, Development, Architecture, Settings, Server) and saving serialized EMF resources.

### 21. Global UI Context Menu Integration
* **Primary Java Class:** `eu.kalafatic.evolution.view.menu.EvoGlobalContextMenuManager`
* **Source Location:** `eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/menu/EvoGlobalContextMenuManager.java`
* **Key Responsibility / Method:** `installGlobalMenuFilter(Display display)` — Attaches display-level `SWT.MenuDetect` filters to detect right-click interactions across controls and inject EVO AI actions.

### 22. Native LLM Model Inference Runtime
* **Primary Java Class:** `eu.kalafatic.evolution.forge.model.inference.ReferenceEvoInferenceEngine`
* **Source Location:** `eu.kalafatic.evolution.forge.model/src/eu/kalafatic/evolution/forge/model/inference/ReferenceEvoInferenceEngine.java`
* **Key Responsibility / Method:** `generate(...)`, `smokeTest(...)` — Provides local Java token inference and smoke-testing for compiled `.evo` model artifacts.

### 23. Asynchronous Event Bus & Process State Tracker
* **Primary Java Class:** `eu.kalafatic.evolution.controller.workflow.RuntimeEventBus`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/workflow/RuntimeEventBus.java`
* **Key Responsibility / Method:** `publish(RuntimeEvent event)`, `subscribe(...)` — Event bus routing execution state events (`TASK_COMPLETED`, `FORGE_TRAINING_FAILED`) to subscribers without thread deadlocks.

### 24. Task Stack Execution Manager
* **Primary Java Class:** `eu.kalafatic.evolution.view.editors.pages.TaskStackPage`
* **Source Location:** `eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/editors/pages/TaskStackPage.java`
* **Key Responsibility / Method:** `addTask(...)`, `runNextTask()` — Manages user task queuing, scenario preset loading, and background execution sequencing.

### 25. Unified UI Style & Layout System
* **Primary Java Class:** `eu.kalafatic.evolution.controller.ui.EvoStyleManager`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/ui/EvoStyleManager.java`
* **Key Responsibility / Method:** `getEvoStyleCss()`, `injectEvoStyle(String html)` — Injects design CSS variables and pure JS draggable sash splitters into embedded HTML pages.

### 26. AI Chat Orchestration Engine
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.selfdev.ChatEngine`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/ChatEngine.java`
* **Key Responsibility / Method:** `processChatMessage(...)` — Manages conversational chat state, context assembly from memory, and LLM inference generation.

### 27. Git Version Control Provider
* **Primary Java Class:** `eu.kalafatic.evolution.controller.orchestration.selfdev.GitSourceProvider`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/selfdev/GitSourceProvider.java`
* **Key Responsibility / Method:** `checkRepository(...)`, `copyDirectory(...)` — Enforces read-only Git repository verification, fetch/pull updates, revision hash tracking, and workspace source copying.

### 28. Structured Output Schema Validation
* **Primary Java Class:** `eu.kalafatic.evolution.controller.parsers.structured.SchemaValidator`
* **Source Location:** `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/parsers/structured/SchemaValidator.java`
* **Key Responsibility / Method:** `validate(JSONObject json, Schema schema)` — Performs recursive JSON structural type checking and in-place scalar-to-object auto-normalization.

---

## 4. Architectural Analysis & Discovered Insights

1. **Strict Decoupling of Forging & Execution Subsystems**: The dataset pipeline (`eu.kalafatic.evolution.forge.data`), model training (`eu.kalafatic.evolution.forge.trainer`), and self-development pipeline (`eu.kalafatic.evolution.controller.orchestration.selfdev`) are strictly modularized into independent OSGi bundles with explicit boundary interfaces.
2. **Deterministic Preflight Enforcement**: Subsystems enforce pre-execution invariants (e.g. `GitCheckTask` blocking downstream phases if the Git repository is dirty or unreachable, and `ForgePreflightValidator` checking disk space and memory before training starts).
3. **Multi-Tier Robustness & Fallbacks**: Capabilities such as dataset acquisition (`HuggingFaceDatasetSource`) and local LLM execution (`OllamaProvider`) implement explicit 3-tier fallback strategies (e.g. Rows API → Parquet API → Hub Tree API for Hugging Face datasets) to maintain operational stability.
4. **Single Source of Truth Configuration**: System configuration and workspace path resolution are centralized under `ResourceManager` and `ProjectModelManager` (backed by EMF `evolution.ecore`), eliminating heuristic path searching across disk.
