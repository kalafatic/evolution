# FIX PROMPT: Darwin Iteration Optimization

**Context:** Analysis (ITERATION_DARWIN_TRACE_ANALYSIS_02052026.MD) and Architectural Discussion (ITERATION_DARWIN_TRACE_AI-DISCUSSION_02052026.MD) revealed redundant intent classification and tactical planning. The system must transition to a hardened state-transition kernel model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- The **IterationManager** is the sole authority responsible for interpreting intelligence outputs, converting evaluation results into state transitions, and advancing the system through lifecycle phases.
- The **EvolutionOrchestrator** operates as a strictly stateless execution engine. It consumes only finalized, immutable plans and executes them deterministically without performing interpretation, planning, or evaluation.
- The **DarwinEngine** functions as an external intelligence module responsible solely for generating and evaluating candidate plans. Its outputs are treated as raw data.

**Task for AI Coder:**
Refactor the Darwin execution flow to adhere to these guardrails.

**Requirements:**
1. **Bypass Orchestrator Analysis:** Modify `EvolutionOrchestrator.handle()` to act as a blind executor. If the system state is `EXECUTING`, it must consume the immutable plan without performing any intent classification or analysis.
2. **Centralize Transitions:** Ensure `IterationManager` is the ONLY component allowed to advance system state.
3. **Eliminate Redundant Evaluation:** Reuse variant evaluation results as the final result instead of re-running evaluation after merge.
4. **Decouple Reasoning:** Prohibit the Orchestrator from semantically interpreting task intent once execution has started.
