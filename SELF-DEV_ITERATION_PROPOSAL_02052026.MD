# FIX PROMPT: Self-Dev Iterative Context Propagation

**Context:** Analysis (SELF-DEV_ITERATION_ANALYSIS_02052026.MD) and Architectural Discussion (SELF-DEV_ITERATION_AI-DISCUSSION_02052026.MD) identified "triple-layered planning" as a major bottleneck. The system must move to a hardened state-transition kernel model.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- The **IterationManager** is the sole authority responsible for interpreting intelligence outputs, converting evaluation results into state transitions, and advancing the system through lifecycle phases.
- The **EvolutionOrchestrator** operates as a strictly stateless execution engine. It consumes only finalized, immutable plans and executes them deterministically without performing interpretation, planning, or evaluation.
- The **DarwinEngine** functions as an external intelligence module responsible solely for generating and evaluating candidate plans. Its outputs are treated as raw data.

**Task for AI Coder:**
Implement hardened state gating and plan handover for the Self-Dev flow.

**Requirements:**
1. **Transition Authority:** Ensure `IterationManager` is the ONLY component permitted to convert Supervisor analysis into state transitions.
2. **Decouple Intelligence:** Supervisor and TaskPlanner outputs must be treated as raw data inputs for the `IterationManager`.
3. **Stateless Orchestrator:** The Orchestrator must blindly execute the plan once `IterationManager` advances the state to `EXECUTING`.
4. **Remove Recursive Interpretation:** Prohibit any semantic interpretation of the Supervisor's intent within the execution layer.
