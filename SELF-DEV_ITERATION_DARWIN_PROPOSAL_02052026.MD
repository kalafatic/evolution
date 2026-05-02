# FIX PROMPT: Self-Dev Darwin Redundancy Removal

**Context:** Analysis (SELF-DEV_ITERATION_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_DARWIN_AI-DISCUSSION_02052026.MD) revealed redundant evaluations and interpretation drift. The system must move to a hardened state-transition machine.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- **Sole Transition Authority:** Only the **IterationManager** can advance or mutate system state. It converts intelligence outputs into control signals.
- **State-Ignorant Executor:** The **EvolutionOrchestrator** consumes finalized, immutable plans and executes them blindly if gated by the global `EXECUTING` state. It performs NO semantic interpretation of intent.
- **Intelligence Gating:** **DarwinEngine** outputs are raw data; they have no influence on behavior until processed by the Transition Authority.

**Task for AI Coder:**
Refactor the Self-Dev Darwinian flow into a hardened state-transition kernel.

**Requirements:**
1. **Transition Table Enforcement:** Implement a mandatory `isValidTransition` check in `IterationManager` based on: `INIT -> ANALYZING -> PLAN_LOCKED -> EXECUTING -> VERIFYING -> DONE|MUTATING`.
2. **Execution Gating:** Add a gate before the Orchestrator runs: `if (state != EXECUTING || !iteration.hasFinalPlan()) fail();`.
3. **Stateless Execution:** Strip the Orchestrator of its tactical autonomy. It must follow the plan without re-analysis once the state is `EXECUTING`.
4. **Mode Projections:** Ensure that Chat and Darwin modes are implemented as specific projections (subsets of the transition table) through the same kernel logic.
5. **Transition Logging:** Implement mandatory `[STATE] FROM -> TO (Reason)` logging for auditability.
