# FIX PROMPT: Self-Dev Darwin Redundancy Removal

**Context:** Analysis (SELF-DEV_ITERATION_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_DARWIN_AI-DISCUSSION_02052026.MD) revealed redundant evaluations and interpretation drift. The system must move to a state-transition kernel model where the `IterationManager` is the sole Transition Authority.

**Task for AI Coder:**
Refactor the Self-Dev Darwinian flow into a hardened state-transition machine.

**Requirements:**
1. **Transition Authority:** Ensure `IterationManager` is the ONLY component permitted to mutate system state. It must convert `DarwinEngine` raw data into state transition decisions.
2. **Stateless Execution:** Refactor `EvolutionOrchestrator.handle()` to blindly execute plans when the state is `EXECUTING`. All semantic interpretation of intent must be removed from the execution layer.
3. **Intelligence Gating:** Prohibit `DarwinEngine` from having any knowledge of execution state or transitions. Its role is strictly "intelligence as a service" for the `IterationManager`.
4. **Result Recycling:** Reuse variant evaluation results as the final iteration outcome. The `IterationManager` must advance the state based on these results without triggering the Orchestrator for redundant final checks.
