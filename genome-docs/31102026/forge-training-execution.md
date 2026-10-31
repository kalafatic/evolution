# EVO Forge Model Training Execution & Telemetry

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/forge-training-execution.md`

---

## 1. Authoritative Orchestration & Lifecycle

`ForgeOrchestratorImpl` (`eu.kalafatic.evolution.forge.controller`) drives model training across 6 defined stages:
1. `ANALYZING`: `ForgeSourceAnalyzer` inspects heterogeneous input datasets.
2. `PREPARING`: `ForgeDatasetComposer` derives token budgets and quality distributions.
3. `VALIDATING_PREFLIGHT`: `ForgePreflightValidator` validates source availability, memory thresholds, and disk space.
4. `TRAINING`: `EvoLlmTrainer` executes multi-epoch backpropagation, reporting elapsed duration, loss, and validation loss through `ProgressListener`.
5. `EVALUATING`: Validation evaluation using real cross-entropy token loss.
6. `FINALIZING`: Checkpoint persistence (`EvoModelArtifact`) and native inference smoke testing (`ReferenceEvoInferenceEngine`).
