# FIX PROMPT: Simple Darwin Flow Unification

**Context:** Analysis (SIMPLE_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_DARWIN_AI-DISCUSSION_02052026.MD) identified logic drift between Chat-initiated Darwin and Supervisor-initiated Darwin.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- **Sole Transition Authority:** Only the **IterationManager** can advance or mutate system state. It converts intelligence outputs into control signals.
- **State-Ignorant Executor:** The **EvolutionOrchestrator** consumes finalized, immutable plans and executes them blindly if gated by the global `EXECUTING` state. It performs NO semantic interpretation of intent.
- **Intelligence Gating:** **DarwinEngine** outputs are raw data; they have no influence on behavior until processed by the Transition Authority.

**Task for AI Coder:**
Unify Darwin execution logic under the `IterationManager` as a deterministic state machine.

**Requirements:**
1. **Unified Entry Point:** Refactor `AiChatPage` to delegate Darwin requests to `IterationManager`. Eliminate redundant reasoning in the Orchestrator.
2. **Transition Table Enforcement:** Implement mandatory `isValidTransition` check in `IterationManager` (INIT -> ANALYZING -> PLAN_LOCKED -> EXECUTING -> VERIFYING -> DONE).
3. **Intelligence Decoupling:** `DarwinEngine` variants must be returned as raw data to `IterationManager`, which then "locks" the selected plan.
4. **Stateless Orchestrator:** The Orchestrator must execute the locked plan blindly once the system enters the `EXECUTING` state.
5. **Transition Logging:** Implement mandatory `[STATE] FROM -> TO (Reason)` logging.
