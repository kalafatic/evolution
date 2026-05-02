# FIX PROMPT: Self-Dev Darwin Redundancy Removal

**Context:** Analysis (SELF-DEV_ITERATION_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_DARWIN_AI-DISCUSSION_02052026.MD) established that the system must move to a hardened state-transition machine.

**Architectural Guardrails (Production Grade):**
1. **Control Plane (IterationManager):** Sole authority for state transitions. Uses **TransitionTokens** to prevent state bypass.
2. **Intelligence Plane (DarwinEngine/Planner):** External module. Outputs are raw data only; they MUST NOT influence control signals until processed by the `IterationManager`.
3. **Execution Plane (Orchestrator):** Stateless engine. Purely reactive to the `EXECUTING` state.

**Task for AI Coder:**
Refactor the Self-Dev Darwinian flow into a production-grade Evolutionary OS Kernel.

**Requirements:**
1. **TransitionToken Pattern:** Implement an `applyTransition(TransitionToken token, SystemState newState)` method. Only the `IterationManager` can issue valid tokens.
2. **Deterministic Verifying:** Refactor the `VERIFYING` phase into a pure, side-effect-free function: `verify(ExecutionResult result, FinalPlan plan) -> EvaluationResult`.
3. **Mutation Isolation:** Prohibit `DarwinEngine` from selecting candidates. It generates/evaluates data; `IterationManager` makes the selection decision.
4. **Recovery Protocol:** Implement a `RECOVERING` state that enforces a context rebuild and rollback to the last stable snapshot before re-entering `PLAN_LOCKED`.
5. **Termination Guards:** Implement mandatory STOP logic for max_iterations, time_budget, and score stagnation.
6. **Event Sourcing:** Implement an immutable transition log (`state_transitions.log`) for all phase shifts.
