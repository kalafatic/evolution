# FIX PROMPT: Simple Darwin Flow Unification

**Context:** Analysis (SIMPLE_DARWIN_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_DARWIN_AI-DISCUSSION_02052026.MD) identified logic drift between Chat-initiated Darwin and Supervisor-initiated Darwin. The system must move to a unified "state-transition kernel" model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- The **IterationManager** is the sole authority responsible for interpreting intelligence outputs, converting evaluation results into state transitions, and advancing the system through lifecycle phases.
- The **EvolutionOrchestrator** operates as a strictly stateless execution engine. It consumes only finalized, immutable plans and executes them deterministically without performing interpretation, planning, or evaluation.
- The **DarwinEngine** functions as an external intelligence module responsible solely for generating and evaluating candidate plans. Its outputs are treated as raw data.

**Task for AI Coder:**
Unify Darwin execution logic under the `IterationManager` as a deterministic state machine.

**Requirements:**
1. **Convergence:** Refactor `AiChatPage` to delegate all Darwin requests to `IterationManager`. Remove redundant PEV loop logic from the core Orchestrator.
2. **Transition Authority:** Ensure `IterationManager` is the sole component permitted to advance state from `ANALYZE` to `EXECUTE`.
3. **Intelligence Decoupling:** `DarwinEngine` variants must be returned as raw data to `IterationManager`, which then "locks" the selected plan.
4. **Stateless Orchestrator:** The Orchestrator must execute the locked plan blindly once the system enters the `EXECUTING` state.
