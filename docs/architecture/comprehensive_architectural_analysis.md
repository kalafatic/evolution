# EVO Platform: Comprehensive Architectural Analysis & Consolidation Roadmap
**Document Identifier**: `docs/architecture/comprehensive_architectural_analysis.md`
**Date**: October 12, 2026
**Status**: MILESTONE 0 ARCHITECTURAL ANALYSIS
**Target Audience**: Software Architects, Core Developers, Maintenance Engineering

---

## Executive Summary

The **EVO (Evolution Operating System)** platform is an OSGi-based modular workbench built on the Eclipse Rich Client Platform (RCP) framework using Java 21, Maven, Tycho 4.0.5, and EMF (Eclipse Modeling Framework). EVO integrates local and remote LLMs, autonomous agentic workspace evolution (Darwin Engine), neural model forging (Forge Engine), dataset acquisition/preparation pipelines (`.evodata`), out-of-process process supervision, and architectural knowledge tracking (Genome).

While the system is functionally mature and possesses a clear core vision, incremental development across multiple features has introduced architectural fragmentation. This document represents **Milestone 0: Comprehensive Architectural Analysis**, providing an evidence-based audit of the entire codebase across 8 mandatory dimensions.

---

## Section 1: Architecture Inventory

### 1.1 OSGi Bundle Taxonomy & Module Catalog

The platform comprises 30 Maven/Tycho OSGi bundle projects aggregated under `eu.kalafatic.evolution.aggregator` (version `2.6.5-SNAPSHOT` / `1.0.0-SNAPSHOT` for supervisor). The modules fall into four distinct architectural layers:

```
+---------------------------------------------------------------------------------------------------+
| PRESENTATION & WORKBENCH LAYER                                                                    |
|  - eu.kalafatic.evolution.view                                    - eu.kalafatic.evolution.media  |
|  - eu.kalafatic.evolution.model.edit                              - eu.kalafatic.evolution.model.editor
+---------------------------------------------------------------------------------------------------+
                                                  |
                                                  v
+---------------------------------------------------------------------------------------------------+
| ORCHESTRATION & CONTROLLER LAYER                                                                  |
|  - eu.kalafatic.evolution.controller                              - eu.kalafatic.evolution.servers|
|  - eu.kalafatic.evolution.supervisor                              - eu.kalafatic.evolution.creatic |
+---------------------------------------------------------------------------------------------------+
                                                  |
                                                  v
+---------------------------------------------------------------------------------------------------+
| SUBSYSTEM & DOMAIN ENGINES                                                                        |
|  - eu.kalafatic.evolution.forge.controller                        - eu.kalafatic.evolution.forge.trainer
|  - eu.kalafatic.evolution.forge.data                              - eu.kalafatic.evolution.forge.model
|  - eu.kalafatic.evolution.forge.runtime                           - eu.kalafatic.evolution.forge.math
|  - eu.kalafatic.evolution.forge.tokenizer                         - eu.kalafatic.evolution.forge.observability
|  - eu.kalafatic.evolution.forge.agent.api                         - eu.kalafatic.evolution.forge-lab
|  - eu.kalafatic.evolution.selfdev.genome                                                          |
+---------------------------------------------------------------------------------------------------+
                                                  |
                                                  v
+---------------------------------------------------------------------------------------------------+
| CORE MODEL & UTILITY LAYER                                                                        |
|  - eu.kalafatic.utils                                             - eu.kalafatic.evolution.model  |
+---------------------------------------------------------------------------------------------------+
```

#### Complete Module Catalog
1. **`eu.kalafatic.utils`**: Base utility library. String manipulation, process execution wrappers (`ProcessUtils`), UI constants (`FUIConstants`), semantic metadata helpers (`AIContextTool`).
2. **`eu.kalafatic.evolution.model`**: Primary domain persistence layer. Holds `evolution.ecore` EMF model (`EvolutionProject`, `SelfDevContext`, `ForgeSession`, `NetworkEntry`, `TaskContext`).
3. **`eu.kalafatic.evolution.model.edit`**: Generated EMF Edit item providers for JFace property sheets and trees.
4. **`eu.kalafatic.evolution.model.editor`**: Generated EMF editor UI actions and wizard pages.
5. **`eu.kalafatic.evolution.model.tests`**: Unit test suite for generated EMF model interfaces.
6. **`eu.kalafatic.evolution.controller`**: Central platform orchestrator. Contains `ResourceManager`, `CognitiveLoopEngine`, `SelfDevOrchestrator`, `LLMDarwinEngine`, `MemoryService`, `ContextualHelpService`, `OllamaProvider`, and `RuntimeEventBus`.
7. **`eu.kalafatic.evolution.controller.tests`**: Unit and integration tests for task orchestration and controller tasks.
8. **`eu.kalafatic.evolution.servers`**: Embedded NanoHTTPD REST servers (`EvolutionServer`), OSGi bundle state inspection endpoint (`/server/osgi`), and MCP protocol implementation (`eu.kalafatic.evolution.servers.mcp`).
9. **`eu.kalafatic.evolution.view`**: Primary Eclipse RCP UI. Multi-page editors (`MultiPageEditor`), view pages (`DevelopmentPage`, `ArchitecturePage`, `TaskStackPage`, `McpSettingsPage`, `ContextPage`), property groups (`DatasetEditorGroup`, `ChatMgmtGroup`), menus (`EvoGlobalContextMenuManager`), and dialogs.
10. **`eu.kalafatic.evolution.supervisor`**: Independent supervisor process (`SupervisorMain`, `EVOSupervisorServer` on port 8089, `EVOSupervisorControlServer` on port 28080) for out-of-process build, deployment, and health monitoring.
11. **`eu.kalafatic.evolution.selfdev.genome`**: Architectural knowledge, milestone generation (`MilestoneGenerator`), file inventory hashing, and upgrade planning (`SelfDevGenomeHub`).
12. **`eu.kalafatic.evolution.selfdev.genome.tests`**: Test suite for genome snapshot generation and differential analysis.
13. **`eu.kalafatic.evolution.forge.tokenizer`**: LLM BPE and character tokenization implementations (`SimpleBPETokenizer`).
14. **`eu.kalafatic.evolution.forge.math`**: Tensor operations, matrix math, SIMD CPU math, and activation functions (`Tensor`, `MatrixMath`).
15. **`eu.kalafatic.evolution.forge.model`**: Neural network architecture specifications (`EvoLlmArchitecture`), model artifact containers (`EvoModelArtifact`), and parameter sizing profiles.
16. **`eu.kalafatic.evolution.forge.observability`**: Training metrics recording, loss history tracking, and progress monitors.
17. **`eu.kalafatic.evolution.forge.agent.api`**: Interfaces for source code analysis, model exporters (`OllamaExporter`), and GGUF format validators (`GGUFValidator`).
18. **`eu.kalafatic.evolution.forge.runtime`**: JVM native Causal LM inference runtime (`ReferenceEvoInferenceEngine`) with KV-Cache acceleration.
19. **`eu.kalafatic.evolution.forge.data`**: Dataset acquisition engine, Hugging Face dataset downloader (`HuggingFaceDatasetSource`), Parquet/JSONL adapters, deduplication (`DatasetDeduplicator`), quality scoring, and `.evodata` container generation (`EvoDataWriter`).
20. **`eu.kalafatic.evolution.forge.trainer`**: JVM native Backpropagation and AdamW optimizer implementation (`EvoLlmTrainer`).
21. **`eu.kalafatic.evolution.forge.controller`**: Authoritative Forge pipeline coordinator (`ForgeOrchestratorImpl`) and job lifecycle manager (`ForgeJob`).
22. **`eu.kalafatic.evolution.forge-lab`**: Experimental model structures, synthetic data playgrounds, and research components.
23. **`eu.kalafatic.evolution.creatic`**: Generative code templates, agent prompt composers, and creative expansion utilities.
24. **`eu.kalafatic.evolution.media`**: Static UI image assets, flag icons, and branding artwork.
25. **`eu.kalafatic.evolution.tests`**: High-level platform integration test suite.
26. **`eu.kalafatic.evolution.quality.tests`**: Static analysis, package boundary enforcement, and quality assurance tests.
27. **`eu.kalafatic.evolution.feature`**: Eclipse feature packaging descriptor (`feature.xml`).
28. **`eu.kalafatic.evolution.repository`**: Tycho product build aggregator (`evolution.product`, `evo.product`).
29. **`eu.kalafatic.evolution.module`**: Sub-project POM aggregator module.
30. **`eu.kalafatic.evolution.aggregator`**: Parent reactor POM.

---

## Section 2: Dependency Map

### 2.1 Dependency Flow & Layering Analysis

The system architecture specifies strict unidirectional dependency flow across layers:

`Product / Feature` -> `UI Layer` -> `Controller & Server` -> `Subsystem Engines` -> `Core Model & Utils`

```
+-------------------------------------------------------------------+
|                        Product & Feature Layer                    |
|             (eu.kalafatic.evolution.repository, feature)          |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                           UI Layer                                |
|                 (eu.kalafatic.evolution.view)                     |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                    Controller & Server Layer                      |
|      (eu.kalafatic.evolution.controller, servers, supervisor)     |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                    Engine & Subsystem Layer                       |
|   (eu.kalafatic.evolution.forge.*, selfdev.genome, creatic)      |
+-------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                    Core Model & Utility Layer                     |
|           (eu.kalafatic.evolution.model, utils, media)            |
+-------------------------------------------------------------------+
```

### 2.2 Identified Dependency Anomalies & Coupling Hotspots

1. **Broad Manifest Exports in `eu.kalafatic.evolution.controller`**:
   - *Evidence*: `MANIFEST.MF` in `eu.kalafatic.evolution.controller` exports internal implementation packages directly (e.g. `eu.kalafatic.evolution.controller.orchestration.selfdev`, `eu.kalafatic.evolution.controller.orchestration.cognitive.loop`, `eu.kalafatic.evolution.controller.memory`, `eu.kalafatic.evolution.controller.tools`).
   - *Impact*: UI components in `eu.kalafatic.evolution.view` bind directly to concrete task and orchestrator classes rather than interface contracts.
2. **Direct Subsystem Implementation Coupling in Controller**:
   - *Evidence*: `eu.kalafatic.evolution.controller` explicitly requires all individual `forge.*` OSGi bundles (`eu.kalafatic.evolution.forge.controller`, `forge.data`, `forge.trainer`, `forge.model`, `forge.runtime`, `forge.agent.api`) in its `Require-Bundle` header rather than depending solely on service interfaces.
   - *Impact*: Any change in low-level forge engine implementations forces recompilation and re-resolution of the main controller bundle.
3. **UI Invocation of Lower-Level Resolvers**:
   - *Evidence*: `DatasetEditorGroup` (`eu.kalafatic.evolution.view.editors.pages.properties`) directly calls `HuggingFaceDatasetProvider` and `DatasetPreparationService` alongside direct EMF model mutations on `ForgeSession`.
   - *Impact*: Bypasses the authoritative `ForgeOrchestratorImpl` facade, leading to state divergence between UI dialogs and background tasks.

---

## Section 3: Responsibility and Service Ownership Map

To eliminate confusion, authoritative owners for major platform operations are defined below:

| Operation / System Area | Authoritative Owner Class | Package | Responsibility |
| :--- | :--- | :--- | :--- |
| **Persistent Configuration** | `ResourceManager` | `eu.kalafatic.evolution.controller.resource` | Single source of truth for EMF `.evo` / `evo_config.xml` model loading, saving, and canonical workspace path validation (`validateCanonicalPath`). |
| **Cognitive Control Loop** | `CognitiveLoopEngine` | `eu.kalafatic.evolution.controller.orchestration.cognitive.loop` | Solves `CognitiveGoal` objectives using decoupled `CognitiveStrategy` pools and tracks session attempt memory (`WorldState`). |
| **Forge Orchestration** | `ForgeOrchestratorImpl` | `eu.kalafatic.evolution.forge.controller.impl` | Lifecycle manager (`ForgeJob`) for model forging: source analysis, dataset composition, preflight validation, training, and export. |
| **Dataset Acquisition** | `TrainingDataAcquisitionServiceImpl` | `eu.kalafatic.evolution.forge.data.impl.service` | Multi-source streaming acquisition (HF REST API, Parquet, Hub tree), quality scoring, deduplication, and `.evodata` compilation. |
| **Native LLM Training** | `EvoLlmTrainer` | `eu.kalafatic.evolution.forge.trainer` | JVM native Backpropagation, loss calculation, AdamW optimization, and model checkpointing. |
| **Workspace Evolution (Self-Dev)**| `SelfDevOrchestrator` | `eu.kalafatic.evolution.controller.orchestration.selfdev` | Executes the 6-stage workspace update pipeline (`GIT_CHECK` -> `COPY` -> `BUILD` -> `EXPORT` -> `START` -> `VERIFY`). |
| **Process Supervision** | `SupervisorMain` / `EVOSupervisorServer` | `eu.kalafatic.evolution.supervisor` | Out-of-process REST supervisor server (port 8089/28080) for external task control and health verification. |
| **User Memory Management** | `MemoryService` | `eu.kalafatic.evolution.controller.memory` | Persistence and CRUD operations for structured `MemoryEntry` records in `${WORKSPACE}/shared/memory/user_memory.json`. |
| **Contextual Help** | `ContextualHelpService` | `eu.kalafatic.evolution.controller.orchestration` | Debounced (250ms) resolution of UI selection snapshots, process tracking, and 3-level context assistance generation. |
| **Genome Tracking** | `SelfDevGenomeHub` | `eu.kalafatic.evolution.selfdev.genome` | SHA-256 differential scanning (`MilestoneGenerator`), genome snapshot persistence, and upgrade planning. |
| **Embedded HTTP Server** | `EvolutionServer` | `eu.kalafatic.evolution.servers` | REST HTTP endpoint handler (`/develop/task`, `/forge/dataset/prepare`, `/server/osgi`) serving embedded NanoHTTPD instance. |

---

## Section 4: Duplication and Inconsistency Report

### 4.1 Identified Code Duplications & Competing Implementations

#### 1. Training & Dataset Preparation Duplication (`LLMDarwinEngine` vs `ForgeOrchestratorImpl`)
* **Evidence**: `LLMDarwinEngine` (`eu.kalafatic.evolution.controller.orchestration.selfdev`) implements its own ad-hoc model dataset preparation and training loops (`buildTrainingSamples`, `runLocalTraining`) rather than delegating model training operations exclusively to `ForgeOrchestratorImpl` and `EvoLlmTrainer`.
* **Impact**: Duplicated training logic, inconsistent loss metric recording, and divergent dataset filtering rules.

#### 2. Dual Pathways for Dataset Preparation
* **Evidence**: Dataset preparation exists in two distinct execution pathways:
  1. *UI In-Process Pathway*: `DatasetEditorGroup` / `DatasetPreparationService` in `eu.kalafatic.evolution.view` constructs `DatasetItem` objects directly on a background thread.
  2. *REST / Cognitive Loop Pathway*: `EvolutionServer` (`POST /forge/dataset/prepare`) -> `CognitiveLoopEngine` -> `DatasetAcquisitionTool` -> `TrainingDataAcquisitionService`.
* **Impact**: Inconsistent preflight validation, divergent error dialog handling, and redundant dataset normalization code.

#### 3. LLM Fallback Model Resolution Logic
* **Evidence**: Automated local model resolution logic is implemented in both `OllamaProvider.findWorkingFallbackModel` (`eu.kalafatic.evolution.controller.orchestration.llm`) and `OllamaService` (`eu.kalafatic.evolution.controller.service`).
* **Impact**: Differing fallback priorities and duplicate model health check requests sent to local Ollama endpoints.

#### 4. JSON Repair and Parsing Overlap
* **Evidence**: String JSON repair and normalization logic is duplicated across `JsonRepairEngine` (`eu.kalafatic.evolution.controller.parsers`), `JsonUtils` (`eu.kalafatic.utils`), `SchemaValidator`, and `StructuredResponsePipeline`.
* **Impact**: Unnecessary maintenance overhead and subtle parsing discrepancies when handling malformed LLM outputs.

#### 5. Process Runner and Output Draining Logic
* **Evidence**: Process stdout/stderr stream draining and thread joining logic is implemented independently in `ProcessRunner` (`eu.kalafatic.evolution.supervisor`), `MavenBuildExecutor` (`eu.kalafatic.evolution.controller`), and `EvoRcpRuntime`.
* **Impact**: Risk of stream buffer deadlocks or lingering processes if stream reader threads fail to exit cleanly upon timeouts.

---

## Section 5: UI Consistency Report

### 5.1 SWT / JFace Patterns and Inconsistencies

An audit of the primary UI pages and groups in `eu.kalafatic.evolution.view` revealed several inconsistencies across user interaction patterns:

1. **Table Viewers & Reordering Logic**:
   - `DevelopmentPage`: Renders `selfDevTable` with columns `#`, `Action`, `Edit`, `Executed From`, `Name`, `Command`, `Path`, `URL`, `Status`. Reordering uses context menu items `▲ Move Up` and `▼ Move Down` operating on `SelfDevRow.order`.
   - `FunctionalityInventoryGroup` (on `TaskStackPage`): Uses an expandable group containing a table with checkboxes (`SWT.CHECK`), cell editors (`ComboBoxCellEditor`), and manual move actions.
   - `DatasetEditorGroup` & `McpServersGroup`: Use standard SWT `TableViewer` controls without ordering actions.
   - *Consolidation Requirement*: Standardize table viewer helper utilities for column creation, row reordering, and double-click action bindings.

2. **Progress Feedback and Status Feedback**:
   - `PropertiesPage`: Renders a double-buffered custom canvas (`statusCanvas`, height 70px) driven by `OrchestrationStatusManager`.
   - `DatasetEditorGroup`: Renders an embedded HTML `#process-progress-bar` in an SWT Browser control.
   - `TaskStackPage` & `DevelopmentPage`: Use Eclipse platform status line contributions (`StatusLineContributionItem`) and background `Job` progress monitors.
   - *Consolidation Requirement*: Standardize long-running task progress reporting through unified `Job` monitors that synchronize both RCP status line items and embedded HTML visualizers.

3. **Error Reporting & Dialogs**:
   - `DatasetEditorGroup` uses direct modal `MessageDialog.openError(...)` popups on SWT UI thread.
   - `DevelopmentPage` handles exceptions via detailed error dialogs displaying full stack traces.
   - HTML-based browser groups (`forge.html`, `develop.html`) display custom HTML modal overlays (`showModalDialog`) with "Copy Content" buttons.
   - *Consolidation Requirement*: Standardize modal error reporting so exceptions occurring in background threads are safely routed to unified SWT error dialogs with clipboard details.

4. **Embedded Browser Styling & Draggable Sashes**:
   - Browser pages (`develop.html`, `forge.html`, `chat.html`, `workflow.html`) receive injected CSS from `EvoStyleManager.getInstance().injectEvoStyle(...)`.
   - However, individual JavaScript event bridges (`window.javaAction`) are implemented with slight signature variations across pages.
   - *Consolidation Requirement*: Centralize JavaScript bridge contracts and standard CSS classes (`.evo-panel`, `.evo-table`, `.evo-sash`) through `EvoStyleManager`.

---

## Section 6: Object-Oriented Design Report

### 6.1 Class Hierarchy and Responsibility Problems

1. **Overly Broad Base Classes**:
   - `ADarwinEngine` (`eu.kalafatic.evolution.controller.orchestration.selfdev`): Contains abstract evolutionary loop methods alongside concrete filesystem directory copying (`copyDirectory`) and branching limit checks.
   - `AbstractSelfDevTask`: Combines task dependency pre-validation, task result construction, and diagnostic stack-trace string formatting.
   - *Recommendation*: Refactor concrete utility methods into specialized helper classes (`DirectoryCopyUtil`, `TaskDiagnosticsFormatter`) using composition rather than inheritance.

2. **Excessive Responsibilities in Presentation Views**:
   - `DevelopmentPage` (`eu.kalafatic.evolution.view.editors.pages`): Responsible for:
     1) Building table widgets and handling cell selections.
     2) Managing row reordering logic.
     3) Invoking Git repository resolution via `ResourceManager`.
     4) Executing background task streams sequentially.
     5) Rendering scenario dialogs and copying text to clipboard.
   - *Recommendation*: Extract UI controller/presenter logic from `DevelopmentPage` into a decoupled `DevelopmentPageController` class.

3. **Public API Exposure vs Implementation Encapsulation**:
   - Packages in `eu.kalafatic.evolution.controller` expose internal implementation classes directly instead of defining strict interface boundaries.
   - *Recommendation*: Introduce dedicated API interface packages (e.g. `eu.kalafatic.evolution.controller.api`) and keep concrete task implementations package-private where feasible.

---

## Section 7: Architectural Risk Assessment

### 7.1 Key System Risks and Verification Safeguards

| Risk Area | Risk Description | Severity | Impact | Safeguard / Verification Requirement |
| :--- | :--- | :--- | :--- | :--- |
| **SWT UI Threading** | Asynchronous callbacks from background threads updating SWT controls without `Display.asyncExec` or missing `!control.isDisposed()` checks. | **HIGH** | `SWTException: Invalid thread access` crashing the RCP workbench UI. | Audit all `Display.getDefault().asyncExec` calls across UI groups and verify control disposal guards. |
| **Port Conflicts in Debug Mode** | Multiple concurrent self-dev runs or supervisor instances attempting to bind default ports (8089, 28080, 48081). | **HIGH** | Socket bind exceptions blocking application startup. | Enforce dynamic port offset mechanism (`+10` offset in `SelfDevContext.isDebugMode()`) and process ownership checks. |
| **EMF Resource Save Undo History Collisions** | Saving EMF models directly via `ProjectModelManager.saveResource` while open in `MultiPageEditor`. | **MEDIUM** | Platform `AssertionFailedException` in Eclipse `DocumentUndoManager`. | Enforce requirement: UI page saves MUST delegate to `MultiPageEditor.doSave(monitor)`. |
| **Memory Pressure in Dataset Streaming** | Streaming large Parquet or JSONL dataset files exceeding available JVM heap space. | **MEDIUM** | `OutOfMemoryError` during dataset preparation. | Enforce 2MB Snappy block caps and 50,000 candidate string extraction limits in `ParquetAdapter`. |
| **Process Buffer Deadlocks** | Process execution without asynchronous stdout/stderr stream draining. | **MEDIUM** | Process hanging indefinitely during Maven builds or Tycho compilation. | Enforce concurrent stream reader thread draining in `ProcessRunner` and `MavenBuildExecutor`. |

---

## Section 8: Refactoring Milestone Roadmap

To achieve complete architectural consolidation safely, implementation will proceed incrementally across 5 distinct, dependency-aware milestones:

```text
MILESTONE 0: Comprehensive Architectural Analysis (CURRENT)
  └── Complete codebase audit, document findings, present roadmap, await approval.

MILESTONE 1: UI Consolidation
  └── Standardize SWT table viewers, context menus, progress feedback, error dialogs, and CSS styling.

MILESTONE 2: Business Logic & Execution Flow Consolidation
  └── Unify dataset acquisition pathways, delegate LLMDarwinEngine training to ForgeOrchestratorImpl, consolidate JSON repair.

MILESTONE 3: Object-Oriented Design & Code Consolidation
  └── Refactor overly broad base classes, extract view presenters, refine package encapsulation.

MILESTONE 4: Module & Shared Infrastructure Consolidation
  └── Harden OSGi service boundaries, remove duplicate process runners, eliminate remaining static state leaks.

MILESTONE 5: Integration Verification & Completion
  └── Full reactor Tycho build, end-to-end integration tests, OSGi bundle resolution verification, final report.
```

### Milestone Details & Acceptance Criteria

#### MILESTONE 1: UI Consolidation
- **Scope**: `eu.kalafatic.evolution.view` (views, editors, pages, dialogs, menus).
- **Target Actions**:
  1. Extract reusable SWT table reordering and selection helpers.
  2. Standardize background task progress reporting using Eclipse `Job` monitors and `OrchestrationStatusManager`.
  3. Standardize modal error reporting and clipboard dialogs.
  4. Unify embedded browser JavaScript event bridge handling.
- **Acceptance Criteria**: All UI views render consistently, user workflows function without regression, no `SWTException` errors occur, and full RCP build compiles cleanly.

#### MILESTONE 2: Business Logic & Execution Flow Consolidation
- **Scope**: `eu.kalafatic.evolution.controller`, `eu.kalafatic.evolution.forge.controller`, `eu.kalafatic.evolution.servers`.
- **Target Actions**:
  1. Re-route `LLMDarwinEngine` training loops to delegate directly to `ForgeOrchestratorImpl` and `EvoLlmTrainer`.
  2. Align UI dataset preparation (`DatasetPreparationService`) and REST dataset preparation (`/forge/dataset/prepare`) to share `TrainingDataAcquisitionService`.
  3. Consolidate fallback model discovery between `OllamaProvider` and `OllamaService`.
  4. Unify JSON repair utilities into `JsonRepairEngine`.
- **Acceptance Criteria**: Single authoritative execution flow for dataset preparation and training, zero duplicate model resolution logic, all unit tests pass.

#### MILESTONE 3: Object-Oriented Design & Code Consolidation
- **Scope**: `eu.kalafatic.evolution.controller`, `eu.kalafatic.evolution.view`, `eu.kalafatic.evolution.selfdev.genome`.
- **Target Actions**:
  1. Extract presenter logic from `DevelopmentPage` into `DevelopmentPageController`.
  2. Refactor concrete utility methods out of `ADarwinEngine` and `AbstractSelfDevTask`.
  3. Clean up method signatures and narrow public API exposures.
- **Acceptance Criteria**: Improved separation of concerns, reduced class coupling, zero public API breakage for OSGi bundle consumers.

#### MILESTONE 4: Module & Shared Infrastructure Consolidation
- **Scope**: OSGi bundle manifests (`MANIFEST.MF`), `eu.kalafatic.evolution.supervisor`, `eu.kalafatic.utils`.
- **Target Actions**:
  1. Consolidate process stream draining and process runner implementations into `ProcessUtils`.
  2. Clean up `Require-Bundle` headers in OSGi manifests to depend on package imports where appropriate.
  3. Verify strict request-scoped session isolation across all asynchronous operations.
- **Acceptance Criteria**: Simplified module dependency graph, zero unresolved OSGi bundle dependencies (`/server/osgi` returns 100% active/resolved bundles), clean Tycho product build.

#### MILESTONE 5: Integration Verification & Completion
- **Scope**: System-wide integration and verification.
- **Target Actions**:
  1. Execute full Maven reactor build (`mvn clean verify`).
  2. Execute all unit and integration test suites across bundles.
  3. Verify exported product packaging (`evolution.product` and `evo.product`).
  4. Produce final consolidation report documenting all completed changes and verification results.
- **Acceptance Criteria**: 100% build success across all 30 reactor modules, all unit/integration tests pass, product exports successfully, zero architectural regressions.

---

## Conclusion & Next Steps

This **Milestone 0 Analysis** establishes a comprehensive, evidence-based baseline of the EVO platform architecture. Implementation of **Milestone 1 (UI Consolidation)** will begin immediately upon review and explicit approval of this roadmap.

**Status**: AWAITING APPROVAL TO PROCEED TO MILESTONE 1.
