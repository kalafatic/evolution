# EVO MODES: FLOW ANALYSIS REPORT

## 1. Introduction
This report provides a comprehensive analysis of the three primary operational modes of the EVO Forge platform: **Iterative Darwin**, **Mediated**, and **Self-Dev**. It details the architectural components, logic flows, and integration points for each mode as implemented in the unified evolutionary kernel.

---

## 2. Iterative Darwin Mode (The Cognitive Kernel)
The Darwin Mode is the platform's central evolutionary engine, designed to solve technical tasks through divergent mutation and grounded evaluation.

### 2.1 Core Components
*   **`IterationManager`**: The single source of truth for kernel state transitions.
*   **`DarwinEngine`**: Orchestrates the evolutionary loop (mutation, evaluation, selection).
*   **`SiblingGenerationManager`**: Manages the sequential generation of semantically divergent trajectories.
*   **`TrajectoryTerritoryMapper`**: Discovers unexplored "technical quadrants" for mutation.
*   **`DarwinVariantSpawner`**: Materializes blueprints into concrete technical proposals.

### 2.2 Logic Flow
1.  **Intent Classification**: `PromptIntentAnalyzer` classifies the user prompt into `CHAT`, `TASK`, or `CONTROL`.
2.  **Strategic Initialization**: The kernel initializes the `OrchestrationState`, establishes the `GoalModel`, and locks the `AbstractionLevel` (Architecture, Design, or Implementation).
3.  **Recursive Discovery**: The `RealityDiscoveryAgent` builds or refines the `TargetRealityModel` from the repository's semantic snapshot.
4.  **Territory Mapping**: The system maps the evolutionary territory to identify dimensions for mutation (e.g., Modularity, Resilience).
5.  **Sequential Branching**: Competing `BranchVariant` proposals are generated. Each sibling is forced to diverge from existing siblings to maximize design space coverage.
6.  **Parallel Evaluation**: Variants are implemented in isolated `git worktrees` and verified through a "Reality Gate" (Compilation, Tests, Static Analysis).
7.  **Selection & Execution**: The `ActivationResolver` selects a winner based on fitness and architectural policy. The winning variant is merged into the stable lineage.

---

## 3. Mediated Mode (The Architectural Bridge)
Mediated Mode allows the platform to act as a senior architect that prepares high-signal context for external frontier models.

### 3.1 Core Components
*   **`RealityDiscoveryAgent`**: Performs the deep 8-pass recursive reconstruction of repository reality.
*   **`ContextCurator`**: Selects a critical subset (4-16 files) of the repository based on graph centrality and knowledge gaps.
*   **`MediatedExportManager`**: Handles the serialization of architectural understanding into portable packages.

### 3.2 The 8-Pass Discovery Process
1.  **Metadata Loading**: Loads `EvoMetadata` from files.
2.  **Local Responsibility**: Identifies artifact inputs, outputs, and decisions.
3.  **Relationship Discovery**: Analyzes dependencies and evolutionary influence.
4.  **Subsystem Discovery**: Identifies clusters and boundaries.
5.  **Reality Synthesis**: Summarizes the domain and calculates completeness.
6.  **Genome Discovery**: Identifies portable architectural patterns.
7.  **Compression**: Condenses observations into a high-signal "architectural essence."
8.  **Use Case Discovery**: Maps repository structures to executable use cases.

### 3.3 Export Convergence
The final output is a **Genome A/B** package:
*   **Genome A**: Optimized prompt for the external model.
*   **Genome B**: Architectural 이해 (understanding) and a ZIP of curated files.

---

## 4. Self-Dev Mode (The Sovereign Loop)
Self-Dev mode is the autonomous evolution of the EVO Forge platform itself.

### 4.1 Core Components
*   **`SelfDevSupervisor`**: A separate JVM process that monitors the EVO kernel and performs rollbacks if a self-mutation bricks the system.
*   **`RestartManager`**: Coordinates the safe shutdown and restart of the OSGi container to apply self-mutations.

### 4.2 Logic Flow (The Sovereign Pulse)
1.  **Self-Scan**: EVO treats its own source code as the "Target Reality."
2.  **Mutation**: Darwin generates tasks to fix bugs (from its own logs) or implement features.
3.  **Application**: The platform applies the changes and triggers a restart.
4.  **Verification**: The `SelfDevSupervisor` verifies that the kernel successfully bootloaded. If it fails, it performs an emergency Git rollback to the last stable state.

---

## 5. Unified Flow & Mandates
All modes are unified within the `DarwinEngine` and adhere to core mandates:
*   **Stamping Mandate**: Every evolutionary decision and branch status is explicitly logged (`[DARWIN_BRANCHES]`) to synchronize the non-linear UI tree.
*   **Dimension Mandate**: Variants must compete on a specific technical dimension while fixing other architectural variables.
*   **Materialization Mandate**: The engine must strictly implement the selected blueprint's philosophy without re-interpreting the original prompt from scratch.

---
**Status:** Completed Analysis
**Target:** Milestone Maturity
