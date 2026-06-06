# ECOS_TRANSITION_PLAN.md

## Overview
Phased plan for transitioning the EVO platform to the full ECOS operational mode.

---

## Phase 0: STABILIZATION
- **Objective**: Eliminate dead code and clarify current component boundaries.
- **Required Changes**:
    - Remove `ExampleMain.java`, `SelfDevMain.java`.
    - Formalize `NeuronAI` as an external optional plugin or remove simulation code.
    - Consolidate redundant scanning logic (`TargetScanner` vs `MetadataAgent`).
- **Validation Criteria**: All unit tests pass; core evolutionary loop remains functional.
- **Risk**: LOW.

## Phase 1: ECOS CORE ALIGNMENT (Kernel Generalization)
- **Objective**: Decouple the kernel from artifact-specific logic.
- **Required Changes**:
    - Introduce `IArtifact` and `IEvolutionEnvironment` interfaces.
    - Move Git and Maven specific logic from `IterationManager` and `DarwinFlow` to `DevelopmentEnvironment`.
    - Move file-system specific logic to `FileArtifactAdapter`.
- **Validation Criteria**: Kernel compiles without references to Git or Maven.
- **Risk**: HIGH (Requires significant refactoring of `IterationManager`).

## Phase 2: COGNITION ALIGNMENT (Agent Isolation)
- **Objective**: Ensure all agents are stateless and Kernel-driven.
- **Required Changes**:
    - Audit all agents for internal state awareness.
    - Enforce Kernel-only orchestration in `DarwinFlow`.
- **Validation Criteria**: Agents can be instantiated and executed in isolation without shared session state.
- **Risk**: MEDIUM.

## Phase 3: DARWIN EVOLUTION ALIGNMENT (Unified Spawning)
- **Objective**: Normalize the mutation generation process across all environments.
- **Required Changes**:
    - Unify blueprint generation logic in `DarwinEngine`.
    - Ensure all environments provide consistent `TrajectoryBlueprint` inputs.
- **Validation Criteria**: Diversified trajectories are generated for both code and mediation artifacts using the same spawner.
- **Risk**: MEDIUM.

## Phase 4: MEDIATED MODE ALIGNMENT (Environment Strategy)
- **Objective**: Refactor Mediated Mode as a first-class ECOS Environment.
- **Required Changes**:
    - Implement `MediationEnvironment`.
    - Move `performMediatedExportConvergence` and related logic to the new environment.
    - Remove mediated-specific conditionals from `IterationManager`.
- **Validation Criteria**: Mediated mode functional using the unified kernel loop.
- **Risk**: MEDIUM.

## Phase 5: SUPERVISOR ALIGNMENT (External Coordination)
- **Objective**: Coordinate external supervision with internal evolutionary state.
- **Required Changes**:
    - Link `eu.kalafatic.evolution.supervisor` events to the core `RuntimeEventBus`.
    - Ensure supervisor respects the `IterationManager` terminal states.
- **Validation Criteria**: Supervisor correctly restarts or rolls back based on Kernel terminal signals.
- **Risk**: LOW.

## Phase 6: LINEAGE SYSTEM ALIGNMENT (Primacy of Persistence)
- **Objective**: Shift from branch-centric to lineage-centric persistence.
- **Required Changes**:
    - Refactor `IterationMemoryService` to prioritize `Trajectory` and mutation history.
    - Update `Checkpoint` schema to be artifact-agnostic.
- **Validation Criteria**: Evolutionary continuity is preserved across restarts using lineage identifiers rather than branch names.
- **Risk**: HIGH (Data migration risk for existing checkpoints).

## Phase 7: FULL ECOS OPERATIONAL MODE
- **Objective**: Complete alignment and enable pressure-centric autonomous evolution.
- **Required Changes**:
    - Enable `Pressure-Centric Mutation Planning` as the default.
    - Final audit of ECOS constraint compliance.
- **Validation Criteria**: Platform can evolve its own architecture based on architectural pressure signals with zero human-in-the-loop (in autonomous mode).
- **Risk**: MEDIUM.

---

## Rollback Conditions (Global)
- Failure of core unit tests.
- Deadlock in evolutionary loops.
- Inconsistent checkpoint state preventing recovery.
- Regression in mediation package quality.

## Completion Criteria (Global)
- 100% compliance with ECOS Vision Constraints.
- Zero artifact-specific logic in `eu.kalafatic.evolution.controller.orchestration`.
- All modes operating as `EvolutionEnvironment` strategies.
