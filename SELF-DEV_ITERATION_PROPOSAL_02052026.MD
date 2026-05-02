# FIX PROMPT: Self-Dev Iterative Context Propagation

**Context:** Analysis (SELF-DEV_ITERATION_ANALYSIS_02052026.MD) shows triple-layered planning where the Supervisor, TaskPlanner, and Orchestrator all perform redundant analysis of the same request.

**Task for AI Coder:**
Implement a consolidated context propagation mechanism.

**Requirements:**
1. **Context Sharing:** Create an `AnalysisContext` object that stores the results of the initial `AnalyticAgent.analyze()` call at the Supervisor level.
2. **Handoff:** Pass this `AnalysisContext` into the `IterationManager` and then into the `TaskExecutor`.
3. **Orchestrator Injection:** Modify `EvolutionOrchestrator.handle()` to accept an optional `AnalysisContext`. If present, skip all initial classification and analysis steps and use the pre-provided category, objective, and refined prompt.
4. **Consistency:** Ensure that any user clarifications received at the Supervisor level are appended to the `AnalysisContext`.
