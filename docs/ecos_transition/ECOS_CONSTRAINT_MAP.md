# ECOS_CONSTRAINT_MAP.md

## Overview
This document maps ECOS vision constraints to enforceable architectural rules and affected modules.

---

## 1. Architecture Constraints

### Rule: Single Authority
- **Constraint**: ONLY `IterationManager` may change system state.
- **Enforcement**: Mandatory code reviews and static analysis (future).
- **Affected Modules**: `eu.kalafatic.evolution.controller` (all agent-based packages).
- **Blockers**: None, currently enforced by policy and `SessionBoundaryGuard`.

### Rule: Mode Isolation
- **Constraint**: Modes represent environments, not independent logic.
- **Enforcement**: Forbid `if(mode == X)` in Kernel.
- **Affected Modules**: `IterationManager`, `DarwinFlow`.
- **Blockers**: Significant mode-specific conditionals currently exist. Requires refactoring to `EvolutionEnvironment` strategy.

---

## 2. Evolution Constraints

### Rule: Lineage Primacy
- **Constraint**: Surviving lineages are the primary persistent cognitive structure.
- **Enforcement**: Artifacts must be linked to an ancestor in the lineage.
- **Affected Modules**: `eu.kalafatic.evolution.controller.trajectory`, `eu.kalafatic.evolution.controller.orchestration.selfdev`.
- **Blockers**: Current persistence is branch-centric.

### Rule: Artifact Agnosticism
- **Constraint**: Kernel must evolve artifacts, not implementations (Java, Git, etc.).
- **Enforcement**: Abstract artifact interfaces.
- **Affected Modules**: `IterationManager`, `DarwinEngine`.
- **Blockers**: Kernel has hardcoded dependencies on Git and Maven.

---

## 3. Cognition Constraints

### Rule: Pressure-Driven Mutation
- **Constraint**: Mutations must emerge from pressure analysis.
- **Enforcement**: Mutation planning (blueprints) must cite the pressure being resolved.
- **Affected Modules**: `DarwinEngine`, `IntentExpansionEngine`.
- **Blockers**: Pressure is currently used as a weight but not the primary generative prompt in all cases.

---

## 4. Orchestration Constraints

### Rule: Deterministic Transitions
- **Constraint**: All behavior must be explainable as STATE → TRANSITION → STATE.
- **Enforcement**: `CognitiveTrace` must record all transitions.
- **Affected Modules**: `IterationManager`.
- **Blockers**: None, currently functional.

---

## 5. Constraint Enforcement Lifecycle

| Phase | Enforcement Mechanism |
| :--- | :--- |
| **Mutation** | Prompt injection: "Cite resolved pressure." |
| **Selection** | Evaluator check: "Does this violate core invariants?" |
| **Transition** | `RuntimeInvariant` check during `applyTransition`. |
| **Persistence** | Schema enforcement in `IterationMemoryService`. |

---

## BLOCKERS

### B1: Kernel Coupling to Infrastructure (Git/Maven)
- **Status**: BLOCKER for Artifact Agnosticism.
- **Resolution**: Move Git/Maven logic to `DevelopmentEnvironment` strategy.

### B2: Mode Conditional Debt
- **Status**: BLOCKER for Mode Alignment.
- **Resolution**: Implement `EvolutionEnvironment` interface and refactor `IterationManager`.

### B3: Branch-Centric Persistence
- **Status**: BLOCKER for Lineage Primacy.
- **Resolution**: Refactor `IterationRecord` and `Checkpoint` to prioritize `Trajectory` over `BranchName`.
