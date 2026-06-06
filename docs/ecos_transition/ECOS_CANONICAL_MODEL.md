# ECOS_CANONICAL_MODEL.md

## Overview
This document defines the unified ECOS model, normalizing existing components into a consistent evolutionary framework.

## 1. Unified Evolutionary Primitives

### Artifact
- **Definition**: The fundamental unit of evolution.
- **Normalized Entities**:
    - `CodeArtifact` (Current: Workspace files)
    - `MediationArtifact` (Current: MediationPackage/ZIP)
    - `CognitiveArtifact` (Current: Prompt, Strategy, ReasoningFocus)
- **Ownership**: The Evolution Kernel manages Artifacts through abstraction.

### Mutation
- **Definition**: A transformation applied to an Artifact to produce a descendant.
- **Normalized Entities**:
    - `StrategicMutation` (Current: BranchVariant strategy)
    - `ImplementationMutation` (Current: Task response/patch)
- **Ownership**: `MutationEngine` / `DarwinEngine`.

### Evaluation
- **Definition**: A process that produces signals and fitness scores for an Artifact.
- **Normalized Entities**:
    - `ArchitecturalSignal` (Current: Stability, Pressure)
    - `OperationalSignal` (Current: Test pass rate, Build status)
- **Ownership**: `Evaluator` / `StabilityAnalyzer`.

### Selection
- **Definition**: The decision process that identifies survivors.
- **Normalized Entities**:
    - `EvolutionarySelection` (Current: Darwin variant selection)
- **Ownership**: `AuthorityEngine` (via `IterationManager`).

### Lineage
- **Definition**: The persistent history of surviving Artifacts.
- **Normalized Entities**:
    - `SurvivingLineage` (Current: Trajectory, IterationRecord)
- **Ownership**: `TrajectoryEngine` / `IterationMemoryService`.

### Pressure
- **Definition**: The forces driving the direction of evolution.
- **Normalized Entities**:
    - `ArchitecturalPressure` (Current: EvolutionaryPressureVector)
- **Ownership**: `EvolutionaryPressureEngine`.

---

## 2. Normalized Module Boundaries

### Evolution Kernel (Core)
- **Components**: `IterationManager`, `PhaseEngine`, `MutationEngine`, `FitnessEngine`, `AuthorityEngine`, `TrajectoryEngine`, `EvolutionaryPressureEngine`.
- **Role**: Deterministic orchestration of evolutionary cycles.
- **Constraint**: Must remain Artifact-agnostic.

### Evolution Environments (Modes)
- **Environments**:
    - `MediationEnvironment` (Current: Mediated Mode)
    - `DevelopmentEnvironment` (Current: Self-Dev Mode)
    - `SimulationEnvironment` (Current: Test Mode)
- **Role**: Provide specialized artifact types, evaluators, and executors to the Kernel.

### Evolution Agents (Cognition)
- **Components**: `DarwinEngine`, `IntentExpansionEngine`, specialized agents (`Analytic`, `Planner`, etc.)
- **Role**: Materialize mutations and analyze artifacts upon Kernel request.
- **Constraint**: Must remain stateless and unaware of global control flow.

---

## 3. Normalized Execution Lifecycle

1. **Initialization**: Kernel initializes an `EvolutionaryTrajectory`.
2. **Pressure Discovery**: `EvolutionaryPressureEngine` identifies current forces.
3. **Intent Grounding**: `IntentExpansionEngine` defines the search space (Dimensions).
4. **Mutation Spawning**: `MutationEngine` requests variants from `DarwinEngine` based on Blueprints.
5. **Multi-Branch Evaluation**: `FitnessEngine` aggregates signals for all variants.
6. **Survivor Selection**: `AuthorityEngine` selects the winning trajectory.
7. **Lineage Update**: `TrajectoryEngine` persists the survivor and its mutation history.
8. **Convergence Check**: `StabilityAnalyzer` determines if equilibrium is reached.

---

## 4. Normalized Vocabulary
- **AVOID**: "Branch", "Proposal", "Mode", "Code", "Git".
- **PREFER**: "Trajectory", "Candidate", "Environment", "Artifact", "Lineage".

---

## 5. Model Integrity Rules
- No environment-specific logic in the Kernel.
- No direct agent-to-agent communication; all flows go through the Kernel.
- Every state transition must be recorded in the `CognitiveTrace`.
- The `SurvivingLineage` is the single source of truth for cognitive continuity.
