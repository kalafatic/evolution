# ASSISTED_CODING Mode Test Steps

**Autonomy Level:** LOW
**Iteration Limit:** 2
**Self-Modification:** FALSE

## Overview
Guided multi-step tasks where the user maintains control over the plan and execution. The system decomposes the request into atomic `Task` objects (file, maven, git, shell) and pauses for user review.

## Test Steps
1. Open the **AiChatPage**.
2. Enable iterative development:
   - Check the **"Iterative Development"** checkbox in the Instructions group.
   - Or include `mode: assisted` in your prompt.
3. Enter a coding request (e.g., `"Implement Javadoc for the model package"`).
4. When the indicator changes to **WAITING FOR USER APPROVAL**, switch to the **Approval Page**.
5. Review the proposed tasks, reorder them if needed, and click **Approve & Apply**.

## Expected Behavior
- The log should display: `Evo-Orchestrator-Planning: Plan generated. Waiting for user review...`
- The system pauses execution until the plan is approved.
- Tasks are executed sequentially, updating their status from `PENDING` (Grey) to `RUNNING` (Yellow) to `DONE` (Green).
- The `PlannerAgent` generates a straightforward sequence without competitive variants.

## Reference Tests
- `eu.kalafatic.evolution.controller.tests.ScenarioTest.testScenario2_PromptIterative`
