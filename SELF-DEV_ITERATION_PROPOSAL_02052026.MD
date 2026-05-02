# FIX PROMPT: Self-Dev Iterative Context Propagation

**Context:** Analysis (SELF-DEV_ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_AI-DISCUSSION_02052026.MD) identified "triple-layered planning" as a major bottleneck. The system must move to a hardened state-transition machine.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- **Sole Transition Authority:** Only the **IterationManager** can advance or mutate system state. It converts intelligence outputs into control signals.
- **State-Ignorant Executor:** The **EvolutionOrchestrator** consumes finalized, immutable plans and executes them blindly if gated by the global `EXECUTING` state. It performs NO semantic interpretation of intent.
- **Intelligence Gating:** **Supervisor** and **TaskPlanner** outputs are raw data; they have no influence on behavior until processed by the Transition Authority.

**Task for AI Coder:**
Implement hardened state gating and plan handover for the Self-Dev flow.

**Requirements:**
1. **Transition Table Enforcement:** Implement a mandatory `isValidTransition` check in `IterationManager` based on: `INIT -> ANALYZING -> PLAN_LOCKED -> EXECUTING -> VERIFYING -> DONE`.
2. **Analysis Decoupling:** Modify `SelfDevSupervisor` so its initial analysis is passed to `IterationManager` as raw data. `IterationManager` must then explicitly transition the state to `PLAN_LOCKED`.
3. **Execution Gating:** Ensure the Orchestrator runs ONLY if `state == EXECUTING`.
4. **Stateless Execution:** Strip all intent classification and planning from `EvolutionOrchestrator`. It must follow the plan blindly once the state machine triggers execution.
5. **Transition Logging:** Implement mandatory `[STATE] FROM -> TO (Reason)` logging.
