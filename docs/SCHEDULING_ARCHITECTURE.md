# OS-Style Scheduling Architecture for Darwin AI Kernel

This document describes the deterministic Backpressure and Execution Scheduling Layer implemented in the AI Kernel.

## Core Principle

All Darwin execution is treated as **scheduled compute tasks under limited cognitive resources**. The system prioritizes stability and predictability over unbounded parallel exploration.

## Architectural Components

### 1. KernelScheduler
The central execution governor. It decides which Darwin variants are allowed to proceed to evaluation based on the current system state and available budget.

### 2. ExecutionBudget
Defines hard and soft limits for an iteration:
- `maxVariantsAllowed`: Limit on parallel branches.
- `maxParallelEvaluations`: Concurrency limit for heavy tasks.
- `maxSignalThroughput`: Throttling threshold for telemetry.
- `timeBudgetMs`: Global timeout for the iteration.

### 3. BackpressureController
Monitors system saturation metrics:
- CPU and Memory pressure.
- Active evaluation count.
- Signal event rate.

### 4. SchedulingPolicy
Logic for selecting candidates when the budget is constrained.
- `DefaultSchedulingPolicy`: Simple FIFO-based selection.
- (Future) `LowConfidenceSkipPolicy`, `PriorityFirstPolicy`.

## Execution Lifecycle

1.  **Mutation**: Darwin Engine generates N proposals.
2.  **Scheduling**: `KernelScheduler` analyzes proposals and current `BackpressureStatus`.
3.  **Plan Generation**: A `ScheduledExecutionPlan` is produced, approving a subset (M <= N) of proposals.
4.  **Throttling**: `RuntimeEventBus` is configured with the plan's budget to throttle low-priority signals.
5.  **Execution**: `DarwinFlow` executes ONLY approved variants.
6.  **Resolution**: `ActivationResolver` filters all incoming signals to ensure only approved variants influence the final decision.

## Overload Handling

When `BackpressureController` detects high pressure:
- `maxVariantsAllowed` is dynamically reduced.
- `maxSignalThroughput` is halved.
- `RuntimeEventBus` drops `EVALUATION_SIGNAL_CREATED` events exceeding the rate limit.

## Flow Diagram

```text
[Darwin Engine] -> Proposals
       ↓
[KernelScheduler] <- [BackpressureController]
       ↓
[ScheduledExecutionPlan]
       ↓
[DarwinFlow] -> [Parallel Worktrees] -> [Evaluators]
                                           ↓
[RuntimeEventBus] <--- (Throttling) --- [Signals]
       ↓
[ActivationResolver] -> Final Winner
```
