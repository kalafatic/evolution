# Event, Signal, and Trajectory Model

## 1. Event-Driven Architecture
The kernel is built on a `RuntimeEventBus` that categorizes all system activity into seven categories:
- **KERNEL**: System lifecycle (Start, Stop, Mode changes).
- **FLOW**: Orchestration control (Iteration start/stop).
- **AGENT**: Reasoning layer (Hypothesis generation, Signal emission).
- **EXECUTION**: Side effects (Tool runs, Git operations).
- **UI**: Presentation and user interaction.
- **SUPERVISOR**: Governance and anomaly detection.
- **WORKSPACE**: Semantic environment events.

## 2. Evaluation Signals
Signals are the "nervous system" of the platform. An `EvaluationSignal` is a standardized DTO containing:
- **Variant ID**: The branch it refers to.
- **Evaluator ID**: The source of the signal.
- **Score**: Quantitative analysis (0.0 - 1.0).
- **Confidence**: Reliability of the score.
- **Severity**: Importance (INFO, WARNING, CRITICAL).

## 3. Signal Propagation
1. Evaluators (Correctness, Performance, etc.) analyze a variant.
2. They publish `EvaluationSignal` objects to the `SignalBus`.
3. The `DecisionResolver` aggregates these signals to make deterministic selection decisions.

## 4. Trajectory Memory
The platform tracks "Trajectories" — the path of evolution over time.
- **TrajectoryMemory**: Records successful and failed strategies.
- **Strategy Reinforcement**: Successful strategies receive a boost in future Darwin iterations.
- **Memory Decay**: Stale or unreinforced artifacts are gradually weakened to prevent context overload.
- **Failure Learning**: Recurring failure patterns are recorded to avoid repeating unsuccessful mutation paths.
