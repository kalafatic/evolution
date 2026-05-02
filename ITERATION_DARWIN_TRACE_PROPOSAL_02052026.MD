# FIX PROMPT: Darwin Iteration Optimization

**Context:** Analysis (ITERATION_DARWIN_TRACE_ANALYSIS_02052026.MD) revealed redundant intent classification and tactical planning in `EvolutionOrchestrator` for tasks already structured by the Darwin Engine. It also identified a redundant final evaluation.

**Task for AI Coder:**
Refactor the Darwin execution flow to eliminate redundant processing.

**Requirements:**
1. **Bypass Orchestrator Analysis:** Modify `EvolutionOrchestrator.handle()` to accept a `preValidated` flag in `TaskRequest`. If true, skip `intentClassifier.classify()` and `analyticAgent.analyze()`.
2. **Reuse Strategy:** In `TaskExecutor.executeTasks()`, set the `preValidated` flag to true when executing tasks derived from `BranchVariant` actions.
3. **Eliminate Redundant Evaluation:** In `IterationManager.runDarwin()`, remove the final `evaluator.evaluate()` call at the end of the MUTATE phase if the `bestVariant` score is already available and valid. The merge result on the main branch should be considered successful if the variant evaluation was successful.
4. **Consistency:** Ensure `IterationStatus` is updated correctly after skipping the redundant evaluation.
