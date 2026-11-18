# DARWIN FORGE — FINAL STABILIZATION REPORT

Date: 2026-11-17. Covers the completion phase (WP0-WP14) executed on top of the earlier audit/refactoring initiative. All claims below distinguish what was actually verified.

## What was done this phase (implementation summary)

**WP1 — Telemetry honesty (all six defects + newly found fabrications fixed)**
- Forge legacy controllers (`TrainingControllerImpl`, `ModelControllerImpl`, `DatasetControllerImpl`, `SnapshotControllerImpl`, `ExporterControllerImpl`): fabricated fallbacks replaced with honest `available=false`/empty/refusal responses.
- `ForgeOrchestratorImpl` evaluation: real `trainResult` values only (no 0.05/1.05/`overfittingDetected=false` fabrications); qualityScore = -1 (not computed) documented.
- `SelfEvoForgingServiceImpl`: real job-driven stats via progress poller + `stopForging` actually stops (ForgeSessionManager.requestFinish); state mapping from `ForgeJob.JobState`.
- `ForgeOrchestratorImpl` progress listener: `onProgressMetrics` maps real optimizer steps into the 60-100% band.
- `TrainingTelemetry.toJsonObject`: computed `progressPercent`, UI aliases (`trainingLoss`, `validationLoss`, `stepsPerSecond`, `estimatedRemainingMillis`), honest `learningRate=-1`.
- `runE2EDemo`: simulated training + DUMMY GGUF removed; honest notification only.
- `LLMDarwinEngine`: 2.5/10.0 sentinels removed; no-loss-history and non-finite losses now fail through the documented safe-penalty path; valLoss is NaN (not trainLoss) when no validation split.
- UI (`viz-interactive-demos.js`, `viz-self-evo.js`, `viz-evolution.js`, `viz-training.js`, `forge.html`): simulated values labeled as SIMULATION or replaced with honest '—'/error states; unverified Ollama claims removed.
- Regression: `TelemetryHonestyRegressionTest` (9 tests).

**WP2 — Acquisition continuation, amplification, recordization, cancellation**
- `TrainingDataAcquisitionServiceImpl`: coverage-gated bounded amplification for explicit sources (gate 30%, cap 2× first-pass yield, marked `#amplified` copies, dedup-bypassed); cooperative cancellation (flag → `Status.CANCELLED` with partial results); fixed `consecutiveEmptyExpansions` dead logic untouched but documented.
- `LocalDatasetSource`: hybrid recordization — lines ≤ 3 chars become rejectable records; remaining content keeps whole-file merge semantics.
- `HuggingFaceDatasetSource`: `next()` now counts accepted records; instruction-branch no longer swallows prompt/response rows; **preflight split-resolution argument swap fixed** (`resolvedSplit`/`requestedSplit` — a real defect).
- Regression: `AcquisitionContinuationRegressionTest` (5 tests); the 4 previously failing tests now pass at production level; 3 infinite mock downloaders in HF tests made one-shot (test defects, not weakened).

**WP3/WP4/WP5 — Dataset, checkpoint and artifact verification**
- `.evodata`: legacy v1.0 loading verified; `validateTokenizerCompatibility` added (type + vocab) and wired into `ForgeOrchestratorImpl` before training; failed saves leave no `.tmp`.
- Trainer: **epoch shuffles now apply to a canonical order** — interrupted+resumed training is bit-exact vs uninterrupted (0 differing floats across all tensors; previously epochs compounded shuffles). Real defect found and fixed.
- `.evo`: sampling configuration persists in binary-form metadata and restores on load; trained-model export/reload logits equality verified.
- `EvoModelReader`: duplicate-tolerant vocab parsing (real trained artifacts contain duplicate vocab keys) + tiered integrity (per-tensor checksums always enforced; content hash exact for current-algorithm files, informational for legacy).
- Regression: `TrainingAndArtifactLifecycleVerificationTest` (5 tests).

**WP9 — Test isolation (I-2)**
- `ProjectModelManager` artifact-resolver seam (set/reset for hermetic tests); `InferenceEngineSwitchingTest` now hermetic and passes individually, in-suite, repeatedly.
- `LlamaServiceTest` polluter fixed via `evo.test.modelDirsOverride` — fixture copies can no longer reach real user model directories.
- Real defect fixed: `LlamaService.resolveEvoModelPath` unconditionally resolved ANY unknown model name to `~/.ollama/models/evo.gguf` / workspace forge-output artifacts; default-model fallbacks are now gated on evo-variant names.

**WP7 — External GGUF verification (real models)**
- Real trained model (59.2M params, 218MB `.evo`) → GGUF (237MB) → loaded and executed by **llama-cli** and **Ollama** (independent runtimes), no errors.
- E2E-trained model → GGUF → **Ollama generates text consistent with its training data** (`user:`/`assistant:` structure from OASST).

**WP8 — ENR hardening**
- Admission-control semaphore (`evo.enr.maxConcurrent`, default CPUs/2) with honest 429 rejection for chat and chat/stream.
- SSE client-disconnect cancellation (replaces hardcoded `() -> false`); native stream path propagates cancellation into the engine.
- llama.cpp path honors request temperature and maxTokens-driven context length.

**WP11 — Complete real-data end-to-end experiment**
- See `DARWIN-FORGE-END-TO-END-ACCEPTANCE-REPORT.md`: 13 stages, real OASST dataset, real training (loss 4.38→3.36), real interruption + resume, real `.evo` + GGUF exports, external Ollama + llama-cli execution. **PASS.**

**WP12 — Final regression battery**
- Tycho OSGi suite: **297/297 tests PASS** (43 classes, incl. 24 new regression tests across 4 new classes).
- GPU module (standalone Maven): **4/4 PASS** (honest diagnostics incl. real `cuInit` verification).
- Build: all affected bundles (forge.math/model/tokenizer/data/trainer/controller/agent.api, controller, enr, utils) compile and install via Tycho offline build.

**WP10 — Parquet decision**
- DECISION: keep the heuristic `ParquetAdapter` as a best-effort extraction fallback; do NOT add a Parquet codec dependency now. Rationale: (a) no current EVO dataset requirement depends on Parquet fidelity — HF acquisition prefers the rows API with Parquet-API fallback for raw downloads; (b) the heuristic adapter is memory-safe (bounded-window scan, `ParquetAdapterMemoryTest`) and useful for degraded recovery; (c) introducing a real codec (e.g. parquet-avro/arrow) adds a heavyweight dependency for a hypothetical workload. Revisit when a concrete Parquet-only dataset enters the training corpus; then benchmark codecs (ZSTD/dictionary fidelity) on representative EVO data before adding the dependency.

**WP6 — GPU truth**
- Documented in `DARWIN-FORGE-REMAINING-LIMITATIONS.md` §1: no GPU execution exists; `cuInit` verification is driver-level only; labels are honest. No fake-GPU claims anywhere.

## Final assessment (WP14)

| Area | Assessment |
|---|---|
| Data acquisition | **INTEGRATION-TESTED** — continuation/amplification/cancellation fixed and regression-tested; verified on real OASST data (E2E stage 1) |
| Dataset preparation | **END-TO-END-VERIFIED** — integrity, tokenizer identity, reproducibility (byte-deterministic), legacy 1.0 compat |
| Native training | **END-TO-END-VERIFIED** — real measured loss decrease on real data; masks/LR/optimizer correctness regression-tested |
| Checkpointing | **END-TO-END-VERIFIED** — real interruption + resume in E2E; bit-exact continuity test (0 differing floats) |
| `.evodata` | **INTEGRATION-TESTED** — SHA-256 integrity, CORRUPTED detection, version validation, legacy loading |
| `.evo` | **INTEGRATION-TESTED** — integrity-gated load, sampling config round-trip, trained-weights fidelity, quantized dtypes |
| GGUF export | **EXTERNALLY-VERIFIED** — llama-cli + Ollama load and execute real exported models; RoPE/sinusoidal numeric caveat documented |
| CUDA | **NOT IMPLEMENTED** — driver verification only; honestly labeled; no fake GPU claims |
| ENR | **INTEGRATION-TESTED** — REST/SSE/cancellation/admission control; RCP dependency (G-3) and resource-manager scope (G-4) remain documented limitations |
| Multi-model | **PARTIAL** — provider dispatch + bounded concurrency; per-model instance configs and VRAM accounting not implemented |
| Telemetry | **INTEGRATION-TESTED** — all fabrications removed, regression-tested; honest unavailable markers |
| RCP | **INTEGRATION-TESTED** — product bundles build; forge UI consumes honest data; no RCP workflow regressions in the suite |
| E2E | **END-TO-END-VERIFIED** — 13-stage real-data lifecycle, externally confirmed (Ollama/llama-cli) |

## Final decision

# ACCEPTED WITH LIMITATIONS

Justification:
- ACCEPTED — the complete native lifecycle (real data → `.evodata` → real training → checkpoint/resume → `.evo` → GGUF → independent external inference) executes correctly end-to-end and was externally verified; the 297-test deterministic suite plus 4 GPU tests are green; all telemetry fabrications are removed.
- WITH LIMITATIONS — GPU execution is not implemented (CPU-only; throughput ~7 samples/s); external GGUF numerics differ from native (RoPE vs sinusoidal); ENR retains the RCP dependency and lacks per-model resource management; early-stopping config remains unconsumed. These are documented in `DARWIN-FORGE-REMAINING-LIMITATIONS.md` and none is misreported as functional.

Not accepted as PRODUCTION-READY, because a green build + internal validation alone cannot justify production acceptance; the remaining limitations above must be resolved (GPU backend decision first, then ENR boundary completion) before that claim is defensible.
