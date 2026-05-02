# FIX PROMPT: Darwin Iteration Optimization

**Context:** Analysis (ITERATION_DARWIN_TRACE_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_DARWIN_TRACE_AI-DISCUSSION_02052026.MD) revealed redundant intent classification and tactical planning. The system must transition to a hardened state-transition machine.

**Architectural Guardrails:**
The system is a closed-loop state transition machine.
1. **Control Plane (IterationManager):** Sole authority for state transitions.
2. **Intelligence Plane (DarwinEngine):** Provides raw data only.
3. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Refactor the Darwin execution flow into a failure-resistant state-transition machine.

**Requirements:**
1. **State Entry Invariants:** Implement mandatory `validateStateEntry` checks in `IterationManager` (e.g., `PLAN_LOCKED` requires `finalPlan != null`).
2. **Verifying Contract:** Ensure ONLY `IterationManager` converts execution data into success/failure decisions and scores.
3. **Execution Gating:** Add a hard gate before the Orchestrator runs: `if (state != EXECUTING || !iteration.hasFinalPlan()) fail();`.
4. **Stateless Execution:** Strip all interpretation from the Orchestrator. It must follow the finalized plan without re-analysis.
5. **Decouple Intelligence:** DarwinEngine must have no knowledge of execution state transitions.
