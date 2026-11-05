# Functionality Intelligence Subsystem: Audit & Execution Architecture

## 1. Overview
The Functionality Intelligence subsystem provides an evidence-based, reliable inventory of system capabilities across the EVO platform. It bridges codebase discovery, semantic sidecars (`.ai.json`), architectural inspection, and isolated evolutionary execution (ANALYZE and REFACTOR workflows) within Darwin Engine without introducing redundant orchestrators.

## 2. Core Invariants & Architecture Components
- **`FunctionalityMetadata extends EvoMetadata`**:
  Models capability identity (`functionalityId`, `functionalityName`, `primaryClass`, `fqcn`, `moduleName`, `sourcePath`, `implementationStatus`), evaluation provenance (`evaluationRationale`, `evaluationMethod`, `confidenceLevel`, `evaluationTimestamp`), and 6 evaluation dimensions (0-100 scale):
  1. `Importance` (35% weight)
  2. `Maturity` (25% weight)
  3. `References / Centrality` (20% weight)
  4. `Size / Complexity` (10% weight)
  5. `Maintainability Risk` (10% inverted weight)
- **`AIContextTool` & Semantic Freshness**:
  Inspects sidecar files `<sourceFile>.ai.json`. Compares last modified timestamps: if the source file is modified after the sidecar timestamp, the metadata is marked as `stale` (`metadataFresh = false`).
- **`FunctionalityInventoryManager`**:
  Maintains the authoritative discovery catalogue of core EVO functionalities (FUNC-01 through FUNC-28). Enriches entries with source path validation, sidecar metadata, and persistence into `genome/current/functionality_inventory.json` and historical snapshots under `genome/history/<timestamp>/`.
- **`FunctionalityInventoryGroup` (TaskStackPage)**:
  Provides the SWT TableViewer interface with color-coded status badges, tooltips, editing dialogs, and actions.
  - Action column (`▶`): Triggers isolated execution using the row's configured mode.
  - Multi-select actions (`Analyze in AI Session`, `Refactor in AI Session`): Launches isolated execution for selected items.
  - Mode Selection: Supports `self-dev`, `mediated`, `intent`, `dream`, and `darwin`.
  - Context Bounding: Truncates source code snippets safely to avoid LLM context overflow while preserving essential structural context and traceability.
- **Darwin Execution Integration (`AiCommanderPage`)**:
  Maps task types directly to Darwin engine types (`SELF_DEV_MODE`, `MEDIATED_MODE`, `INTENT_MODE`, `DREAM_MODE`, `DARWIN_MODE`), configuring instructions and launching through the existing Darwin state machine.
- **Genome Milestone Integration (`MilestoneGenerator`)**:
  Produces `functionality_inventory.json` as an authoritative milestone artifact.

## 3. Verification & Compliance
- All 28 catalogue entries map directly to real Java files within the repository.
- Stale detection verified when source files are touched.
- Build and test verification completed with 100% test passage.
