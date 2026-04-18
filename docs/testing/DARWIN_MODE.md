# DARWIN_MODE Test Steps

**Autonomy Level:** MEDIUM
**Iteration Limit:** 3
**Self-Modification:** FALSE

## Overview
Competitive evolutionary solving. The `DarwinEngine` generates multiple competing "variants" (experimental Git branches) for each goal, evaluates them against build/test metrics, and merges only the fittest solution.

## Test Steps
1. Open the **AiChatPage**.
2. Activate Darwin Mode:
   - Check the **"Darwin Mode"** setting in the Properties Page.
   - Or include `mode: darwin` in your prompt.
3. Provide a complex goal (e.g., `"Refactor the EvolutionOrchestrator to reduce complexity"`).
4. Monitor the logs for variant generation.

## Expected Behavior
- The log should display: `[DARWIN] Generating variants for goal: ...`
- `DarwinEngine` should produce 2-3 structured proposals with hypotheses.
- The system creates temporary branches with the `exp/` prefix (e.g., `exp/refactor/v1`).
- Each variant is scored based on Build Success (20%), Test Pass Rate (50%), and Coverage (10%).
- The fittest variant is merged, and experimental branches are deleted.

## Reference Tests
- `eu.kalafatic.evolution.controller.tests.DarwinEvolutionTest`
- `eu.kalafatic.evolution.controller.tests.ScenarioTest.testScenario3_PromptEvo`
