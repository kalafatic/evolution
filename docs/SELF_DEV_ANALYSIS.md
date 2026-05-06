# Evolution Project: Self-Development Functionality Analysis

## 1. Overview
The self-development functionality allows the Evolution system to autonomously improve its own codebase. It operates in iterative cycles, leveraging either a straightforward task-based refinement or a Darwinian evolutionary approach with parallel variant evaluation.

---

## 2. Core Workflow & Main Entry Points

### 2.1 Entry Points
*   **UI (Eclipse RCP):** Triggered via the "🚀 Self-Dev" button in `AiChatPage`. This initializes a `SelfDevSession` and handoff to the `SelfDevSupervisor`.
*   **CLI (Standalone):** `eu.kalafatic.evolution.controller.orchestration.SelfDevMain`. Invoked by an external supervisor JAR, typically used for long-running autonomous improvements that survive IDE restarts.

### 2.2 Standard Workflow
1.  **Session Initiation:** `SelfDevSupervisor` coordinates the session lifecycle (start, stop, iteration loop).
2.  **Iteration Manager Handoff:** For each iteration, the Supervisor delegates execution to the `IterationManager` (The Kernel Control Plane).
3.  **Strategic Branching:**
    *   **Darwin Mode:** Creates a snapshot branch, generates multiple `BranchVariant` proposals via `DarwinEngine`, and evaluates them in parallel worktrees.
    *   **Iterative Mode:** Decomposes the goal into a sequence of atomic `Task` objects via `TaskPlanner`.
4.  **Task Execution (PEV Loop):**
    *   **PLAN:** Tactical plan generation.
    *   **EXECUTE:** Blind execution via `EvolutionOrchestrator`.
    *   **VERIFY:** Result validation via `ValidatorAgent`.
5.  **Evaluation & Decision:** `Evaluator` runs builds/tests to compute a success score. The Kernel then decides whether to `COMMIT`, `ROLLBACK`, or `STOP`.

---

## 3. Adaptation to New Kernel Architecture

The self-development process has been adapted to follow the **Deterministic State-Transition Evolutionary Kernel** architecture as defined in `ARCHITECTURE_GUIDELINES.MD`.

### 3.1 Architectural Invariants Applied
*   **Single Transition Authority:** `SelfDevSupervisor` no longer manages strategic state. All transitions (e.g., `MUTATING`, `EXECUTING`, `VERIFYING`) are explicitly handled by the `IterationManager`.
*   **No Bypass Paths:** The `SelfDevMain` entry point has been refactored to route all requests through `KernelFacade` instead of calling the `EvolutionOrchestrator` directly.
*   **Stateless Execution:** The `EvolutionOrchestrator` remains a "blind executor." Specific self-development logic (like variant merging) is kept within the Control Plane (`IterationManager`).
*   **Intelligence Isolation:** `DarwinEngine` is strictly a proposal generator. Branch management and worktree lifecycle are now fully controlled by the Kernel.

### 3.2 State Machine Mapping
The autonomous loop now maps directly to the system states:
-   **Variant Generation:** `MUTATING`
-   **Parallel Evaluation:** `EXECUTING` (sub-instances)
-   **Verification:** `VERIFYING`
-   **Finalization:** `DONE` or `FAILED`

---

## 4. Key Components Analysis
*   **`SelfDevSupervisor`**: Session coordinator; manages iteration count and handoff to external bootstrappers.
*   **`IterationManager`**: The "Brain"; sole authority for moving the system through its evolutionary states.
*   **`DarwinEngine`**: Intelligence component; reasons over repository history and current snapshot to propose improvements.
*   **`KernelFacade`**: Unified API; ensures all external triggers (UI or CLI) respect the Kernel's state machine.
