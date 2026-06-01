# Unified Intent and Evolution Pipeline Specification

## 1. Pipeline Specification

The evolution pipeline has been unified into a single, cohesive flow within the `IterationManager` kernel.

### Flow Architecture:
1.  **Entry Point**: `IterationManager.handle(TaskRequest)` receives the user prompt.
2.  **Strategic Initialization**: Discovery and Analyzis stages grounding the session in the repository context.
3.  **Evolutionary Loop**: `IterationManager.evolve` starts the recursive cognition loop.
4.  **Intent Grounding (Phase: INTENT_EXPANSION)**:
    *   The loop's first phase is always `INTENT_EXPANSION`.
    *   `IntentExpansionEngine` is the sole authority for interpreting intent, identifying technical metadata, and discovering semantic dimensions.
    *   If the intent is ambiguous, the system enters `CLARIFY_USER` mode to seek grounding.
    *   Once intent is clear, the phase transitions linearly to `ARCHITECTURE_VARIANTS`.
5.  **Trajectory Evolution (Phases: ARCHITECTURE_VARIANTS to FINAL_SYNTHESIS)**:
    *   Competing trajectories are spawned based on the grounded intent.
    *   Recursive mutation and evolutionary pressure are applied only from this point forward.
    *   `StabilityAnalyzer` monitors architectural equilibrium to determine convergence.

## 2. Removed Dual-Path Logic Report

The following redundant or confusing logic paths have been eliminated to ensure a single source of truth:

*   **Redundant Clarification Engine**: `IntentClarificationEngine.java` has been deleted. Its redundant `clarify` method was removed from the initial grounding step in `IterationManager.evolve`.
*   **Bypassed Intent Grounding**: Removed the dual-handling where intent was "clarified" before entering the evolutionary loop and then "expanded" again inside it.
*   **Dimension-Driven Recursion Triggers**: Removed logic in `runDarwinIteration` that triggered early recursion or parallel branching based on semantic dimensions during intent expansion. Dimensions are now interpreted as context for architectural branching rather than triggers for the expansion phase itself.
*   **Clarification-as-Evolution Confusion**:
    *   Intent expansion no longer triggers `EvolutionaryTrajectoryEngine.evolve`, preventing premature application of evolutionary pressure.
    *   `StabilityAnalyzer` explicitly excludes the `INTENT_EXPANSION` phase from convergence logic, ensuring grounding is always completed before stability is assessed.
*   **Unified Phase Progression**: The `BRANCH_PARALLEL` strategy in the intent phase was removed in favor of a standard linear progression to architectural variants, where parallelism is natively handled by the Darwin engine.
