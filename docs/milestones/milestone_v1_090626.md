# Milestone Freezepoint v1 - 090626 (DARWIN GENOME)

## 🧬 System State Summary
The system is currently at version **2.6.5-SNAPSHOT**. It functions as a production-capable **AI Evolution Platform** that coordinates divergent coding trajectories. It effectively manages session-isolated evolutionary loops grounded in repository reality.

## 🧱 Core Invariants (DO NOT BREAK)
1.  **Single Transition Authority**: Only `IterationManager` may mutate system state (`SystemState`).
2.  **Darwinian Divergence**: Mutation cycles MUST generate at least 4 unique conceptual trajectories.
3.  **Thread-Session Integrity**: Access to session resources must pass through `RuntimeInvariant.checkSession`.
4.  **No Global Drift**: Hidden singleton access within active sessions is prohibited and reported.

## 📊 Stability Classification
- **Stable**: `IterationManager`, `OrchestratorServiceImpl`, `EMF Core Model`, `RuntimeEventBus`.
- **Semi-Stable**: `DarwinEngine`, `TrajectoryTerritoryMapper`, `ContextCurator`.
- **Experimental**: `selfdev-genome` runtime, `NeuronAI` evaluators, `IntentExpansionEngine`.

## ❄️ Darwin Freeze Boundaries
- **SAFE MUTATION ZONES**:
    - Agent prompt templates (`/src/eu/kalafatic/evolution/controller/agents/`).
    - Evaluation fitness heuristics.
    - Diversity vector weights.
- **UNSAFE MUTATION ZONES**:
    - `IterationManager` state machine transitions.
    - `RuntimeEventBus` category definitions.
    - `SessionBoundaryGuard` lifecycle management.
- **PROHIBITED CHANGE AREAS**:
    - EMF Package `orchestration` structure (requires full model regeneration).

## 🛠 External Self-Dev Supervisor Integration
The following hooks are provided for the external Supervisor:
- **Hook Points**:
    - `TASK_STARTED` / `TASK_COMPLETED` (Execution tracking).
    - `VARIANT_EVALUATED` (Decision point).
- **Validation Gates**:
    - `mvn integration-test` (Build integrity).
    - `DarwinDiversityAnalyzer` (Conceptual integrity).
- **Rollback Conditions**:
    - `InvariantViolationException` detection.
    - Consecutive task failures (> 3).
- **Safe Deployment Boundaries**:
    - Mutations must be committed to a non-main branch (`evo-*`).

## 🧬 Evolution Strategy Notes
- **Recommended Next Direction**: Stabilize `IntentExpansionEngine` to reduce semantic ambiguity before spawning.
- **Forbidden Evolution Areas**: Do not move towards multi-threaded shared state; maintain the "Single-Thread-per-Session" model.
- **High-Value Mutation Targets**: Context curation logic (`ContextCurator`) to improve signal-to-noise ratio in small local models.
