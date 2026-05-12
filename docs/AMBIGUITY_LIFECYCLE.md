# Ambiguity Lifecycle and Clarification Flow

## Lifecycle of a Prompt

1.  **Received:** Raw user input is captured.
2.  **Expansion:** `IntentExpansionEngine` decomposes the input into `IntentDimensions`.
3.  **Analysis:** Each dimension is scored for ambiguity.
4.  **Hypothesis Generation:** Coherent combinations of dimension values are formed into `IntentHypotheses`.
5.  **Planning:** `ClarificationPlanner` selects a strategy based on ambiguity scores and overall confidence.
6.  **Resolution:**
    - If `CLARIFY_USER`, a structured question is presented.
    - If `BRANCH_PARALLEL`, multiple `BranchVariants` are seeded from the hypotheses.
    - If `AUTO_INFER`, the highest-confidence hypothesis is selected as the primary objective.
7.  **Mutation:** Darwin Engine generates implementation actions based on the resolved intent.

## Example Flow: "create java class which can print text"

### Detected Dimensions:
- **Target:** Console, File, Logger, or UI? (Ambiguity: 0.8)
- **Style:** Static Utility vs. Instance Service? (Ambiguity: 0.4)

### Hypotheses:
- **Hypothesis A:** Console-based static utility (Simple/Beginner).
- **Hypothesis B:** Logger-based service (Production/Enterprise).

### Clarification Strategy:
- **Strategy:** `CLARIFY_USER`
- **Question:** "Should the class print to the console, a logger, or a file?"

## Migration Notes

- **Old Flow:** `IntentAnalyzer` produced a single flat goal. Darwin mutated immediately.
- **New Flow:** `IntentExpansionEngine` produces multiple hypotheses. Darwin seeds from these hypotheses.
- **Data:** `OrchestrationState` now holds `IntentExpansionResult` in metadata.
