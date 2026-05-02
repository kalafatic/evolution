# FIX PROMPT: Iterative Flow Tactical Reuse

**Context:** Analysis (ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_AI-DISCUSSION_02052026.MD) revealed redundant planning layers. The system must transition to a hardened state-transition kernel model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- The **IterationManager** is the sole authority responsible for interpreting intelligence outputs, converting evaluation results into state transitions, and advancing the system through lifecycle phases.
- The **EvolutionOrchestrator** operates as a strictly stateless execution engine. It consumes only finalized, immutable plans and executes them deterministically without performing interpretation, planning, or evaluation.
- The **DarwinEngine** functions as an external intelligence module responsible solely for generating and evaluating candidate plans. Its outputs are treated as raw data.

**Task for AI Coder:**
Refactor the Iterative flow to enforce the "Plan Contract" and remove redundant reasoning.

**Requirements:**
1. **Transition Authority:** Ensure only `IterationManager` can transition the system to the `EXECUTING` state once the `TaskPlanner` has delivered the raw plan data.
2. **Immutable Plan Contract:** The `TaskPlanner` output must be converted by `IterationManager` into an immutable execution plan.
3. **Stateless Execution:** In `EvolutionOrchestrator.executeTaskWithRetries()`, if the system state is `EXECUTING`, use the pre-defined tactical plan blindly. Eliminate the `agent.process()` planning call.
4. **Decouple Reasoning:** Prohibit the execution layer from performing any semantic interpretation of the tasks.
