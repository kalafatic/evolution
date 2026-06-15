# Mediated Mode Analysis — Evolutionary Milestone

## Goal
Understand how context compression and export evolution work within the Mediated Mode architecture.

---

## 1. Reality Model Lifecycle

### Reconstruction Process
The `TargetRealityModel` is built via an 8-pass recursive process in `RealityDiscoveryAgent`:
1.  **Metadata Loading**: Loads `EvoMetadata` from files (summaries, authority scores).
2.  **Local Responsibility**: Identifies inputs, outputs, and decisions for individual artifacts.
3.  **Relationship Discovery**: Analyzes incoming/outgoing dependencies and "evolutionary leverage."
4.  **Subsystem Discovery**: Identifies clusters of interacting artifacts and defines boundaries.
5.  **Reality Synthesis**: Summarizes the domain, purpose, and architecture; calculates completeness.
6.  **Genome Discovery**: Identifies portable architectural "genes" and patterns.
7.  **Compression**: Condenses observations into a high-signal "architectural essence."
8.  **Use Case Discovery**: Maps repository structures to executable use cases.

### Storage & Persistence
*   **Storage**: The model is stored as a `TargetRealityModel` object in the `TaskContext` metadata (`targetRealityModel`).
*   **Persistence**: It is serialized to JSON (`reality-model.json`) during the export process by `MediatedExportManager`. The `XmiForgeRepository` also handles persistent storage for complex graph states in the Forge Lab.

---

## 2. Hotspot Discovery Lifecycle

### Ranking Heuristics
Hotspots are identified and ranked based on:
1.  **Graph Centrality**: `getTopCentralNodes()` calculates in-degree and out-degree. High-degree nodes are primary hotspot candidates.
2.  **Architectural Authority**: Metadata scores (from `EvoMetadata`) indicate the "importance" of a file as defined by previous analysis or developer annotations.
3.  **Evolutionary Influence**: `Relationship Discovery` (Pass 3) explicitly asks the LLM to score "influence" (0.0-1.0) and identify "break impacts."
4.  **Semantic Density**: `ContextCurator` scores files based on the number of structures (methods, classes) and attributes they contain.

---

## 3. File Ranking & Selection Lifecycle

### Selection Mechanism (The 4-16 File Constraint)
*   **How does it work?**
    *   `ContextCurator.selectContext()` uses a "Coverage-Driven" scoring system.
    *   **Pass A (Critical Coverage)**: Selects files based on Authority + Knowledge Gaps + Influence.
    *   **Pass B (Subsystem Boundaries)**: Ensures at least one boundary file from each subsystem is included.
    *   **Fallback**: Ensures a minimum of 4 high-centrality files if the budget permits.

### Scoring & Judgment
*   **Judgment**: The selection is a hybrid of **Graph Analysis** (centrality), **Heuristics** (keywords like "manager", "kernel"), and **LLM Judgment** (during Pass 2 & 3 of discovery).
*   **Enforcement**: `DarwinFitnessRanker.calculateMediationFitness()` applies a strict penalty for violating the 4-16 range:
    *   **Ideal (4-16)**: `+0.2` reward.
    *   **Insufficient (<4)**: `-0.15` penalty.
    *   **Bloat (>16)**: `-0.05` penalty per file over 16.
    *   **Empty**: `-0.5` fatal penalty.

---

## 4. Compression & Information Loss

### Compression Lifecycle
*   **Process**: Pass 7 of discovery asks the LLM to compress the entire `TargetRealityModel` into "3 paragraphs" of high-signal architectural essence.
*   **Export**: `MediatedExportManager` creates specialized projections (Architecture, Implementation, Genome) to ensure that only relevant information is passed to the next evolutionary stage.

### Information Loss Analysis
*   **Darwin Lineage Survival**:
    *   **Current Export Leak**: The export currently focuses on the *state* of the reality model but may lose the *reasoning* behind branch selection.
    *   **Missing Data**: While `evolution-analysis.md` (history analysis) is included, the specific "why" a branch won vs. lost is often flattened into the `architecture.md` summary.
    *   **Continuity Risk**: If the "Survival Argument" and "Tradeoffs" from the Darwin round aren't explicitly injected into the `execution-instructions.md`, the external LLM loses the "lineage context" (the memory of rejected siblings).

### Information Measure
*   **Before Export**: Full `TargetRealityModel` containing all 8 passes, including knowledge gaps, individual architectural facts, and the complete influence graph.
*   **After Export**: A distilled ZIP containing the `reality-model.json` (full state) but often a reduced `prompt.md` and `affected-files/` set.
*   **Loss**: The primary loss is **Relational Granularity**. While the facts are there, the "narrative of discovery" (how Pass 2 led to Pass 4) is lost during compression, potentially causing the next model to re-discover known facts.
