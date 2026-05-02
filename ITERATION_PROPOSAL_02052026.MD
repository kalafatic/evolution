# FIX PROMPT: Iterative Flow Tactical Reuse

**Context:** Analysis (ITERATION_ANALYSIS_02052026.MD) shows that `TaskPlanner` generates atomic tasks with clear rationales, but `EvolutionOrchestrator` then asks the agent to generate a "tactical plan" again for each task.

**Task for AI Coder:**
Enable tactical plan reuse in the Iterative flow.

**Requirements:**
1. **Pass Tactical Data:** Modify the `Task` model to include a `suggestedTacticalPlan` field.
2. **Planner Logic:** In `TaskPlanner.generateTasks()`, have the AI populate this field with the specific steps it expects for the task.
3. **Orchestrator Bypass:** In `EvolutionOrchestrator.executeTaskWithRetries()`, check if the task already has a tactical plan. If it does, skip the `agent.process()` call in Phase 1 (PLAN) and use the pre-existing plan.
4. **Validation:** Ensure the `ValidatorAgent` still validates the execution even if the plan was pre-defined.
