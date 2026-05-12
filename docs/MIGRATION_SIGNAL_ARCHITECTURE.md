# Migration Signal Architecture

## Overview
This document outlines the refactored signal-based evaluation architecture introduced in the Darwin platform. It serves as a foundation for centralized deterministic decision-making.

## 1. Core Components

### EvaluationSignal
A standardized telemetry object used by all evaluators to report observations.
- `variantId`: Unique identifier for the proposal branch.
- `evaluatorId`: Name of the evaluator producing the signal (e.g., MavenEvaluator, SemanticAnalyzer).
- `score`: Scalar indicator (0.0 to 1.0) of quality/success.
- `confidence`: Evaluator's confidence in the score (0.0 to 1.0).
- `severity`: INFO, WARNING, or CRITICAL.
- `explanation`: Human-readable rationale.
- `timestamp`: Time of creation.

### SignalSeverity
Enum defining the impact of a signal:
- `INFO`: Normal observation.
- `WARNING`: Minor issue or high-risk pattern detected.
- `CRITICAL`: Blocking failure (e.g., build error).

## 2. Propagation Model
Evaluators are now strictly **telemetry producers**. They MUST NOT:
- Activate branches.
- Select winners.
- Modify variant scores directly (legacy support remains but is deprecated).

Signals are published via the `RuntimeEventBus` using the `EVALUATION_SIGNAL_CREATED` event type.

## 3. Future Integration Points

### ActivationResolver (Future)
The `ActivationResolver` will be the central component that:
1. Subscribes to `VARIANT_EVALUATED` events.
2. Collects all `EvaluationSignal` objects for each variant.
3. Applies a deterministic policy (e.g., weighted average, critical gate) to resolve the final active branch.

### DecisionSnapshot (Future)
An immutable record of all signals and the final resolution logic used for a specific iteration, facilitating transparency and auditability.

### ActivationGate (Current Role)
The `ActivationGate` remains a **recommendation layer**. It processes signals to rank variants but delegates the final state transition to the Kernel.

## 4. Implementation Guidelines
- **Stateless Evaluators**: Evaluators should remain stateless and focused on a single dimension of analysis.
- **Async Processing**: Evaluation occurs in parallel, with signals being aggregated asynchronously by the orchestration flow.
- **Metadata Extensibility**: Use the `metadata` map in `EvaluationSignal` for domain-specific telemetry (e.g., compile error counts, linting violations).
