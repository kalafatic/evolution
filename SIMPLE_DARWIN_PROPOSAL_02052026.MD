# FIX PROMPT: Simple Darwin Flow Unification

**Context:** Analysis (SIMPLE_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_DARWIN_AI-DISCUSSION_02052026.MD) identified logic drift. The system must move to a unified Evolutionary OS Kernel.

**Architectural Guardrails (Production Grade):**
1. **Control Plane (IterationManager):** Sole transition authority. Uses **TransitionTokens**.
2. **Intelligence Plane (DarwinEngine):** Raw data only.
3. **Execution Plane (Orchestrator):** Stateless engine. Blind execution.

**Task for AI Coder:**
Unify Darwin execution logic under the `IterationManager` as a failure-resistant state machine.

**Requirements:**
1. **Unification:** Delegate all Darwin requests to `IterationManager`. Remove redundant reasoning in the Orchestrator.
2. **Hardened Transitions:** Implement the **TransitionToken** pattern for state changes.
3. **Pure Verification:** Refactor `VERIFYING` to be a pure function of execution result and plan.
4. **Control Gating:** Only `IterationManager` selects the best candidate; `DarwinEngine` only provides evaluations.
5. **Stateless Execution:** The Orchestrator blindly executes the plan once state is `EXECUTING`.
6. **Event Sourcing:** Persistent log of all transitions, variant generations, and selections.
