# Darwin Analysis — Evolutionary Milestone

## Goal
Understand how evolutionary candidates are born, mutated, evaluated, selected, and executed within the Darwin Engine.

---

## 1. Evolution Lifecycle
The Darwin evolution lifecycle is a multi-stage pipeline designed to ensure architectural divergence and technical grounding.

1.  **Territory Mapping**: `TrajectoryTerritoryMapper` scans the `TargetRealityModel` and identifies unexplored "technical quadrants."
2.  **Blueprint Discovery**: The mapper generates unique `TrajectoryBlueprint` objects.
3.  **Branch Spawning**: `DarwinVariantSpawner` materializes these blueprints into concrete technical proposals via isolated LLM calls.
4.  **Diversity Analysis**: `DarwinDiversityAnalyzer` ensures that candidates are conceptually distinct using vector-based distance calculations.
5.  **Fitness Ranking**: `DarwinFitnessRanker` scores candidates based on structural completeness and action specificity.
6.  **Decision Authority**: `DecisionResolver` (via `ActivationResolver`) selects the winner based on weighted policies and real-world signal feedback.
7.  **Execution**: The winning variant's actions are scheduled and executed.

---

## 2. Branch Lifecycle

### Candidate Creation
*   **Where do branches originate?**
    *   **Primary Source**: `TrajectoryTerritoryMapper.discoverNext()`. This is an LLM-driven discovery process that analyzes the `TargetRealityModel` and existing blueprints to find unexplored architectural paths.
    *   **Logic**: It's a **Prompt + templates** system. The mapper is provided with the project structure, goal, and reality model, and is instructed to generate exactly one unique JSON blueprint.
    *   **Fallbacks**: If the mapper fails to produce a unique set of blueprints (branching limit not met), `DarwinEngine` injects **hardcoded fallbacks**: `fallback-stabilization`, `fallback-mutation`, and `fallback-divergence`.

### Branch Materialization
*   **How does a blueprint become a Trajectory?**
    *   `DarwinVariantSpawner.spawnBlueprints()` takes a `TrajectoryBlueprint` and passes it to `buildBlueprintPrompt()`.
    *   **Injection**: `strategy_type` is injected directly from the blueprint into the prompt and the final JSON response.
    *   **Validation**: `DarwinVariantValidator.validate()` checks the LLM response for schema completeness and ensures it contains executable `actions`. If validation fails, the spawner attempts 3 retries before triggering an `autoRepair()` (deterministic bootstrap).

---

## 3. Diversity Lifecycle

### Diversity Calculation
*   **How is vector distance calculated?**
    *   `DarwinDiversityAnalyzer` maps the `engineering_dimensions` from the LLM response to a 10-dimensional `TrajectoryVector`.
    *   **Dimensions**: Modularity, Resilience, Architectural Depth, Service Orientation, Persistence, Determinism, Extensibility, Coupling, Abstraction, and Risk Acceptance.
    *   **Mapping logic**: Uses keyword matching on the dimension values (e.g., "high", "async", "reactive" => 0.9; "low", "atomic", "monolithic" => 0.1).
    *   **Distance**: A weighted Euclidean distance is used, normalized to the 0.0-1.0 range.

### Why did candidates collapse to dist=0.00?
*   **Vector Overlap**: If multiple LLM materializations use the same or semantically identical keywords for their `engineering_dimensions`, they result in identical `TrajectoryVector` points.
*   **Redundancy Penalty**: `DarwinDiversityAnalyzer.calculateDiversity()` applies a steep penalty: `if (getActionTargets(candidate).equals(getActionTargets(other))) { dist *= 0.5; }`.
*   **Root Cause**: When the LLM provides generic or repeated technical values (e.g., all variants defaulting to "medium" modularity), the conceptual distance collapses. If `dist < 0.05`, the system treats it as an exact duplicate and rejects the candidate.

---

## 4. Fitness & Selection Lifecycle

### Authority & Decision Path
*   **Why did fitness 0.93 lose against fitness 0.88?**
    *   The `score` field in the JSON is only the **raw LLM fitness**. The actual selection is governed by the `ActivationResolver` policy stack.
    *   **Hidden Scores / Boosts**:
        1.  **KEPT Boost**: If a variant is marked as `KEPT` (survivor from a previous round), it receives a score of `0.95+`.
        2.  **Progress Bias**: Variants with physical changes (`actions`) get a `+0.05` boost during implementation phases.
        3.  **Signal Boost**: `calculateSignalBoost()` integrates feedback from the `SignalBus`. If real-world evidence (tests, build logs) strongly supports a lower-fitness candidate, it can receive up to a `30%` total score weight boost.
    *   **Decision Path**: `ActivationResolver` aggregates scores from `SemanticCoherencePolicy`, `ComplexityCostPolicy`, `StabilityImpactPolicy`, `TrajectoryStabilityPolicy`, and `SignalBus` feedback. A high "raw" score can be crushed by poor historical reliability or high complexity costs.

### Synthesis Breakdown
*   **Why 1 active trajectory became 0 valid trajectories?**
    *   This usually occurs in the `DarwinDiversityAnalyzer.analyze()` filter.
    *   If the only materialized variant is rejected due to **SCHEMA_FATAL** (no actions) or **COLLAPSE_FATAL** (exact duplicate of a rejected sibling or ancestor), the list of unique variants becomes empty.
    *   This is a major clue that the LLM is failing to diverge from the "forbidden philosophies" or is failing to provide specific technical actions.

---

## 5. Failure Points & Architectural Bottlenecks
*   **Divergence Fatigue**: LLMs tend to gravitate toward "Probable Survivor" patterns, causing diversity collapse.
*   **Context Bloat**: As the `TargetRealityModel` grows, the prompt size for territory mapping increases, potentially degrading the quality of discovery.
*   **Signal Lag**: If the `SignalBus` is empty (e.g., first iteration), the system relies purely on heuristic policies, which may favor "safe" but suboptimal candidates.
