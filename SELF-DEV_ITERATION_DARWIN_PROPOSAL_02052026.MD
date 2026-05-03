# FIX PROMPT: Self-Dev Darwin Redundancy Removal

**Context:** Analysis (SELF-DEV_ITERATION_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_DARWIN_AI-DISCUSSION_02052026.MD) established that the system must move to a hardened state-transition machine.

**Architectural Guardrails (Production Grade):**
The system is an **Evolutionary OS Kernel**.
1. **Control Plane (IterationManager):** Sole authority. Uses **TransitionTokens** to prevent state bypass.
2. **Intelligence Plane (DarwinEngine/Planner):** Provides raw data only. Side-effect free.
3. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Refactor the Self-Dev Darwinian flow into a definitive Evolutionary OS Kernel.

**Requirements:**
1. **TransitionToken Pattern:** Implement `applyTransition(TransitionToken token, SystemState newState)`. Only the `IterationManager` can issue valid tokens.
2. **State Entry Invariants:** Enforce mandatory requirements for node entry (e.g., `PLAN_LOCKED` requires `finalPlan != null`).
3. **Deterministic Verifying:** Refactor `VERIFYING` phase into a pure, versioned function: `verify(ExecutionResult result, FinalPlan plan) -> EvaluationResult`.
4. **Recovery Protocol:** Implement a `RECOVERING` state that enforces context rebuild and rollback to the last stable snapshot.
5. **Termination Guards:** Implement mandatory STOP logic for resource budget and score stagnation.
6. **Event Sourcing:** Implement an immutable `state_transitions.log` per iteration.
