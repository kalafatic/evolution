# Darwin Architecture Diagnostic Report

I. FAILURE RANKING

1.  **Diversity Evaluation Layer** (Shallow mapping and late-stage detection)
2.  **Territory Discovery Layer** (Root source of semantic re-labeling and duplication)
3.  **Fallback / Recovery System Contamination** (Injection of hardcoded operators into candidate pool)
4.  **Variant Materialization Layer** (LLM template collapse and engineering dimension repetition)
5.  **Fitness / Authority Selection Layer** (Policy-driven decision overrides and hidden biases)
6.  **Mediated Export / Lineage Loss** (Loss of historical reasoning granularity)
7.  **Blueprint Generation Layer** (Downstream constraint and bias injection)

II. PRIMARY FAILURE POINT

**Early Pipeline Diversity Collapse**: Meaningful divergence is lost at the **Territory Discovery Layer** due to semantic re-labeling. While the mapper attempts to identify unique "technical quadrants," the resulting blueprints frequently converge on the same architectural assumptions. This is then exacerbated by the **Diversity Evaluation Layer** using a shallow keyword-based vector model that fails to detect subtle semantic overlap before materialization.

III. SECONDARY COUPLED FAILURES

1.  **Fallback Contamination**: Deterministic recovery entities (e.g., `fallback-divergence`) are injected into the active candidate pool and treated as competitive variants, diluting evolutionary pressure and contaminating the selection process.
2.  **Materialization Template Collapse**: The **Variant Materialization Layer** frequently defaults to a "Medium" value across all 10 engineering dimensions, causing a mathematical collapse in the `TrajectoryVector` space (`dist=0.00`).
3.  **Authority Selection Bias**: Selection is heavily influenced by **KEPT boosts (0.95+)** and **Signal Boosts**, which consistentely override higher-fitness "discovery" candidates in favor of historical survivors or safe fallbacks.

IV. EVIDENCE SUMMARY

*   **Logs**: `[DIVERSITY] COLLAPSE FATAL: dist=0.00` is frequently triggered in `DarwinDiversityAnalyzer` because variants share identical `engineering_dimensions` (e.g., modularity="medium", resilience="standard").
*   **Observed Behavior**: `DarwinEngine.java` injects `fallback-stabilization` and `fallback-divergence` directly into the blueprint list when discovery fails to reach the branching limit, treating them as valid candidates in the selection lifecycle.
*   **Architectural Flow**: `TrajectoryTerritoryMapper` discovers trajectories sequentially, but its uniqueness check is limited to string-based strategy/philosophy comparison, allowing semantic duplicates to pass into the resource-intensive materialization stage.
*   **Authority Logic**: `ActivationResolver.java` implements a hardcoded `0.9+` boost for variants with `ActivationState.KEPT`, effectively bypassing multi-policy competition.

V. CONFIDENCE SCORES

*   Territory: 0.85
*   Blueprint: 0.65
*   Variant: 0.75
*   Diversity: 0.95
*   Fitness: 0.80
*   Fallback contamination: 0.85
*   Mediation loss: 0.70
