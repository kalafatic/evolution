# Stabilization Philosophy

## 1. Principles of Stabilization
Stabilization is about reinforcement, not simplification. The goal is to make the existing complex architecture reliable, traceable, and deterministic.

- **Semantic Stabilization**: Ensuring terms like "Variant", "Signal", and "Trajectory" have consistent meanings across the codebase.
- **Architectural Clarity**: Explicitly documenting boundaries and authority.
- **Deterministic Behavior**: Ensuring the same input and state produce the same orchestration path.
- **Controlled Evolution**: Allowing the system to evolve while maintaining strict kernel-level bounds.

## 2. Core Invariants
These rules must never be broken:
1. **Single Transition Authority**: Only `IterationManager` transitions system state.
2. **Stateless Execution**: `EvolutionOrchestrator` remains a blind executor.
3. **No Bypass Paths**: All flows must go through the Kernel.
4. **Isolated Intelligence**: Agents have no state awareness or control responsibility.

## 3. Observability and Traceability
Stabilization requires deep visibility into the kernel's "thought process."
- **Cognitive Trace**: Every decision is recorded as a `CausalNode` in a deterministic chain.
- **Decision Snapshots**: The state of all signals and rankings at the moment of a decision is captured for auditing.
- **Replayability**: The system aims for the ability to replay an entire iteration from its trace.

## 4. Conflict Resolution
When authorities conflict (e.g., an automated evaluator rejects a variant but a user wants to proceed), the **Authority Hierarchy** is used to resolve the issue deterministically. Human authority always occupies the top tier.
