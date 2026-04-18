# SELF_DEV_MODE Test Steps

**Autonomy Level:** HIGH
**Iteration Limit:** 5 (Default)
**Self-Modification:** TRUE

## Overview
Autonomous self-modification mode. The `SelfDevSupervisor` manages a multi-iteration loop (default 5 iterations) using the Darwinian engine. Unlike other modes, this mode allows the system to modify its own source code within predefined `allowedPaths`.

## Test Steps
1. Open the **AiChatPage**.
2. Activate Self-Dev Mode:
   - Click the **"🚀 Self-Dev"** button.
   - Or check **"Self Iterative Development"** in the Instructions group.
   - Or include `mode: self-dev` in your prompt.
3. Provide a goal related to the platform itself (e.g., `"Optimize logging performance in the Evolution platform"`).
4. Monitor the supervisor's progress.

## Expected Behavior
- The log should display: `[SUPERVISOR] Starting Self-Development Session: ...`
- The system manages iterations: `[SUPERVISOR] Starting Iteration 1 of 5`.
- Modifications are permitted in `eu.kalafatic.evolution.controller/src` and other allowed paths.
- The `IterationManager` enforces safety by rolling back if an iteration breaks the build or fails tests.
- Automated Git commits are performed for successful iterations.

## Reference Tests
- `eu.kalafatic.evolution.controller.tests.DarwinEvolutionTest` (covers the underlying Darwin loop used by Self-Dev)
- `eu.kalafatic.evolution.controller.tests.ScenarioTest.testScenario4_PromptEvoIterative`
- `eu.kalafatic.evolution.controller.tests.ModeRouterTest.testExplicitSelfDevMode`
