# Intent Expansion Architecture

## Overview

The Intent Expansion Engine is a critical addition to the Darwin Orchestration Kernel. It addresses the issue of "implementation-first" behavior for ambiguous prompts by introducing a structured exploration of the intent space before any implementation variants are generated.

## Core Components

### 1. IntentExpansionEngine
- **Role:** Analyzes the raw user prompt for ambiguity.
- **Output:** `IntentExpansionResult` containing dimensions of ambiguity and coherent hypotheses.
- **Constraints:** Must not generate code or modify state.

### 2. IntentDimension
- Represents an unresolved axis of intent (e.g., Output Type, Runtime Behavior).
- Tracks confidence and ambiguity scores for each dimension.

### 3. IntentHypothesis
- A coherent interpretation of the user request.
- Seeds the Darwin evolutionary process with structured engineering objectives.

### 4. ClarificationPlanner
- Determines the strategy for handling ambiguity:
  - **AUTO_INFER:** Low risk, proceed with assumptions.
  - **BRANCH_PARALLEL:** Medium risk, create parallel Darwin branches for different hypotheses.
  - **CLARIFY_USER:** High risk, ask the user for more information.

## Integration in DarwinFlow

The engine is integrated into the `PHASE_INTENT_EXPANSION` phase of `DarwinFlow`.

```text
User Request
    ↓
IntentExpansionEngine
    ↓
IntentExpansionResult (Dimensions & Hypotheses)
    ↓
ClarificationPlanner
    ↓
[Clarification Request?] → (User Input) → (Restart Expansion)
    ↓
Darwin Mutation (Seeds from Hypotheses)
    ↓
...
```

## Benefits

- **Stability:** Simple prompts become more deterministic.
- **Efficiency:** Reduces unnecessary branching by consolidating assumptions early.
- **Transparency:** Ambiguity becomes a first-class citizen in the orchestration trace.
- **LLM Performance:** Small local LLMs perform better with structured guidance.
