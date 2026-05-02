# FIX PROMPT: Self-Dev Iterative Context Propagation

**Context:** Analysis (SELF-DEV_ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_AI-DISCUSSION_02052026.MD) revealed triple-layered planning. The system must transition to a production-grade Evolutionary OS Kernel.

**Architectural Guardrails (Production Grade):**
1. **Control Plane (IterationManager):** Sole transition authority. Uses **TransitionTokens**.
2. **Intelligence Plane (Planner/Supervisor):** Provides raw data only. Decoupled from execution.
3. **Execution Plane (Orchestrator):** Stateless engine gated by `EXECUTING` state.

**Task for AI Coder:**
Implement hardened state gating and plan handover for the Self-Dev flow.

**Requirements:**
1. **Transition Authority:** Ensure `IterationManager` is the ONLY component permitted to advance state. Implement the **TransitionToken** pattern.
2. **Analysis Decoupling:** Modify `SelfDevSupervisor` to pass initial data to `IterationManager`. `IterationManager` converts this data into a state transition decision.
3. **Execution Gating:** The Orchestrator must run ONLY if `state == EXECUTING`.
4. **Stateless Execution:** Strip all interpretation from `EvolutionOrchestrator`. It must follow the plan blindly.
5. **Recovery Model:** Implement a `RECOVERING` state for failed iterations.
6. **Event Sourcing:** Implement an immutable transition log for auditability.
