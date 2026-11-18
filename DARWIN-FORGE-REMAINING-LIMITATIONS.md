# DARWIN FORGE — REMAINING LIMITATIONS

Date: 2026-11-17 (Phase D). Every item below is a documented, honest limitation — none is hidden behind a fallback or reported as functional.

## 1. GPU execution — NOT IMPLEMENTED

- No CUDA kernels, no GPU memory allocation, no tensor transfers, no kernel launches exist anywhere in the codebase. `cuInit(0)` verification succeeds on this machine (RTX 5070 Ti) but proves driver initialization only.
- Training and native inference run as CPU float32 Java.
- Honest labels in place: `CUDA_DRIVER_VERIFIED` (driver init verified), `CPU_HOST_JAVA (CUDA DRIVER PRESENT)`, and the GPU module refuses to produce model artifacts.
- Impact: ~7 samples/s training throughput measured in the E2E run; production-scale training is impractical until a genuine GPU backend exists (out of scope for this initiative).

## 2. External GGUF numerics — RoPE vs sinusoidal mismatch

- The exported GGUF declares `llama` architecture with RoPE metadata; the native model uses additive sinusoidal absolute positional encoding (correctly declared as `sinusoidal_abs` in `.evo`).
- llama.cpp/Ollama apply RoPE at inference, so external outputs are NOT numerically comparable to the native engine. Outputs remain structurally valid (both runtimes load and execute the models without errors).

## 3. ENR runtime boundary

- ENR still `Require-Bundle`s `eu.kalafatic.evolution.controller` → the full Eclipse RCP/SWT closure is pulled in; `EnrMain`'s standalone claim remains aspirational (G-3). A pure-Java SPI split + bridge bundle is designed in the baseline but not implemented.
- No shared VRAM/RAM allocator across providers; the ENR-local admission-control semaphore (WP8) bounds concurrent requests, but there is no memory-pressure eviction, per-model residency tracking for native models, or GPU VRAM scheduling (nothing to schedule while inference is CPU-only).
- llama.cpp path now honors request temperature and maxTokens-based context length (WP8); per-model instance configuration objects are not yet implemented.

## 4. Early stopping / training config consumption

- `earlyStoppingEnabled` / `targetLossThreshold` / `timeBudgetHours` / `warmupSteps` configuration fields remain unconsumed by the trainer (warmup is fixed at 3% of steps). Documented, not faked.

## 5. Quantization

- F16/BF16/INT8: real compact storage in `.evo` (verified). INT4: explicitly rejected (`UnsupportedOperationException`) — packed nibble storage not implemented.
- GGUF export always writes `general.file_type = ALL_F32`; quantized GGUF tensor types are not produced.

## 6. Dataset format scale

- `.evodata` loads datasets fully into RAM (single `data.jsonl` entry); no sharding, no random-access index, no streaming writer. Fine for current dataset sizes (≤ 100 MB), limiting for very large corpora.
- Token counts in `.evodata` are heuristic estimates (words × 1.3); datasets are stored as raw text and re-tokenized at training time.

## 7. Parquet

- Only the heuristic `ParquetAdapter` (string harvester) exists; no Parquet codec library is used. Decision (WP10): KEEP the heuristic adapter as a best-effort fallback; introduce a real codec library only when a concrete dataset requirement demands it (see WP10 rationale in the final report). Parquet fidelity for dictionary/ZSTD/nested encodings is not guaranteed.

## 8. Legacy `.evo` integrity policy

- Files written by earlier pipeline revisions may carry vocab sections with duplicate JSON keys (tolerated, last-wins) and content hashes computed by an unreproducible algorithm (per-tensor SHA-256 checksums remain enforced; the legacy content hash is informational only). New files carry the full digest in metadata and get exact verification.

## 9. Telemetry coverage

- All fabricated telemetry removed (WP1); remaining gaps: `progressPercent` derives from step/token counters (no separate job-level percent channel for non-training stages), `learningRate` is not yet plumbed from the optimizer into telemetry JSON (reported as -1/unavailable), and accuracy is not measured anywhere (UI shows '—').

## 10. Forge-lab prototype

- `forge-lab/` remains an out-of-scope prototype with stub implementations (`EvolutionaryTrainer.step()`, `XmiForgeRepository`, `ExporterControllerImpl`) — separate from the production bundles; not part of the Tycho aggregator.

## 11. E2E repeatability of the full run

- The complete 13-stage lifecycle ran once end-to-end (905 s) and was externally verified (Ollama + llama-cli). A second full run was not completed within the session budget; deterministic stage behaviors (dataset byte-reproducibility, bit-exact checkpoint resume, artifact integrity) are covered by the 297-test regression suite.
