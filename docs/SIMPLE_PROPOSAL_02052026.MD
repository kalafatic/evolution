# FIX PROMPT: Simple Chat Flow Hardening

**Context:** Analysis (SIMPLE_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_AI-DISCUSSION_02052026.MD) established that even simple chat must follow the "Evolutionary OS Kernel" model to avoid interpretation drift.

**Architectural Guardrails (Production Grade):**
1. **Control Plane (IterationManager):** Sole authority for state transitions. Uses **TransitionTokens**.
2. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable paths.

**Task for AI Coder:**
Refactor the simple chat flow into a state-driven reactive path.

**Requirements:**
1. **Transition Authority:** Ensure `IterationManager` is the only component that can transition the system to the `CHAT_ACTIVE` state.
2. **Deterministic Response:** If in `CHAT_ACTIVE` state, the Orchestrator must only return the response without initializing task contexts or reasoning loops.
3. **Mode Projection:** Implement Chat as a minimal projection: `INIT -> CHAT_ACTIVE -> DONE`.
4. **State Gating:** Prohibit any semantic interpretation of the prompt once the state has been set.
5. **Event Sourcing:** Persistent log of the transition to and from conversational mode.
