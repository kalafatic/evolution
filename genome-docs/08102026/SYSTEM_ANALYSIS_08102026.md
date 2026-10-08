# Timestamped System Analysis Report — 08102026

## 1. Overview
This timestamped analytic document records the complete reverse-engineered system snapshot of the **EVO Platform** as of October 8, 2026 (`08102026`).

## 2. Core Subsystems & Responsibilities

### Kernel & Orchestration (`eu.kalafatic.evolution.controller`)
* **`ResourceManager`**: Single authoritative persistent configuration accessor over EMF resource `evolution.ecore`. Enforces canonical filesystem rules (`validateCanonicalPath`).
* **`CognitiveLoopEngine`**: Adaptive goal-oriented controller executing `CognitiveGoal` objectives using decoupled strategy pools.
* **`SelfDevOrchestrator`**: Task engine running the 6-stage self-evolution loop (`GIT_CHECK` -> `COPY` -> `BUILD` -> `EXPORT` -> `START` -> `VERIFY`).
* **`RuntimeEventBus`**: Throttled (100ms) pub/sub event bus broadcasting `RuntimeEvent` notifications across category channels.

### Forge LLM Engine (`eu.kalafatic.evolution.forge.*`)
* **`HuggingFaceDatasetSource`**: Multi-tier HTTP fallback downloader (Rows API -> Parquet API -> Hub Repository Tree) with redirect streaming (up to 5 hops).
* **`DatasetDeduplicator`**: MinHash 5-gram token shingle fingerprinting for documents >= 150 chars/15 words; exact SHA-256 for shorter text.
* **`SimpleBPETokenizer`**: BPE tokenizer training directly on text corpus without synthetic placeholders.
* **`EvoLlmTrainer`**: Native Java neural network trainer supporting `TrainingProfile.EVO_FAST` and prompt loss masking.
* **`ForgeOrchestratorImpl`**: Controller coordinating `ForgeJob` states (`ANALYZING` -> `PREPARING` -> `TRAINING` -> `EVALUATING` -> `FINALIZING`).

### Self-Dev & Genome (`eu.kalafatic.evolution.selfdev.genome`)
* **`SelfDevGenomeHub`**: Central repository for project snapshots, milestone records, and upgrade plans.
* **`LocalGenomeRepository`**: Persists structural genome artifacts.
* **`SecondhandUpgradeEngine`**: Compiles architectural change proposals (`ArchitecturalChange`) across evolutionary cycles.

### Process Supervisor (`eu.kalafatic.evolution.supervisor`)
* **`SupervisorMain`**: Dual-mode launcher running headless or RCP UI (`SupervisorUiWindow`).
* **`EVOSupervisorServer`**: REST server running on port 8089 (or 8099 in DEBUG isolation mode).
* **`EVOSupervisorControlServer`**: Control plane running on port 28080 (or 28090 in DEBUG isolation mode).

## 3. Storage & Artifact Specifications
* **`.evodata`**: Binary Zip container holding `manifest.json`, `train.jsonl`, `val.jsonl`.
* **`.evo`**: Native LLM model artifact storing `EvoLlmArchitecture`, `EvoTokenizerArtifact`, and trained float tensor weights.
* **`GGUF`**: Model export format validated via `LlamaCppRunner` before Ollama registration.
