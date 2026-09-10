# Operations Lifecycle and State Authority

**Document Identifier**: `docs/architecture/lifecycle.md`
**Date**: September 9, 2026

---

## 1. Unified Operational State Machine

All long-running execution runs across EVO conform to a standardized lifecycle state machine:

```text
       ┌──────────┐
       │ CREATED  │
       └────┬─────┘
            │
            ▼
       ┌──────────┐
       │ANALYZING │
       └────┬─────┘
            │
            ▼
       ┌──────────┐
       │ PLANNING │
       └────┬─────┘
            │
            ▼
       ┌──────────┐
       │PREPARING │
       └────┬─────┘
            │
            ▼
       ┌──────────┐
       │  READY   │
       └────┬─────┘
            │
            ▼
       ┌──────────┐
       │ RUNNING  │
       └────┬─────┘
            │
            ▼
       ┌──────────┐
       │EVALUATING│
       └────┬─────┘
            │
            ▼
       ┌──────────┐
       │FINALIZING│
       └────┬─────┘
            │
            ▼
       ┌──────────┐
       │VALIDATING│
       └────┬─────┘
            │
      ┌─────┴─────┐
      ▼           ▼
┌───────────┐ ┌──────────┐
│ COMPLETED │ │  FAILED  │
└───────────┘ └──────────┘
```

---

## 2. Subsystem Lifecycle Authority Map

To satisfy **Invariant I1 (Single State Authority)**, every operation has exactly one authoritative lifecycle owner:

| Subsystem / Capability | State Transition Authority | Owned State Objects | Execution / Worker Engine |
| :--- | :--- | :--- | :--- |
| **Chat / Inference** | `OrchestratorServiceImpl` | `Session`, `TaskContext` | `OllamaProvider` / `ReferenceEvoInferenceEngine` |
| **Darwin Evolution** | `IterationManager` | `IterationState`, `Trajectory` | `ADarwinEngine` |
| **Forge Orchestration** | `ForgeJob` (`eu.kalafatic.evolution.forge.controller.api.ForgeJob`) | `ForgeJob` state machine | `ForgeOrchestratorImpl` |
| **Dataset Preparation** | `DatasetAcquisitionEngine` | `DatasetSourceStats`, `.evodata` | `DatasetAcquisitionEngine` / `DataCleaner` |
| **Model Training** | `EvoLlmTrainer` | `TrainingState`, `AdamW` weights | `EvoLlmTrainer` / `EvoLlmModel` |
| **Model Export** | `OllamaExporter` / `SnapshotControllerImpl` | Export Snapshot metadata | `OllamaExporter` / `GGUFValidator` |
| **Self-Dev Execution** | `SelfDevSupervisor` | `state.json`, Task state | `ProcessRunner` / `EVOSupervisorControlServer` |
| **Model Versioning** | `GitVersionControlProvider` | `.evo/` refs, commit lineage | `GitManager` |

---

## 3. Transition Rules and Authority Rules

1. **Single Transition Authority**:
   - UI components, background threads, and trainers register as observers; they MUST NOT trigger out-of-band state transitions directly.
2. **Cancellation Propagation**:
   - User cancellation requests propagate down: UI ──► Session ──► Orchestrator ──► Execution Thread/Process.
   - Cancelled runs terminate with state `CANCELLED` (never misreported as `FAILED`).
3. **No Fake Success (Invariant I10)**:
   - A run reaching `100%` progress MUST have completed the `VALIDATING` stage.
   - Intermediate success (e.g., successful compilation) MUST NOT set state to `COMPLETED` without full runtime validation.
