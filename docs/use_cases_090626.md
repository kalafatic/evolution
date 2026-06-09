# System Use Cases - AI Evolution Platform (090626)

## 1. Primary Use Cases

### 1.1. Mediated Architectural Evolution
**Actor:** User (Supervisor)
**Trigger:** User enters a complex request (e.g., "Refactor the signaling layer for better resilience").
**Preconditions:** System in stable `INIT` state; Git initialized.
**Flow steps:**
1. `IterationManager` enters `ANALYZING` state.
2. `RealityDiscoveryAgent` scans code hotspots.
3. `DarwinEngine` generates 4 divergent trajectories.
4. `RuntimeEventBus` publishes `VARIANT_EVALUATED` signals.
5. UI displays competing branches for review.
6. User selects trajectory `v2`.
7. `TaskExecutor` applies the mutation.
**Modules involved:** `controller.orchestration`, `controller.engine`, `view`.
**Output:** Mutated code on a specific Git branch; updated Trajectory Record.
**Failure modes:** Blueprint collapse (LLM fails to generate JSON); recovery via `STABILIZATION_RECOVERY` blueprint.

### 1.2. Simple Task Execution (Auto-Approve)
**Actor:** User / System
**Trigger:** Arrival of a trivial coding task with `autoApprove=true`.
**Preconditions:** Session active.
**Flow steps:**
1. `ModeRouter` selects `SIMPLE_CHAT` path.
2. `PlannerAgent` creates a single-step plan.
3. `TaskExecutor` executes immediately without pausing for selection.
4. `ValidatorAgent` verifies the result.
**Modules involved:** `controller.agents`, `controller.orchestration`.
**Output:** File change committed to workspace.
**Failure modes:** Build failure; recovery via `RepairAgent`.

---

## 2. Secondary / Internal Use Cases

### 2.1. Autonomous Self-Upgrade Proposal
**Actor:** System (Genome Engine)
**Trigger:** Background trigger or user-initiated "Self-Improve" command.
**Flow steps:**
1. `selfdev-genome` analyzes its own module dependencies.
2. `TrajectoryTerritoryMapper` identifies architectural debt.
3. System generates an `UpgradePlan` in `SECONDHAND` mode.
4. Proposals are added to the `OrchestrationNavigator`.
**Modules involved:** `selfdev.genome`, `model`.
**Output:** Structured self-improvement proposal (`genome.json`).
**Failure modes:** Invariant violation detection by `RuntimeInvariant`.

### 2.2. Session Recovery (Checkpointing)
**Actor:** System (Kernel)
**Trigger:** Kernel restart or session resumption.
**Flow steps:**
1. `IterationMemoryService` loads the latest `Checkpoint` from disk.
2. `IterationManager` restores EMF state and trajectory history.
3. `RuntimeEventBus` publishes `SESSION_RESUMED`.
**Modules involved:** `controller.orchestration.selfdev`, `model`.
**Output:** System returns to previous phase (`ANALYZING`, `EXECUTING`, etc.).
**Recovery behavior:** Automatic state alignment via `IterationRecord` lineage.

---

## 3. Background Workflows
- **Memory Decay Management**: `WorkspaceAgent` periodically prunes stale semantic artifacts.
- **Address Discovery**: `NetworkDiscoveryService` scans for new network entry points.
- **Model Reconciler**: `GitEmfReconciler` ensures the EMF model and disk state remain synchronized.
