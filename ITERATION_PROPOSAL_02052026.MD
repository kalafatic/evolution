# FIX PROMPT: Iterative Flow Tactical Reuse

**Context:** Analysis (ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_AI-DISCUSSION_02052026.MD) revealed redundant planning layers. The system must transition to a hardened state-transition machine.

**Architectural Guardrails:**
The system is a closed-loop state transition machine.
1. **Control Plane (IterationManager):** Sole authority for state transitions.
2. **Intelligence Plane (Planner):** Provides raw data only.
3. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Refactor the Iterative flow into a hardened state-transition machine.

**Requirements:**
1. **Transition Authority:** Only `IterationManager` can transition the system to the `EXECUTING` state once the `TaskPlanner` has delivered the raw plan data.
2. **Entry Invariant:** The transition to `PLAN_LOCKED` must require `finalPlan != null`.
3. **Stateless Execution:** In `EvolutionOrchestrator.executeTaskWithRetries()`, if in `EXECUTING` state, use the tactical plan blindly. Eliminate the `agent.process()` tactical planning call.
4. **Decouple Reasoning:** Prohibit the execution layer from performing any semantic interpretation of the tasks.
