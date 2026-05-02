# FIX PROMPT: Simple Chat Flow Hardening

**Context:** Analysis (SIMPLE_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_AI-DISCUSSION_02052026.MD) established that even simple chat must follow the "state-transition kernel" model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine.
1. **Control Plane (IterationManager):** Sole authority for state transitions.
2. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Refactor the simple chat flow into a state-driven reactive path.

**Requirements:**
1. **Transition Authority:** Ensure `IterationManager` is the only component that can transition the system to the `CHAT_ACTIVE` state.
2. **Entry Invariant:** The transition to `CHAT_ACTIVE` must require valid mode-router data.
3. **Mode Projection:** Implement the Chat flow as a minimal subset of the transition table: `INIT -> CHAT_ACTIVE -> DONE`.
4. **Stateless Execution:** If in `CHAT_ACTIVE` state, the Orchestrator must only return the generated response without initializing complex task contexts or reasoning loops.
