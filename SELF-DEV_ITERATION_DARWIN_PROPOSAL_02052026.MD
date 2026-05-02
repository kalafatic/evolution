# FIX PROMPT: Self-Dev Darwin Redundancy Removal

**Context:** Analysis (SELF-DEV_ITERATION_DARWIN_ANALYSIS_02052026.MD) identified that the final evaluation in `IterationManager.runDarwin()` is redundant because the fittest variant was already evaluated in a worktree.

**Task for AI Coder:**
Remove the redundant final evaluation in the Darwinian flow.

**Requirements:**
1. **Reuse Variant Score:** In `IterationManager.runDarwin()`, instead of calling `evaluator.evaluate()` after the `gitManager.merge()`, reuse the `EvaluationResult` from the `bestVariant`.
2. **Sanity Check (Optional):** Replace the full `mvn clean install` evaluation with a lightweight sanity check (e.g., check if the merge resulted in conflicts) to save significant time.
3. **Commit Metadata:** Include the original variant score and strategy in the final commit message to maintain traceability.
