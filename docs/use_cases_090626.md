# System Use Cases - AI Evolution Platform (090626)

## 1. Primary Use Cases

### 1.1. Mediated Architectural Evolution
- **Actor:** Human Supervisor
- **Trigger:** Complex refactoring or feature intent provided via `AiChatPage`.
- **Preconditions:** Active `SelfDevSession`; Git branch established.
- **Flow steps:**
    1. `IterationManager` triggers `RealityDiscoveryAgent` to build `TargetRealityModel`.
    2. `DarwinEngine` spawns 4-6 `BranchVariant` objects via `TrajectoryTerritoryMapper`.
    3. `RuntimeEventBus` publishes `VARIANT_EVALUATED` (AGENT category).
    4. UI (DevelopmentPage) renders divergent branches with "Predicted Score".
    5. User provides fuzzy selection (e.g., "Select 1.1") via `DecisionResolver`.
    6. `TaskExecutor` applies patch; `FileChangeTracker` marks files as `WRITE`.
    7. `ValidatorAgent` executes `mvn integration-test`.
- **Modules involved:** `controller.orchestration`, `controller.engine`, `view.editors`.
- **Output/Result:** Mutated codebase on `evo-*` branch; persistent trajectory record.
- **Failure modes:** Blueprint collapse (empty variants); recovery via `STABILIZATION_RECOVERY` fallback.

### 1.2. Simple Atomic Task
- **Actor:** System / User
- **Trigger:** Straightforward intent (e.g., "Create class X").
- **Preconditions:** `REASONING_ATOMIC` behavior trait present in session.
- **Flow steps:**
    1. `ModeRouter` selects `SIMPLE_CHAT` path.
    2. `PlannerAgent` identifies target artifact.
    3. `TaskExecutor` executes direct `WRITE` operation.
    4. `ValidatorAgent` performs semantic validation.
- **Modules involved:** `controller.agents`, `controller.tools`.
- **Output/Result:** New file created in workspace.
- **Failure modes:** File collision; recovery via `RepairAgent` auto-skeleton.

---

## 2. Secondary & Background Workflows

### 2.1. Autonomous Genome Upgrade
- **Actor:** System (`selfdev-genome`)
- **Trigger:** Discovery of architectural debt in core kernel modules.
- **Preconditions:** System in `MEDIATED` or `SELF_DEV` mode.
- **Flow steps:**
    1. `selfdev-genome` analyzes its own module dependencies.
    2. `TrajectoryTerritoryMapper` identifies architectural debt.
    3. System generates an `UpgradePlan` (FileChange + ArchitecturalChange).
    4. Proposals are registered in `CapabilityRegistry` as `SECONDHAND` mutations.
    5. Supervisor reviews via `OrchestrationNavigator`.
- **Modules involved:** `selfdev.genome`, `controller.orchestration.capability`.
- **Output/Result:** Structural improvement proposal for the kernel itself.
- **Failure modes:** Invariant violation; handled by `RuntimeInvariant`.

### 2.2. Workspace Memory Decay
- **Actor:** System (`SemanticWorkspace`)
- **Trigger:** Completion of an evolution iteration.
- **Flow steps:**
    1. `SemanticWorkspace.applyDecay()` called.
    2. Stale artifacts (DecayScore < 0.1) are pruned.
    3. `RuntimeEventBus` publishes `MEMORY_DECAY_APPLIED`.
- **Modules involved:** `controller.orchestration.workspace`.
- **Output/Result:** Clean reasoning context for the next generation.

---

## 3. Failure & Recovery Behavior

### 3.1. Capability Unavailability
- **Trigger:** Orchestrator attempts to access a contract not present in `CapabilityRegistry`.
- **Flow:**
    1. `IterationManager` logs capability registration error.
    2. System falls back to `KernelScheduler` or default implementations.
- **Result:** Degraded but functional execution.

### 3.2. Evolution State Desync
- **Trigger:** Inconsistency between Git branch state and EMF model.
- **Flow:**
    1. `GitEmfReconciler` detects drift.
    2. `IterationMemoryService` loads last known `Checkpoint`.
    3. State is reset to last stable phase.
- **Result:** Trajectory integrity preserved.
