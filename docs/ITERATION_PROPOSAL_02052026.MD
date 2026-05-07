# FIX PROMPT: Iterative Flow Tactical Reuse

**Context:** Analysis (ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_AI-DISCUSSION_02052026.MD) revealed redundant planning layers. The system must transition to a production-grade Evolutionary OS Kernel.

**Architectural Guardrails (Production Grade):**
1. **Control Plane (IterationManager):** Sole authority for state transitions. Uses **TransitionTokens**.
2. **Intelligence Plane (Planner):** Raw data only.
3. **Execution Plane (Orchestrator):** Stateless engine. Blindly executes immutable plans.

**Task for AI Coder:**
Refactor the Iterative flow into a hardened state-transition machine.

**Requirements:**
1. **Transition Authority:** Only `IterationManager` can advance state via the **TransitionToken** pattern.
2. **Immutable Plan:** Convert the `TaskPlanner` output into an immutable `finalPlan` inside the `IterationManager`.
3. **Stateless Execution:** In `EvolutionOrchestrator.executeTaskWithRetries()`, follow the plan blindly without intent interpretation.
4. **Recovery Protocol:** Implement `RECOVERING` state for failed iterations.
5. **Event Sourcing:** Persistent immutable log of state shifts.
