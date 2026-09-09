# Operations Lifecycle and State Authority

**Document Identifier**: `docs/architecture/lifecycle.md`
**Date**: September 9, 2026

---

## 1. Unified Operational State Machine

All long-running execution runs across EVO (Inference, Darwin, Forge, Self-Dev) conform to a standardized lifecycle state machine:

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

## 2. Transition Rules and Authority

1. **Single Transition Authority**:
   - For Forge workflows: `ForgeJob` state machine (`eu.kalafatic.evolution.forge.controller.api.ForgeJob`).
   - For Darwin iterations: `IterationManager`.
   - For Self-Dev execution: `SelfDevSupervisor`.
2. **Cancellation Propagation**:
   - User cancellation requests propagate down: UI ──► Session ──► Orchestrator ──► Execution Thread/Process.
   - Cancelled runs terminate with state `CANCELLED` (never misreported as `FAILED`).
3. **No Fake Success (Invariant I10)**:
   - A run reaching `100%` progress MUST have completed the `VALIDATING` stage.
   - Intermediate success (e.g., successful compilation) MUST NOT set state to `COMPLETED` without full runtime validation.
