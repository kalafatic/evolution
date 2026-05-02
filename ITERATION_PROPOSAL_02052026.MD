# FIX PROMPT: Iterative Flow Tactical Reuse

**Context:** Analysis (ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_AI-DISCUSSION_02052026.MD) revealed redundant planning layers. The system must move to a state-transition kernel model where the `IterationManager` is the sole Transition Authority.

**Task for AI Coder:**
Enable deterministic tactical plan execution in the Iterative flow.

**Requirements:**
1. **Transition Authority:** Ensure only `IterationManager` can advance state to `EXECUTING`.
2. **Immutable Plan Contract:** Modify the `Task` model to include a `finalTacticalPlan` field. In `TaskPlanner.generateTasks()`, have the AI populate this field as an immutable control signal.
3. **Stateless Execution:** In `EvolutionOrchestrator.executeTaskWithRetries()`, if the system state is `EXECUTING`, it MUST use the `finalTacticalPlan` blindly. Remove the `agent.process()` call in Phase 1 (PLAN) for such tasks.
4. **Decouple Reasoning:** Ensure the Orchestrator does not perform any semantic interpretation of the task intent once the system is in the execution phase.
