# FIX PROMPT: Iterative Flow Tactical Reuse

**Context:** Analysis (ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_AI-DISCUSSION_02052026.MD) revealed redundant planning layers. The system must move to a hardened state-transition kernel model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- **Sole Transition Authority:** Only the **IterationManager** can advance or mutate system state. It converts intelligence outputs into control signals.
- **State-Ignorant Executor:** The **EvolutionOrchestrator** consumes finalized, immutable plans and executes them blindly if gated by the global `EXECUTING` state. It performs NO semantic interpretation of intent.
- **Intelligence Gating:** **TaskPlanner** outputs are raw data; they have no influence on behavior until processed by the Transition Authority.

**Task for AI Coder:**
Refactor the Iterative flow into a hardened state-transition machine.

**Requirements:**
1. **Transition Table Enforcement:** Implement a mandatory `isValidTransition` check in `IterationManager` based on: `INIT -> ANALYZING -> PLAN_LOCKED -> EXECUTING -> DONE`.
2. **Immutable Plan Contract:** Convert the `TaskPlanner` raw output into an immutable `finalPlan` inside the `IterationManager` before advancing to `EXECUTING`.
3. **Stateless Execution:** In `EvolutionOrchestrator.executeTaskWithRetries()`, if `state == EXECUTING`, use the tactical plan blindly. Eliminate the `agent.process()` tactical planning call.
4. **Decouple Reasoning:** Prohibit the execution layer from performing any semantic interpretation of the tasks.
5. **Transition Logging:** Implement mandatory `[STATE] FROM -> TO (Reason)` logging.
