# EVO Architecture Audit

## Executive Summary

*   **Overall Platform Stability:** **FRAGILE but FUNCTIONAL**. The core evolutionary workflows are wired and operational, but the system relies heavily on complex state management and LLM reliability.
*   **Major Architectural Risks:**
    *   **State Explosion:** The recursive Darwin loop creates a deep tree of trajectories and file changes that could lead to memory/performance issues if not properly pruned.
    *   **Signal Bleed:** Continued existence of static `getInstance()` methods in core services (`RuntimeEventBus`, `SignalBus`) creates a risk of cross-session contamination despite the `SessionContainer` architecture.
    *   **Tycho/OSGi Complexity:** The build and test environment is extremely sensitive, making automated verification difficult.
*   **Operational Maturity:** **EXPERIMENTAL/BETA**. The system shows sophisticated engineering in dimension discovery and diversity analysis, but operational stability (especially in local mode with small models) remains unproven.
*   **Recursive Cognition Status:** **OPERATIONAL**. True recursive evolution is implemented via `DarwinFlow` and `EvolutionaryTrajectoryEngine`, where selected winners inform the next generation's mutation pressure.
*   **Concurrency Status:** **PARTIALLY ISOLATED**. `SessionContext` provides good isolation for executor services and buses, but shared global state in some legacy components remains a threat.
*   **Darwin Functionality Status:** **FUNCTIONAL**. The multi-branch spawning, vector-based diversity filtering, and deterministic materialization fallbacks are all active.

## Real Runtime Architecture

### Orchestration Graph
The system follows a strict state machine managed by `EvolutionPhaseMachine`:
`INTENT_EXPANSION` → `ARCHITECTURE_VARIANTS` -> `SELECTION_REFINEMENT` → `IMPLEMENTATION_PLAN` → `FINAL_SYNTHESIS` → `TERMINAL_SUCCESS`.

### Runtime Lifecycle
1.  **Entry:** `OrchestratorServiceImpl.submit()` creates/retrieves a `SessionContext`.
2.  **Kernel Access:** `KernelFacade` routes the request to `IterationManager`.
3.  **Dimension Discovery:** `IntentExpansionEngine` identifies "unresolved semantic dimensions" (e.g., Persistence Model, Abstraction Level).
4.  **Evolutionary Loop:** `DarwinFlow` executes the multi-generation loop.
    *   `DarwinEngine` generates 4-branch blueprints.
    *   `DarwinVariantSpawner` materializes them via LLM.
    *   `DarwinDiversityAnalyzer` filters variants using `TrajectoryVector` Euclidean distance.
5.  **Execution:** Selected winner tasks are executed via `TaskExecutor` and `ToolFactory`.

### Memory Lifecycle
*   **Persistent:** `IterationMemoryService` saves `Checkpoint` objects to disk, including cognitive traces, file changes, and trajectory history.
*   **Restoration:** `IterationManager` restores full state from checkpoints upon session resumption.

### Session Ownership
*   `SessionManager` owns `SessionContainer` (implemented by `SessionContext`).
*   Each session has its own `RuntimeEventBus` and `ExecutorService`.

### Execution Pipeline
*   **Tooling:** `ITool` interface implemented by `GitTool`, `MavenTool`, `ShellTool`, etc.
*   **Retries:** `IterationManager.executeTasksWithRetries` provides deterministic recovery for failed tasks.

### UI Synchronization Flow
*   **Event-Driven:** Kernel publishes `RuntimeEvent`s to the session's `RuntimeEventBus`.
*   **Projection:** `ProjectionService` updates an immutable `RuntimeProjection`.
*   **Refresh:** `AiChatPage` observes the projection and calls `scheduleRefresh()`, which is throttled (100ms) to prevent UI storms.

## Core System Findings

### Darwin Evolution Engine
*   **Status:** **OPERATIONAL**
*   **Evidence:** `DarwinEngine.java`, `DarwinDiversityAnalyzer.java`.
*   **Runtime Behavior:** Uses 9-dimensional vector scoring to ensure technical divergence between trajectories.
*   **Risks:** High reliance on LLM structured output; handled via `DarwinVariantSpawner.autoRepair`.

### Intent & Dimension Inference
*   **Status:** **OPERATIONAL**
*   **Evidence:** `IntentExpansionEngine.java`, `DimensionInferenceEngine.java`.
*   **Runtime Behavior:** Correctly prioritizes high-level philosophies before low-level implementation details.
*   **Architectural Consistency:** Highly consistent with "Architecture Emergence Rule".

### Mediated Mode
*   **Status:** **OPERATIONAL**
*   **Evidence:** `MediatedExportManager.java`, routing in `ModeRouter.java`.
*   **Runtime Behavior:** Successfully diverts from code execution to "cognitive interpretation" and generates ZIP context packages.

### UI Projection Layer
*   **Status:** **OPERATIONAL**
*   **Evidence:** `ProjectionService.java`, `RuntimeProjection.java`.
*   **Runtime Behavior:** Provides stable, thread-safe synchronization between the background kernel and SWT UI.

### Kernel Component Integration (Mutation/Fitness/Reality/Phase)
*   **Status:** **PARTIAL**
*   **Evidence:** `DefaultMutationEngine`, `DefaultFitnessEngine`, etc. in `eu.kalafatic.evolution.controller.kernel`.
*   **Runtime Behavior:** Most "Default" implementations are thin wrappers around `DarwinEngine` or `Evaluator`. The "RealityEngine" performs Git-based delta analysis but lacks deep semantic "truth" verification.

### Evolutionary Pressure System
*   **Status:** **EXPERIMENTAL**
*   **Evidence:** `EvolutionaryPressureEngine.java`.
*   **Runtime Behavior:** Currently uses heuristic-based pressure (e.g., fitness < 0.5 => high failure exposure). Needs deeper LLM integration for "real" architectural tension discovery.

### Dead Code / Obsolete Components
*   **Status:** **DEAD CODE**
*   **Evidence:** `IterativeFlow.java` (Missing/Deleted), `EvoNavigator` 2, 3, 4 (Stale versions).

## Darwin Recursive Cognition Assessment

Darwin currently behaves as **RECURSIVE EVOLUTIONARY COGNITION**.

**Evidence:**
1.  **Lineage Preservation:** `DarwinFlow.generateProposals` retrieves the `lastWinner` from the memory service and injects it as `SURVIVING TRAJECTORY (ANCESTOR)` into the next prompt.
2.  **Negative Pressure:** `rejectedSiblings` are tracked and injected as `FORBIDDEN PHILOSOPHIES` to prevent the LLM from cycling back to failed ideas.
3.  **Pressure-Driven Mutation:** `EvolutionaryTrajectoryEngine` identifies architectural pressures (ambiguity, extensibility) and forces the next generation to address them specifically.

## Parallel Session Assessment

Concurrent sessions are **STABLE BUT AT RISK**.

*   **Isolation:** `SessionContext` correctly separates event buses and registries.
*   **Risk:** `RuntimeEventBus.getInstance()` and `SignalBus.getInstance()` still exist and are used in some components (e.g., `ProjectionService` constructor). This creates a possibility of "global leak" if a session-specific context is not explicitly passed.

## Mediated Mode Assessment

Mediated execution is **FUNCTIONAL**.

*   **Wiring:** It is fully wired from `AiChatPage` (user selection) through `ModeRouter` (flow assignment) to `MediatedExportManager` (final output).
*   **Integration:** It utilizes the same Darwin evolutionary kernel as local mode, but applies "Cognition Rules" to focus on understanding rather than code generation.

## Architectural Drift Analysis

1.  **Conflicting Architectures:** The system is in the middle of a transition from "Plain Evolutionary" (PEV) to the "Darwin Kernel". `IterationManager` contains both `runPEV()` and `runDarwinIteration()`.
2.  **Duplicated Responsibilities:** `IntentAnalyzer` and `IntentExpansionEngine` both perform intent analysis but with different schemas and purposes (Extraction vs. Search Space expansion).
3.  **Stale Abstractions:** `EvoNavigator` has four versions in the source tree, causing confusion about which one is the current source of truth for the project explorer.
4.  **Phase Hardcoding:** `EvolutionPhaseMachine` hardcodes the first 4 generations of evolution. While this ensures depth, it conflicts with the "Accelerated Atomic Convergence" goal mentioned in memory.

## Stabilization Priorities

1.  **[CRITICAL] Global Service Cleanup:** Remove or strictly deprecate `static getInstance()` on `RuntimeEventBus`, `SignalBus`, and `CapabilityRegistry` to guarantee session isolation.
2.  **[HIGH] Dead Code Removal:** Purge `EvoNavigator2/3/4` and any references to `IterativeFlow`.
3.  **[MEDIUM] Reality Check Enhancement:** Move `DefaultRealityEngine` beyond simple Git diffs to include semantic validation of structural changes.
4.  **[MEDIUM] Documentation Sync:** Update `docs/` to reflect that `DarwinFlow` has superseded `IterativeFlow`.

## Refactoring Recommendations

1.  **Unify Intent Analysis:** Merge `IntentAnalyzer` into `IntentExpansionEngine` to provide a single, robust semantic grounding phase.
2.  **Formalize Kernel Componentry:** Complete the `eu.kalafatic.evolution.controller.kernel` package by moving logic out of `IterationManager` and into the specialized engines (Mutation, Fitness, etc.).
3.  **Strengthen Session Context:** Enforce dependency injection of `SessionContainer` into all agents and tools, removing the fallback to global singletons.
4.  **Refine Stability Metrics:** Enhance `StabilityAnalyzer` to use LLM-based architectural equilibrium detection rather than just Diminishing Improvement Delta of fitness scores.
