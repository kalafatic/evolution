# GENOME & MILESTONE BASELINE REPORT

**Document Type:** Universal Genome Baseline & Capability Inventory
**Project:** EVO / Evolution
**Analysis Timestamp:** 2026-10-29T20:44:00Z
**Directory Location:** `genome-docs/29102026/GENOME_MILESTONE_BASELINE_29102026.md`
**Git Branch:** `jules-5933475433298650312-6c1d7316`
**Execution Mode:** Analysis and Documentation Only

---

## 1. GENOME & MILESTONE SUBSYSTEM ANALYSIS

The Genome and Milestone subsystem (`eu.kalafatic.evolution.selfdev.genome`) provides structural evolutionary mapping, SHA-256 inventory tracking, and milestone generation.

### 1.1 Key Architecture Components
* **`SelfDevGenomeHub`**: Singleton hub tracking repository state snapshots, milestone history (`genome/history/YYYY/YYYY-MM-DD/timestamp/`), and upgrade plans.
* **`MilestoneGenerator`**: Multi-stage milestone compilation engine (calculates file inventory SHA-256 hashes, invokes `RepoArchitectureScanner`, formats `architecture.md`, `use_cases.md`, `genome.json`, and Stage 8 `functionality_inventory.json`).
* **`FunctionalityInventoryManager`**: Catalogues system capabilities (`FunctionalityEntry`), computes 6 evaluation dimension scores, enriches `.ai.json` sidecars, and persists `functionality_inventory.json`.

---

## 2. FUNCTIONALITY INVENTORY & OPERATIONAL CAPABILITIES

Below is the verified inventory mapping key functionalities to their implementation status:

| # | Functionality | Primary Class | OSGi Module | Status | Verification Evidence |
|---|---------------|---------------|-------------|--------|------------------------|
| 1 | Self-Dev Workflow | `SelfDevOrchestrator` | `controller` | OPERATIONAL | Tested 6-stage lifecycle, deployer, verifier. |
| 2 | Cognitive Loop Engine | `CognitiveLoopEngine` | `controller` | OPERATIONAL | Solves `CognitiveGoal` with dynamic strategies. |
| 3 | LLM Forging Pipeline | `ForgeOrchestratorImpl` | `forge.controller` | OPERATIONAL | End-to-end dataset acquisition, training, export. |
| 4 | Native Java LLM Trainer | `EvoLlmTrainer` | `forge.trainer` | OPERATIONAL | Prompt loss masking, validation loss reuse, profiles. |
| 5 | BPE Tokenizer Training | `SimpleBPETokenizer` | `forge.tokenizer` | OPERATIONAL | Fast unique-word corpus sampling (<1s). |
| 6 | `.evodata` Dataset Archive | `EvoDatasetArtifact` | `forge.data` | OPERATIONAL | Clean Zip handling, leakage verification, metadata. |
| 7 | Multi-Tier HF Downloader | `HuggingFaceDatasetSource` | `forge.data` | OPERATIONAL | 5-hop redirect, Parquet & Hub Tree API fallbacks. |
| 8 | Dataset Deduplication | `DatasetDeduplicator` | `forge.data` | OPERATIONAL | MinHash 5-gram shingles & SHA-256 hash matching. |
| 9 | Standalone GPU Diagnostic | `GpuDiagnosticMain` | `gpu` | OPERATIONAL | CUDA FFM lookup, `nvidia-smi` detection. |
| 10 | Genome & Milestones | `MilestoneGenerator` | `selfdev.genome` | OPERATIONAL | Multi-stage snapshot & inventory generation. |
| 11 | Process Supervisor | `SupervisorMain` | `supervisor` | OPERATIONAL | Dual-mode launcher, REST servers, RCP window. |
| 12 | Embedded HTTP Server | `EvolutionServer` | `servers` | OPERATIONAL | NanoHTTPD endpoints for chat, forge, develop, API. |
| 13 | RBAC Security System | `AuthorizationService` | `servers` | OPERATIONAL | SQLite database, principal policies, DENY precedence. |
| 14 | Session-Isolated Console | `SessionLogger` | `controller` | OPERATIONAL | Per-session file logs and Eclipse console streams. |
| 15 | Contextual Help Service | `ContextualHelpService` | `controller` | OPERATIONAL | Debounced 3-level assistance (Selection, Process, Memory). |
| 16 | User Memory Subsystem | `MemoryService` | `controller` | OPERATIONAL | Persistent workspace JSON storage (`user_memory.json`). |
| 17 | Background Dream Engine | `DreamEngine` | `controller` | OPERATIONAL | Read-only background experience analysis. |
| 18 | Target Reality Discovery | `RealityDiscoveryAgent` | `controller` | OPERATIONAL | Category detection (`PROJECT`, `DATASET`, etc.), sample extraction. |
| 19 | General-Purpose INTENT | `IntentReconstructionEngine` | `controller` | OPERATIONAL | Universal file/dataset classification and discovery. |
| 20 | Unified Style System | `EvoStyleManager` | `controller` | OPERATIONAL | Station design tokens, draggable sash splitters. |
| 21 | Workbench MultiPageEditor| `MultiPageEditor` | `view` | OPERATIONAL | Tab synchronizations across 8 workstation pages. |
| 22 | Global Context Menu | `EvoGlobalContextMenuManager` | `view` | OPERATIONAL | Global SWT menu detection & merging. |
| 23 | Pre-Launch Net Security | `NetworkAccessManager` | `controller` | OPERATIONAL | Cross-platform firewall rules & executable permissions. |
| 24 | Architecture Page Nav | `ArchitecturePage` | `view` | OPERATIONAL | Progressive level scanning & SVG graph rendering. |
| 25 | Fast Startup Performance| `BackgroundDiscoveryService`| `controller` | OPERATIONAL | On-demand background jobs, fast Git scanning. |

---

## 3. ARCHITECTURAL BASELINE RULES & CONSTRAINTS FOR FUTURE TASKS

1. **User-Triggered Execution Invariant:** All heavy operations (scans, network downloads, preflight checks, context help, genome updates) MUST be strictly user-triggered via direct UI action or explicit configuration, NEVER automatically on selection change or startup.
2. **Read-Only / Non-Destructive Invariants:** Target discovery, INTENT mode, and DREAM mode operate strictly read-only on target artifacts.
3. **No Code Modification Rule for Analysis Tasks:** Analytical tasks must never modify production Java source code, POM files, generated EMF models, or product definitions.
4. **EMF Persistence Rule:** Programmatic EMF updates in UI pages must delegate persistence to editor save hooks (`editor.doSave()`) to prevent Eclipse buffer undo listener assertions.
5. **Thread Safety & SWT Access:** UI widget access and timer calls from background threads MUST use `Display.asyncExec()`.
