# FIX PROMPT: Darwin Iteration Optimization

**Context:** Analysis (ITERATION_DARWIN_TRACE_ANALYSIS_02052026.MD) revealed redundant intent classification and tactical planning in `EvolutionOrchestrator` for tasks already structured by the Darwin Engine. It also identified a redundant final evaluation. Architectural Discussion (ITERATION_DARWIN_AI-DISCUSSION_02052026.MD) established that the system must move to a "state-transition kernel" model.

**Task for AI Coder:**
Refactor the Darwin execution flow into a deterministic state-transition model where the `IterationManager` is the sole Transition Authority.

**Requirements:**
1. **Centralize Control:** Ensure `IterationManager` is the ONLY component allowed to advance system state (e.g., PLAN -> EXECUTE).
2. **Stateless Execution:** Refactor `EvolutionOrchestrator.handle()` to act as a blind executor. If the system state is `EXECUTING`, it must consume the immutable plan without performing any intent classification (`intentClassifier.classify()`) or analysis (`analyticAgent.analyze()`).
3. **Intelligence Decoupling:** Treat `DarwinEngine` outputs as raw data. The `IterationManager` must be the layer that converts these proposals into a finalized plan and triggers the state transition to `EXECUTING`.
4. **Eliminate Redundant Evaluation:** In `IterationManager.runDarwin()`, reuse the variant's evaluation score as the final result instead of re-running `evaluator.evaluate()` after the merge. The `IterationManager` should advance the state to `DONE` based on the previously validated variant score.
