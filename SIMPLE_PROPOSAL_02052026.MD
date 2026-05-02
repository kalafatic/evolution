# FIX PROMPT: Simple Chat Flow Hardening

**Context:** Analysis (SIMPLE_ANALYSIS_02052026.MD) and Architectural Discussion (SIMPLE_AI-DISCUSSION_02052026.MD) established that even simple chat must follow the "state-transition kernel" model to avoid interpretation drift.

**Architectural Guardrails:**
The system is a closed-loop state transition machine, not an AI agent system. All behavior is governed by explicit state transitions rather than distributed reasoning across components.
- The **IterationManager** is the sole authority responsible for interpreting intelligence outputs, converting evaluation results into state transitions, and advancing the system through lifecycle phases.
- The **EvolutionOrchestrator** operates as a strictly stateless execution engine. It consumes only finalized, immutable plans and executes them deterministically without performing interpretation, planning, or evaluation.
- The **DarwinEngine** functions as an external intelligence module responsible solely for generating and evaluating candidate plans. Its outputs are treated as raw data.

**Task for AI Coder:**
Refactor the simple chat flow into a state-driven reactive path.

**Requirements:**
1. **State-Gated Entry:** Move ModeRouter logic and Greeting detection to a pre-orchestration phase that sets a global `CHAT_ACTIVE` state.
2. **Blind Execution:** Refactor `EvolutionOrchestrator.handle()` to return responses immediately if in `CHAT_ACTIVE` state, without initializing complex task contexts or reasoning loops.
3. **Transition Authority:** Ensure only the authority layer can transition the system out of conversational mode.
4. **Remove Semantic Interpretation:** The Orchestrator must not "decide" if a request is chat-worthy; it must only execute the response logic prescribed by the state.
