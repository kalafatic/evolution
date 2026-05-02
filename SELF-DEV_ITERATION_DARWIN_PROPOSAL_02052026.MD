# FIX PROMPT: Self-Dev Darwin Redundancy Removal

**Context:** Analysis (SELF-DEV_ITERATION_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_DARWIN_AI-DISCUSSION_02052026.MD) revealed redundant evaluations and interpretation drift. The system must move to a hardened state-transition machine.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- The **IterationManager** is the sole authority responsible for interpreting intelligence outputs, converting evaluation results into state transitions, and advancing the system through lifecycle phases.
- The **EvolutionOrchestrator** operates as a strictly stateless execution engine. It consumes only finalized, immutable plans and executes them deterministically without performing interpretation, planning, or evaluation.
- The **DarwinEngine** functions as an external intelligence module responsible solely for generating and evaluating candidate plans. Its outputs are treated as raw data.

**Task for AI Coder:**
Refactor the Self-Dev Darwinian flow into a hardened state-transition kernel.

**Requirements:**
1. **Transition Authority:** Ensure `IterationManager` is the ONLY component permitted to mutate system state based on `DarwinEngine` raw data.
2. **Stateless Execution:** Refactor `EvolutionOrchestrator.handle()` to blindly execute plans when the state is `EXECUTING`. All semantic interpretation of intent must be removed from the execution layer.
3. **Intelligence Gating:** Prohibit `DarwinEngine` from having any knowledge of execution state or transitions.
4. **Result Recycling:** Reuse variant evaluation results as the final iteration outcome. Advance state based on these results without redundant Orchestrator checks.
