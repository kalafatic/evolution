# Milestone Freezepoint v1 - 090626 (DARWIN GENOME)

## 1. System State Summary (TRUTH ONLY)
The system is at version **2.6.5-SNAPSHOT**. It is a functional **Evolutionary AI Kernel** capable of:
- Multi-trajectory spawning and vector-based diversity analysis.
- Mediated selection of architectural variants.
- Repository-grounded intent expansion and context curation.
- Atomic side-effect application via Git.
- Session isolation using thread-local guards.

**Implemented Features:** PEV Loop, Runtime Event Bus, Capability Registry, Darwin Engine, Semantic Workspace.
**Partial/Experimental:** `selfdev-genome` upgrade plans, `NeuronAI` signals, advanced auto-repair skeletons.

## 2. Core Execution Model
- **Entry Points:** `OrchestratorServiceImpl`, `IterationManager`.
- **Runtime Flow:** `INIT` -> `ANALYZING` -> `(CLARIFYING)` -> `EXECUTING` -> `VERIFYING` -> `DONE`.
- **Control Structure:** Centralized authority machine in `IterationManager`.
- **System Heartbeat:** The recursive iteration loop in `IterationManager.evolve()`.

## 3. Stability Classification Map
- 🟢 **Stable Core (DO NOT TOUCH):**
    - `eu.kalafatic.evolution.model` (EMF Package)
    - `eu.kalafatic.evolution.controller.kernel` (Invariants)
    - `eu.kalafatic.evolution.controller.workflow` (Event Bus)
- 🟡 **Controlled Evolution Zone (Safe Mutation):**
    - `eu.kalafatic.evolution.controller.engine` (Darwin logic)
    - `eu.kalafatic.evolution.controller.agents` (AI Roles)
    - `eu.kalafatic.evolution.controller.mediation` (Curation)
- 🔴 **Experimental Zone (Free Mutation):**
    - `eu.kalafatic.evolution.selfdev.genome`
    - `eu.kalafatic.evolution.controller.orchestration.intent`

## 4. Core Invariants (ABSOLUTE RULES)
- Only `IterationManager` transitions the global state.
- Every entry point must be session-guarded via `SessionBoundaryGuard`.
- All kernel signals must be published via `RuntimeEventBus` with correct category.
- Minimum 4-branch divergence mandate for the `DarwinEngine`.

## 5. Darwin Mutation Boundaries
- **Safe Mutation Zones:** Agent prompt templates, fitness heuristics, vector weights.
- **Unsafe Mutation Zones:** PEV loop sequence, event bus categories, thread-local management.
- **Forbidden Structural Changes:** EMF orchestration schema changes without synchronized regeneration.
- **High-Risk Coupling:** Interaction between OSGi bundles and JGit transactions.

## 6. Fitness & Selection Model
- **Correctness:** Evaluated via `mvn integration-test` (build integrity).
- **Successful Evolution:** A trajectory that passes verification AND maintains conceptual distance (Diversity > 0.1).
- **Regression Criteria:** Detection of `InvariantViolationException` or drop in test pass rate.

## 7. Mediated Model Summary
- **System Conceptual Model:** A state-controlled mutation kernel.
- **Core Abstractions:** Evolutionary Trajectory, Semantic Dimension, Target Reality Model.
- **Behavioral Flows:** Intent Expansion -> Darwin Branching -> Validated Convergence.

## 8. External Self-Dev Supervisor Contract
- **Build Triggers:** Published `TASK_STARTED` events.
- **Test Execution Gates:** `ValidatorAgent` calls to `mvn integration-test`.
- **Rollback Conditions:** `InvariantViolationException`, consecutive failures, or user rejection in mediated mode.
- **Deployment Constraints:** Mutations must remain on isolated `evo-*` branches.
- **Validation Checkpoints:** `VARIANT_EVALUATED` (Pre-selection), `FLOW_COMPLETED` (Post-execution).

## 9. Evolution Strategy (NEXT STEP ONLY)
- **SHOULD Evolve:** `ContextCurator` token-budget optimization for larger snapshots.
- **MUST NOT Evolve:** Global static state for session resources.
- **High Value Target:** `TrajectoryTerritoryMapper` Bulk Discovery efficiency.
- **Recommended Focus:** Stabilize `IntentExpansionEngine` to reduce semantic noise.
