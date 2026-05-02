# FIX PROMPT: Simple Darwin Flow Unification

**Context:** Analysis (SIMPLE_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_DARWIN_AI-DISCUSSION_02052026.MD) identified a fundamental logic drift between Chat-Darwin and Supervisor-Darwin. The system must move to a state-transition kernel model where the `IterationManager` is the sole Transition Authority.

**Task for AI Coder:**
Unify all Darwinian execution logic under the `IterationManager` as a deterministic state machine.

**Requirements:**
1. **Unify Entry Points:** Refactor `AiChatPage.startSelfDevAction` to delegate Chat-initiated Darwin requests to the `IterationManager`. Remove the internal PEV loop fallback in the Orchestrator.
2. **Transition Authority:** Ensure `IterationManager` is the ONLY component allowed to advance state from `ANALYZE` to `PLAN` and `EXECUTE`.
3. **Intelligence Gating:** Modify the flow so that `DarwinEngine` variants are returned to `IterationManager` as raw data. `IterationManager` must then perform the selection and "lock" the plan into the system state before triggering execution.
4. **Stateless Orchestrator:** Ensure the Orchestrator executes the resulting tasks blindly, without performing any additional reasoning or planning, once the `IterationManager` has transitioned the system to `EXECUTING`.
