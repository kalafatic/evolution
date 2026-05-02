# FIX PROMPT: Self-Dev Iterative Context Propagation

**Context:** Analysis (SELF-DEV_ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_AI-DISCUSSION_02052026.MD) revealed triple-layered planning. The system must move to a state-transition kernel model where the `IterationManager` is the sole Transition Authority.

**Task for AI Coder:**
Implement hardened context propagation and state gating for the Self-Dev flow.

**Requirements:**
1. **Transition Authority:** Ensure `IterationManager` is the ONLY component allowed to convert Supervisor analysis into execution-ready plans and state transitions.
2. **Analysis Decoupling:** Modify `SelfDevSupervisor` so its initial analysis is passed to `IterationManager` as raw data. `IterationManager` then becomes responsible for "locking" this analysis into the system state.
3. **Stateless Orchestrator:** Refactor `EvolutionOrchestrator.handle()` to skip all intent re-analysis and planning if the system state is `EXECUTING`. It must blindly follow the plan provided by the `IterationManager`.
4. **Enforce separation:** Prohibit the Orchestrator from semantically interpreting the Supervisor's intent once the execution phase has started.
