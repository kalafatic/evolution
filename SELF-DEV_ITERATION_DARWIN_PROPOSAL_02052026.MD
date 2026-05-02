# FIX PROMPT: Self-Dev Darwin Redundancy Removal

**Context:** Analysis (SELF-DEV_ITERATION_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_DARWIN_AI-DISCUSSION_02052026.MD) established that the system must move to a hardened state-transition machine.

**Architectural Guardrails:**
The system is a closed-loop state transition machine.
1. **Control Plane (IterationManager):** Sole authority for state transitions. Converts intelligence data into decisions.
2. **Intelligence Plane (DarwinEngine/Planner):** Provides raw data only. No control over transitions.
3. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Refactor the Self-Dev Darwinian flow into a failure-resistant state-transition kernel.

**Requirements:**
1. **State Entry Invariants:** Implement `validateStateEntry` in `IterationManager`. e.g., `PLAN_LOCKED` requires `finalPlan != null`, `VERIFYING` requires `executionResult != null`.
2. **Verifying Contract:** Ensure ONLY `IterationManager` converts `executionResult` into `evaluationResult` and decides the next state (DONE or MUTATING).
3. **Mutation Isolation:** The `MUTATING` state must be prohibited from execution. It only produces new plan data.
4. **Execution Gate:** Add a hard gate before the Orchestrator: `if (state != EXECUTING || !iteration.hasFinalPlan()) fail();`.
5. **Stateless Execution:** Strip all interpretation from the Orchestrator. It must follow the plan without re-analysis.
