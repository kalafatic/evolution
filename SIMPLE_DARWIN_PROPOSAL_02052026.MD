# FIX PROMPT: Simple Darwin Flow Unification

**Context:** Analysis (SIMPLE_DARWIN_ANALYSIS_02052026.MD) found a functional discrepancy between Darwin mode in the Chat UI and the Iteration Manager. The Chat UI currently uses a single PEV loop instead of the robust branch-and-select strategy.

**Task for AI Coder:**
Unify the Darwin execution logic to use `IterationManager` regardless of where it is triggered.

**Requirements:**
1. **Refactor AiChatPage:** Modify `startSelfDevAction` in `AiChatPage.java` to always utilize the `SelfDevSupervisor` and `IterationManager` when `DarwinMode` is active.
2. **Standardize Task Context:** Ensure the `TaskContext` correctly propagates the "Single Task" intent to the `IterationManager` so it can run a one-off Darwin cycle instead of an infinite loop.
3. **Bridge Orchestrator:** If the Orchestrator receives a request with Darwin mode enabled, it should ideally delegate to a local instance of `IterationManager` instead of running its internal `executeTaskWithRetries` logic directly.
