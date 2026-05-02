# FIX PROMPT: Darwin Iteration Optimization

**Context:** Analysis (ITERATION_DARWIN_TRACE_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_DARWIN_TRACE_AI-DISCUSSION_02052026.MD) revealed redundant intent classification and tactical planning. The system must transition to a hardened Evolutionary OS Kernel.

**Architectural Guardrails (Production Grade):**
1. **Control Plane (IterationManager):** Sole authority for state transitions. Uses **TransitionTokens**.
2. **Intelligence Plane (DarwinEngine):** Raw data only. Side-effect free.
3. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Refactor the Darwin execution flow into a production-grade state-transition machine.

**Requirements:**
1. **TransitionToken Pattern:** Hardened transition logic preventing state bypass.
2. **Deterministic Verifying:** Refactor `VERIFYING` phase into a pure deterministic function.
3. **Control Gating:** Only `IterationManager` converts variant data into the selected `finalPlan`.
4. **Stateless Execution:** Strip all interpretation from the Orchestrator. It follows the finalized plan without re-analysis.
5. **Termination Guards:** Implement mandatory STOP logic for score stagnation and iteration limits.
6. **Event Sourcing:** Implement an immutable transition log for auditability.
