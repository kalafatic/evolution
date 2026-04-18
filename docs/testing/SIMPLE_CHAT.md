# SIMPLE_CHAT Mode Test Steps

**Autonomy Level:** LOW
**Iteration Limit:** 1
**Self-Modification:** FALSE

## Overview
Direct interaction with the AI without triggering background orchestration or code changes. The `EvolutionOrchestrator` bypasses the planning loop and dispatches the request directly to the `GeneralAgent`.

## Test Steps
1. Open the **AiChatPage**.
2. Enter a general query (e.g., `"How does the Evolution platform handle multiple modes?"`).
3. Alternatively, explicitly force the mode by typing `mode: chat` followed by your request.
4. Observe the logs in the chat view.

## Expected Behavior
- The log should display: `Evo-Orchestrator-Mode: SIMPLE_CHAT detected. Bypassing orchestration loop.`
- The `GeneralAgent` should provide a text-only response.
- No tasks should appear in the **Approval Page** or **Tasks** list.
- No files should be modified.

## Reference Tests
- `eu.kalafatic.evolution.controller.tests.ScenarioTest.testScenario1_SimplePrompt`
