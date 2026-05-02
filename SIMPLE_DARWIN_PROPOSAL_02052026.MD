# FIX PROMPT: Simple Darwin Flow Unification

**Context:** Analysis (SIMPLE_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_DARWIN_AI-DISCUSSION_02052026.MD) identified logic drift. The system must move to a unified "state-transition kernel" model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine.
1. **Control Plane (IterationManager):** Sole authority for state transitions.
2. **Intelligence Plane (DarwinEngine):** Provides raw data only.
3. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Unify Darwin execution logic under the `IterationManager` as a failure-resistant state machine.

**Requirements:**
1. **Convergence:** Refactor `AiChatPage` to delegate all Darwin requests to `IterationManager`. Remove redundant PEV logic from the core Orchestrator.
2. **State Entry Invariants:** Implement mandatory `validateStateEntry` checks. e.g., `EXECUTING` requires `finalPlan != null`.
3. **Sole Transition Authority:** Only `IterationManager` may advance state. It converts DarwinEngine data into plan selection decisions.
4. **Stateless Execution:** The Orchestrator must blindly execute the plan once in the `EXECUTING` state, with no re-analysis or planning logic.
