# FIX PROMPT: Simple Chat Flow Hardening

**Context:** Analysis (SIMPLE_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_AI-DISCUSSION_02052026.MD) established that even simple chat must follow the "state-transition kernel" model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- **Sole Transition Authority:** Only the **IterationManager** can advance or mutate system state. It converts intelligence outputs into control signals.
- **State-Ignorant Executor:** The **EvolutionOrchestrator** consumes finalized, immutable plans and executes them blindly if gated by the global `EXECUTING` state. It performs NO semantic interpretation of intent.
- **Intelligence Gating:** Any classification results are treated as raw data; they have no influence on behavior until processed by the Transition Authority.

**Task for AI Coder:**
Refactor the simple chat flow into a state-driven reactive path.

**Requirements:**
1. **Transition Authority:** Ensure `IterationManager` is the only component that can transition the system to the `CHAT_ACTIVE` state.
2. **Mode Projection:** Implement the Chat flow as a minimal subset of the transition table: `INIT -> CHAT_ACTIVE -> DONE`.
3. **Execution Gating:** Move ModeRouter logic to a pre-orchestration phase that gates entry to the Orchestrator.
4. **Stateless Orchestrator:** If in `CHAT_ACTIVE` state, the Orchestrator must only return the generated response without initializing task contexts or reasoning loops.
5. **Transition Logging:** Log the shift to `CHAT_ACTIVE`.
