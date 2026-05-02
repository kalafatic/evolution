# FIX PROMPT: Simple Chat Flow Hardening

**Context:** Analysis (SIMPLE_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_AI-DISCUSSION_02052026.MD) established that the system must move to a "state-transition kernel" model. Even for simple chat, the Orchestrator should not re-analyze intent if the mode has been deterministically set.

**Task for AI Coder:**
Harden the simple chat flow into a state-driven reactive path.

**Requirements:**
1. **State-Gated Entry:** In `EvolutionOrchestrator.handle()`, move the ModeRouter logic and Greeting detection to a pre-orchestration phase.
2. **Stateless Response:** If the mode is `SIMPLE_CHAT` or a greeting is detected, return the response immediately. Do not initialize `TaskContext` or `ConversationState` for these terminal states unless a persistent goal is already active.
3. **Transition Authority:** Ensure that `IterationManager` (or its equivalent in simple chat context) is the only component that can transition the system into a "CHAT_ACTIVE" state.
4. **Remove Semantic Interpretation:** Stripped the Orchestrator of the ability to "decide" if a request is chat-worthy once the mode has been set by the authority layer.
