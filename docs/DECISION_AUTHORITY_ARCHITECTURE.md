# Decision Authority Architecture

## Overview
The Darwin orchestration system has been refactored to introduce a centralized deterministic decision authority layer called **ActivationResolver**. This layer ensures that all activation decisions (branch selection, winner determination, rank finalization) have a single, reproducible origin.

## Authority Flow

```text
Evaluators (Telemetry)
    ↓
EvaluationSignals (Immutable Observations)
    ↓
ActivationGate (Ranking & Recommendations)
    ↓
ActivationResolver (Deterministic Decision Authority)
    ↓
DecisionSnapshot (Immutable Decision Record)
    ↓
DarwinFlow (Execution Coordination)
```

## Core Components

### 1. ActivationResolver
The **ActivationResolver** is the ONLY component allowed to:
- Activate branches
- Select winners
- Finalize ranks
- Produce execution decisions

It is strictly deterministic and does not perform any execution, git mutations, or LLM calls.

### 2. DecisionSnapshot
Every decision made by the resolver is captured in an immutable **DecisionSnapshot**. This snapshot serves as the audit and debug artifact for every Darwin cycle. It includes:
- Iteration ID and Selected Variant ID
- Ranked list of variants
- Aggregated scores from signals
- Critical failures detected
- Activation reason and resolver policy used
- Confidence levels and timestamp

### 3. ResolverPolicy
The resolver uses a pluggable **ResolverPolicy** system. Policies are evaluated in order, and the first one that selects a variant wins. Available policies include:
- **HighestScorePolicy**: Selects the variant with the highest aggregated signal score.
- **ManualSelectionPolicy**: Respects explicit user selection (primary in MEDIATED mode).
- **ConfidenceThresholdPolicy**: Selects based on recommendation confidence levels.
- **CriticalFailurePolicy**: Filters out variants with critical signals before selection.

### 4. ActivationGate (Advisory Only)
The **ActivationGate** is now strictly advisory. It calculates rankings and recommendations but has NO authority to change branch state or enforce selections.

## Resolver Lifecycle

1. **Discovery**: DarwinFlow generates variants.
2. **Evaluation**: Evaluators execute variants and emit `EvaluationSignal`s.
3. **Recommendation**: ActivationGate consumes variants/signals and produces `ActivationRecommendation`s.
4. **Resolution**: ActivationResolver consumes signals, recommendations, and policies to produce a `DecisionSnapshot`.
5. **Execution**: DarwinFlow executes the decision recorded in the snapshot (e.g., merging the selected branch).

## Migration Notes

Previously, decision logic was scattered across `DarwinFlow` and partially influenced by `ActivationGate`.
- **DarwinFlow**: Score comparison logic has been replaced by calls to `ActivationResolver`.
- **ActivationGate**: State mutation and auto-activation heuristics have been removed.
- **Signals**: All logic now relies on `EvaluationSignal` objects collected via the `RuntimeEventBus`.

This refactor establishes a strict authority pipeline that improves auditability and enables the platform to scale with complex evaluator systems without increasing orchestration complexity.
