# FIX PROMPT: Darwin Iteration Optimization

**Context:** Analysis (ITERATION_DARWIN_TRACE_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_DARWIN_TRACE_AI-DISCUSSION_02052026.MD) revealed redundant intent classification and tactical planning. The system must transition to a hardened state-transition kernel model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- **Sole Transition Authority:** Only the **IterationManager** can advance or mutate system state. It converts intelligence outputs into control signals.
- **State-Ignorant Executor:** The **EvolutionOrchestrator** consumes finalized, immutable plans and executes them blindly if gated by the global `EXECUTING` state. It performs NO semantic interpretation of intent.
- **Intelligence Gating:** **DarwinEngine** outputs are raw data; they have no influence on behavior until processed by the Transition Authority.

**Task for AI Coder:**
Refactor the Darwin execution flow into a hardened state-transition machine.

**Requirements:**
1. **Transition Table Enforcement:** Implement a mandatory `isValidTransition` check in `IterationManager` based on: `INIT -> ANALYZING -> PLAN_LOCKED -> EXECUTING -> VERIFYING -> DONE|MUTATING`.
2. **Execution Gating:** Add a gate before the Orchestrator runs: `if (state != EXECUTING || !iteration.hasFinalPlan()) fail();`.
3. **Stateless Execution:** Strip the Orchestrator of its tactical autonomy. It must follow the plan without re-analysis once the state is `EXECUTING`.
4. **Intelligence Decoupling:** Modify the flow so that `DarwinEngine` variants are returned to `IterationManager` as raw data. `IterationManager` must perform the selection and state shift.
5. **Transition Logging:** Implement mandatory `[STATE] FROM -> TO (Reason)` logging.
