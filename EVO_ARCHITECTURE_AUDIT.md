# EVO Architecture Audit

## Executive Summary

The EVO platform is a sophisticated evolutionary operating system currently in a **late-beta operational state**. While the core concepts of recursive evolutionary cognition and trajectory-driven engineering are implemented and reachable, the platform exhibits significant **architectural drift** and **stabilization risks** particularly in multi-session isolation and noisy LLM response handling.

*   **Overall platform stability:** FRAGILE. Core flows work for standard cases but fail on non-deterministic LLM output or complex environment setups.
*   **Major architectural risks:** State leakage between parallel sessions due to singleton misuse; tight coupling between UI and kernel; high sensitivity to LLM JSON formatting.
*   **Operational maturity:** PARTIAL. The system can execute end-to-end tasks but requires frequent manual intervention (MANUAL mode is the default and most stable).
*   **Recursive cognition status:** OPERATIONAL but shallow. The system identifies dimensions and branches, but recursive depth is often limited by convergence heuristics or model capability.
*   **Concurrency status:** UNSAFE. Parallel sessions are partially isolated but share global buses and registries.
*   **Darwin functionality status:** OPERATIONAL. Multi-branch spawning, diversity filtering, and winner execution are functional.

## Real Runtime Architecture

The ACTUAL execution flow of the EVO platform follows a unified evolutionary mandate:

1.  **Entry:** `OrchestratorServiceImpl` receives a `TaskRequest`.
2.  **Session Context:** A `SessionContext` is created/retrieved, providing an isolated `ExecutorService` and `CapabilityRegistry`.
3.  **Kernel Initiation:** `IterationManager` (the single authority) takes control.
4.  **Discovery:** `StructureAgent` and `TargetScanner` build a repository-grounded snapshot.
5.  **Intent Expansion:** `IntentExpansionEngine` discovers `EvolutionDimension`s (decision points).
6.  **Evolutionary Loop:** `IterationManager` invokes `DarwinFlow` recursively.
    *   **Spawning:** `DarwinEngine` materializes `TrajectoryBlueprint`s into `BranchVariant`s.
    *   **Diversity:** `DarwinDiversityAnalyzer` filters variants using vector distance.
    *   **Selection:** The system pauses for manual user selection (default) or uses `AuthorityController` for auto-selection.
    *   **Execution:** `DarwinFlow.executeWinner` applies the winner's changes (via worktrees or direct writes) and runs `Evaluator`.
7.  **Convergence:** `DarwinFlow` checks for stability or minimal delta to break the recursion.
8.  **Finalization:** `FinalResponseAssembler` generates the report; `MediatedExportManager` packages results in mediated mode.

### Lifecycle & State Management
*   **Single Transition Authority:** `IterationManager` successfully enforces state transitions via `TransitionToken`.
*   **Memory Lifecycle:** `IterationMemoryService` persists state to `orchestrator/memory` and `iterations/`. It is refreshed on initialization but relies on disk I/O which can lead to race conditions in high-frequency operations.

## Core System Findings

| Subsystem | Status | Evidence | Runtime Behavior | Risks |
| :--- | :--- | :--- | :--- | :--- |
| **Orchestration Control Plane** | OPERATIONAL | `IterationManager.java` | AUTHORITATIVE. Correctly manages transitions from INIT to DONE/FAILED. | Logic duplication in `DarwinFlow` and `IterationManager` (partially refactored). |
| **Darwin Engine** | OPERATIONAL | `DarwinEngine.java`, `DarwinFlow.java` | Technical trajectories are spawned based on blueprints. Vector diversity is enforced. | LLM non-determinism can lead to 0 variants surviving diversity checks. |
| **Intent Expansion** | FRAGILE | `IntentExpansionEngine.java` | Discovers dimensions and axes. | Highly dependent on LLM JSON precision. Fails if LLM echoes templates. |
| **Memory/Lineage** | OPERATIONAL | `IterationMemoryService.java`, `EvolutionMemoryGraph.java` | Lineage is preserved across iterations. Checkpoints are functional. | Disk-based persistence may cause performance bottlenecks or sync issues. |
| **Resume Logic** | FRAGILE | `IterationManager.java` (Constructor) | Restores Phase and Goal from checkpoints. | **CRITICAL:** `FileChangeTracker` state is NOT persisted; "no files" are shown in the UI changes panel after a resume even if physical changes exist. |
| **Execution Pipeline** | PARTIAL | `TaskExecutor.java`, `ShellTool.java`, `MavenTool.java` | Executes shell commands and Maven builds. | `MavenTool` is a simple wrapper over `ShellTool`. Error parsing is regex-based and fragile. |
| **Mediated Mode** | OPERATIONAL | `MediatedExportManager.java`, `DarwinFlow.executeWinner` | Cognitive merges work; ZIP export is functional. | Bypasses physical evaluation entirely; relies purely on "cognitive understanding". |
| **UI (SWT/RCP)** | FRAGILE | `AiChatPage.java`, `chat.html` | Complex listener-based sync. Reflects kernel state via `RuntimeEventBus`. | UI thread blocking; refresh storms; state desync if events are missed. |
| **Session Isolation** | PARTIAL | `SessionContext.java` | Isolated executors and registries exist. | State leakage via `RuntimeEventBus.getInstance()` and `SignalBus.getInstance()`. |

## Darwin Recursive Cognition Assessment

**Status: Functional Recursive Evolution** (with caveats)

Evidence:
*   `IterationManager.evolve` contains a `while` loop that recursively calls `runDarwinIteration` until all dimensions are resolved or safety limits are hit.
*   `DarwinEngine` retrieves the previous iteration's winner as `lineageContext` to ensure continuity.
*   `EvolutionMemoryGraph` tracks the decision hierarchy.

However, it often behaves as a **Static Proposal Engine** when:
1.  Ambiguity scores are low, leading to immediate "Deterministic Execution".
2.  The model fails to identify new dimensions in the 2nd generation, causing premature convergence.

## Parallel Session Assessment

**Status: UNSAFE / Partially Isolated**

Concurrent sessions are a "best-effort" architecture rather than first-class:
*   `SessionContext` isolates the thread pool and `CapabilityRegistry`.
*   **CRITICAL FAILURE:** `RuntimeEventBus.getInstance()` is used globally in many agents and tools. Events from Session A are routinely broadcast to Session B's UI components.
*   **CRITICAL FAILURE:** `SignalBus.getInstance()` and `WorkflowStepRegistry.getInstance()` are global singletons, leading to signal bleed and step collisions.

## Mediated Mode Assessment

**Status: OPERATIONAL**

Mediated mode is correctly wired:
1.  `ModeRouter` routes to `DarwinFlow`.
2.  `DarwinFlow` detects `BehaviorTrait.SUPERVISION_MEDIATED`.
3.  `executeWinner` performs a "Cognitive Merge" (updating metadata like `current_understanding`) instead of applying physical file changes.
4.  `IterationManager.performMediatedExportConvergence` successfully triggers `MediatedExportManager`.

## Architectural Drift Analysis

1.  **Orchestration Redundancy:** Logic for "what to do next" is split between `IterationManager`, `DarwinFlow`, and `EvolutionPhaseMachine`.
2.  **Singleton Mismatch:** Half the system uses `SessionContext` isolation; the other half uses `static getInstance()` (e.g., `SignalBus`, `RuntimeEventBus`).
3.  **Tool Fragility:** `MavenTool` and `GitTool` are thin wrappers over `ShellTool` but carry significant complex logic for output parsing that should be in specialized `Evaluator` components.
4.  **UI/Model Coupling:** `AiChatPage` directly manipulates `Orchestrator` EMF models and `SessionState` objects simultaneously.

## Stabilization Priorities

1.  **Session Isolation (High):** Refactor `RuntimeEventBus` and `SignalBus` to be session-scoped.
2.  **Resume Continuity (High):** Persist `FileChangeTracker` state to allow the UI to reflect changes after a session resume ("no files-resume" bug).
3.  **LLM Robustness (High):** Implement robust JSON repair and schema validation in `IntentExpansionEngine` to prevent "Failed to parse intent" errors.
4.  **UI Stability (Medium):** Throttle UI updates and ensure `!isDisposed()` checks are universal in `asyncExec` blocks.
5.  **Error Handling (Medium):** Move complex regex parsing from `Evaluator` and `ShellTool` into dedicated, unit-tested `ResponseParser` components.

## Refactoring Recommendations

1.  **Unify Eventing:** Pass the `SessionContext` or `RuntimeEventBus` instance through the `TaskContext` to all tools and agents. Remove all calls to `RuntimeEventBus.getInstance()` in non-global contexts.
2.  **Schema Hardening:** Use `DarwinVariantValidator` patterns in `IntentExpansionEngine`.
3.  **Phase Decoupling:** Move all phase transition logic EXCLUSIVELY into `EvolutionPhaseMachine` and let `IterationManager` be the pure executor of those transitions.
4.  **Agent Statelessness:** Ensure agents in `AgentFactory` do not hold session-specific state.
