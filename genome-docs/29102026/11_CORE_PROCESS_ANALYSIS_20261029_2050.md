# CORE PROCESS ANALYSIS REPORT

**Document Type:** Universal Process Analysis & Execution Path Reconstruction
**Project:** EVO / Evolution
**Analysis Timestamp:** 2026-10-29T20:50:00Z
**Directory Location:** `genome-docs/29102026/11_CORE_PROCESS_ANALYSIS_20261029_2050.md`
**Git Branch:** `jules-5933475433298650312-6c1d7316`
**Commit:** `6f7df5711e00b2b7b1f40b7aa90d1a8ab1c90bdf`

---

## 1. SELECTED CORE PROCESSES

The four key processes selected for deep analysis represent the primary operational axes of the EVO platform:

1. **Process 1: Self-Development Orchestration Loop (`SelfDevOrchestrator.java`)**
2. **Process 2: Authoritative Forge LLM Training Pipeline (`ForgeOrchestratorImpl.java`)**
3. **Process 3: Incremental Genome & Milestone Generator (`MilestoneGenerator.java`)**
4. **Process 4: Process Adapter Resolution & Workbench Tab Synchronization (`ProcessAdapterFactory.java` & `MultiPageEditor.java`)**

---

## 2. PROCESS SPECIFICATIONS & RECONSTRUCTED EXECUTION PATHS

### 2.1 Process 1: Self-Development Orchestration Loop
* **Primary Class:** `eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator`
* **Purpose:** Provides fully autonomous or user-triggered self-modification, compilation, export, deployment, and verification of EVO itself.
* **Triggers:** User action in `DevelopmentPage` or REST submission via `EvolutionServer`.
* **Execution Path (6 Stages):**
  1. `GIT_CHECK`: Verifies working tree cleanliness via `GitTool.checkStatus()`.
  2. `COPY`: Staging workspace copy to `self-dev-run/`.
  3. `BUILD`: Invokes `MavenBuildExecutor` (`mvn clean package`).
  4. `EXPORT`: Validates `EVO_RCP` export artifact production.
  5. `START`: Invokes `EvoRcpRuntime.start()`, deploying export to `<runtimeDir>/evo` and executing network firewall readiness checks via `NetworkAccessManager`.
  6. `VERIFY`: Executes `EvoRcpVerifier` checking `evo_ready.txt`, HTTP `/server/status`, and OSGi bundles.
* **Invariants:** Session isolation per task run, mandatory pre-launch network access rule verification, idempotent staging cleanup.
* **Error Handling:** Captures Win32 security blocks, exit codes, and process timeouts, outputting structured `TaskResult` diagnostics without altering system security settings.

### 2.2 Process 2: Authoritative Forge LLM Training Pipeline
* **Primary Class:** `eu.kalafatic.evolution.forge.controller.ForgeOrchestratorImpl`
* **Purpose:** Orchestrates dataset acquisition, preflight validation, tokenization, neural network training, model validation, and `.evo`/`.gguf` export.
* **Triggers:** User action on `ForgePage` or REST call `POST /forge/prepare`.
* **Execution Path:**
  1. `Source Analysis`: `ForgeSourceAnalyzer` inspects HF datasets, local folders, or `.evodata` files.
  2. `Dataset Composition & Acquisition`: Delegates to `TrainingDataAcquisitionService`, applying multi-tier HTTP fallbacks (`HuggingFaceDatasetSource`: Rows API -> Parquet API -> Hub Tree API).
  3. `Preflight Validation`: `ForgePreflightValidator` verifies byte target thresholds (`isTargetReached()`), token budgets, and memory bounds.
  4. `Training Execution`: Invokes `EvoLlmTrainer.train(...)` with prompt loss masking and step/epoch progress listeners.
  5. `Validation & Artifact Export`: `EvoModelValidator` verifies non-empty parameters, valid vocabulary, and lack of NaN values before writing `.evo` and `.gguf` artifacts.
* **Invariants:** Rejection of shortfalls when `isTargetReached() == false`, session-isolated telemetry buffering via `ForgeSessionManager`.

### 2.3 Process 3: Incremental Genome & Milestone Generator
* **Primary Class:** `eu.kalafatic.evolution.selfdev.genome.MilestoneGenerator`
* **Purpose:** Captures repository knowledge state snapshots, computes file inventory SHA-256 hashes, tracks incremental changes (`NEW`, `MODIFIED`, `UNCHANGED`), and generates milestone documentation.
* **Triggers:** User action on `ArchitecturePage` or scheduled background job.
* **Execution Path (8 Stages):**
  1. `FILE_INVENTORY`: Scans project files and calculates SHA-256 checksums.
  2. `SCAN_ARCHITECTURE`: Delegates to `RepoArchitectureScanner` for component tree mapping.
  3. `GENERATE_DOCUMENTS`: Formats `architecture.md`, `use_cases.md`, `milestone_v1.md`.
  4. `COMPILE_GENOME_JSON`: Assembles structured facts, observations, and recommendations into `genome.json`.
  5. `STAGING_COMMIT`: Performs atomic file writes to temporary staging directory.
  6. `HISTORICAL_SNAPSHOT`: Writes dated snapshot under `genome/history/YYYY/YYYY-MM-DD/timestamp/`.
  7. `LIVE_UPDATE`: Updates `genome/current/` if non-zero changes detected.
  8. `FUNCTIONALITY_INVENTORY`: Invokes `FunctionalityInventoryManager` to write `functionality_inventory.json`.
* **Invariants:** Idempotent execution (skips live update if 0 changes detected), atomic staging directory swap.

### 2.4 Process 4: Process Adapter Resolution & Workbench Tab Synchronization
* **Primary Classes:** `eu.kalafatic.evolution.controller.orchestration.adapter.ProcessAdapterFactory`, `eu.kalafatic.evolution.view.editors.MultiPageEditor`
* **Purpose:** Normalizes diverse process execution types (Darwin Iterations, Forge Training Epochs, Self-Dev Steps) into unified `IProcessAdapter` metrics and synchronizes active session state across all 8 workbench editor tabs.
* **Triggers:** Tab switch in `MultiPageEditor.pageChange(int newPageIndex)` or active session context updates.
* **Execution Path:**
  1. `Context Resolution`: Inspects `AiCommanderPage.getCurrentSessionName()` and active process trackers (`ContextProcessTracker`, `ForgeSessionManager`).
  2. `Adapter Resolution`: `ProcessAdapterFactory` resolves concrete `IProcessAdapter` instance (`DarwinProcessAdapter`, `ForgeProcessAdapter`, `WorkflowProcessAdapter`).
  3. `Tab Synchronization`: `MultiPageEditor` invokes `refreshUI()` on target page (`IterationPage`, `ForgePage`, `DevelopmentPage`, `TaskStackPage`, `ArchitecturePage`, `ComparePage`).
* **Invariants:** Thread-safe UI updates on SWT Display thread via `Display.asyncExec()`, fallback session preservation.

---

## 3. ROOT CAUSE & ARCHITECTURAL VERIFICATION FINDINGS

1. **Self-Dev Launch Security & Preflight Invariants:** `NetworkAccessManager` correctly handles missing admin elevation by falling back to loopback (127.0.0.1) without throwing blocking process launch exceptions.
2. **Forge Acquisition Pre-Training Guard:** `ForgeOrchestratorImpl` strictly enforces `isTargetReached() == true`, preventing incomplete dataset acquisitions from corrupting model training cycles.
3. **Genome Snapshot Idempotency:** `MilestoneGenerator` uses atomic staging and SHA-256 comparison to prevent zero-change disk churn.
4. **SWT Edge IPC & UI Threading Protection:** All browser bridge registrations are guarded by registration boolean flags and wrapped in `Display.asyncExec()` to eliminate thread access exceptions and Edge browser IPC timeouts.
