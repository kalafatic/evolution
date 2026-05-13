# AI Evolution Platform Architecture

## 1. Overview
The AI Evolution Platform is a deterministic, state-transition-based evolutionary kernel designed for autonomous and semi-autonomous software development. It leverages an orchestration-first approach where the kernel manages intelligence rather than being replaced by it.

## 2. Core Vision
The platform is not a chatbot; it is a **Modular Capability Platform**. It treats AI models as pluggable "coprocessors" while maintaining absolute control over state and execution through a centralized kernel.

## 3. Authority Hierarchy
The system operates under a strict hierarchy:
1. **Human Supervisor**: Final authority on all state changes in MEDIATED mode.
2. **Kernel (IterationManager)**: The sole component allowed to transition system states.
3. **DecisionResolver**: The sole component allowed to activate variants and select winners.
4. **DarwinEngine / Agents**: Responsible for exploration and proposal generation (No authority).

## 4. Key Components
- **IterationManager**: The Control Plane. Orchestrates the lifecycle of an intent.
- **DarwinEngine**: Generates branch variants for exploration.
- **SignalBus**: Standardized backplane for all evaluation telemetry.
- **DecisionResolver**: Deterministic authority for variant selection.
- **Semantic Workspace**: Persistent reasoning environment and trajectory memory.

## 5. Invariants
- **Single Transition Authority**: Only `IterationManager` may change system state.
- **Stateless Execution**: The orchestrator executes without making strategic decisions.
- **Intelligence Isolation**: Agents are pure; they do not control the flow.
- **Deterministic State Machine**: Behavior must always be explainable as `STATE → TRANSITION → STATE`.

## 6. Execution Pipeline (PEV Loop)
Every task follows the Plan-Execute-Verify cycle:
1. **PLAN**: Define the technical approach.
2. **CONTEXT**: Gather relevant artifacts.
3. **EXECUTE**: Apply changes (typically via Git branches).
4. **VERIFY**: Evaluate against constraints and tests.
5. **ANALYZE**: Diagnose failures and determine progress.
6. **MUTATE**: Adjust strategy if necessary.
