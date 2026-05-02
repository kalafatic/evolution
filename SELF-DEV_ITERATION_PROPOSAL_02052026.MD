# FIX PROMPT: Self-Dev Iterative Context Propagation

**Context:** Analysis (SELF-DEV_ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_AI-DISCUSSION_02052026.MD) revealed triple-layered planning. The system must transition to a hardened state-transition machine.

**Architectural Guardrails:**
The system is a closed-loop state transition machine.
1. **Control Plane (IterationManager):** Sole authority for state transitions.
2. **Intelligence Plane (Planner/Supervisor):** Provides raw data only.
3. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Implement hardened state gating and plan handover for the Self-Dev flow.

**Requirements:**
1. **State Entry Invariants:** Implement mandatory `validateStateEntry` checks. e.g., `PLAN_LOCKED` requires `finalPlan != null`.
2. **Analysis Decoupling:** Modify `SelfDevSupervisor` to pass its analysis as raw data to `IterationManager`. `IterationManager` converts this data into a `PLAN_LOCKED` transition.
3. **Sole Transition Authority:** Only `IterationManager` can advance state. No component may independently re-interpret or mutate state.
4. **Stateless Execution:** Strip all intent classification and tactical planning from `EvolutionOrchestrator`. It must follow the plan provided by the `IterationManager` blindly once in the `EXECUTING` state.
