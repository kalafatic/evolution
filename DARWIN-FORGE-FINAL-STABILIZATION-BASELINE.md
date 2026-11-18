# DARWIN FORGE — FINAL STABILIZATION BASELINE

Date: 2026-11-17 (completion phase, WP0)
Scope: Darwin Forge native pipeline (acquisition → `.evodata` → training → `.evo` → export → ENR → external runtime).
Method: source-level verification + Tycho/OSGi test execution + independent deep-dive analyses of every outstanding defect.
Note on Git state: per project AGENTS.md, Git operations are user-controlled (EGit); repository state was verified by file inspection and test execution instead of git status/diff.

## 1. Repository baseline

- Root: `/home/petr/git/evolution-121126` — Java 21, Eclipse RCP, OSGi, Tycho 4.0.5 (offline-capable, p2 cached).
- Previous initiative documents present and consistent with source: `DARWIN-FORGE-ARCHITECTURE-AUDIT.md`, `DARWIN-FORGE-DEFECT-REGISTER.md`, `DARWIN-FORGE-REFACTORING-PLAN.md`, `DARWIN-FORGE-TEST-REPORT.md`, `EVO-DATA-FORMAT-SPECIFICATION.md`, `EVO-MODEL-FORMAT-SPECIFICATION.md`, `DARWIN-FORGE-TRAINING-ARCHITECTURE.md`, `EVO-NATIVE-RUNTIME-ARCHITECTURE.md`, `EVO-NATIVE-RUNTIME-API.md`, `EVO-NATIVE-MODEL-COMPATIBILITY.md`.
- Regression suite `NativePipelineRegressionTest` (25 tests) present.
- Hardware: RTX 5070 Ti (16 GB VRAM) present; Ollama daemon running with real models incl. `evo:latest` (237 MB); llama-cli binaries available (`/home/petr/git/llama.cpp/build/bin/llama-cli`, `/home/petr/Downloads/llama-b10617/llama-cli`); real trained `.evo` artifacts in `~/workspace/forge-output`.

## 2. Verified existing functionality (re-verified this session)

- **Tensor engine**: matmul/transpose/softmax/elementwise with validation (re-verified in source; `SimpleTensor.java` validation present).
- **Model core**: genuine multi-head attention with causal + padding masks, correct analytic backprop, KV cache with correct positional offsets, deterministic init (seed 42).
- **Trainer**: AdamW (decoupled decay, bias correction, global clip), cosine LR + warmup, MEAN_PER_TOKEN/SUM reductions, NaN/Inf detection (train + validation), stop/pause/abort, canonical-keyed optimizer moments, completed-epoch no-op, tokenizer vocab padding.
- **`.evodata` v1.1**: per-entry SHA-256, CORRUPTED status + diagnostics, tokenizer identity, formatVersion validation, deterministic timestamps, content-hash cache keys, honest report claims.
- **`.evo` EVO_NAT2 v2**: real full content hash (metadata) + legacy prefix, sectionCount=6, per-tensor checksums enforced on every production load path, F16/BF16/INT8 real compact storage (INT4 rejected), exporter weight preservation, weights.bin GGUF sidecar.
- **GPU module**: real nvidia-smi diagnostics + real `cuInit(0)` verification (verified `CUDA_DRIVER_VERIFIED` on this machine), no simulated training, serializer with real hash + read verification.
- **Test state**: 128/128 targeted tests pass (incl. all previously fixed suites). GPU module: 4/4 pass.

## 3. Previously fixed defects (spot-verified in source this session)

A-1..A-4, B-1..B-3, B-5..B-8, B-11, B-12, B-16, B-17, B-19, C-1, C-2, C-4, C-7, C-9, D-1..D-4, D-9, D-10, D-14, E-1..E-4, I-3..I-14 — all confirmed present in current source (e.g. `PositionalEncoding.forwardAtOffset`, `SimpleTensor.validateElementwiseShape`, `EvoModelWriter` `evo_content_hash`, `EvoLlmTrainer.lastCompletedEpochs`, `SimpleBPETokenizer` `<pad_N>`, `CudaComputeVerifier.CUDA_DRIVER_VERIFIED`, `EvoModelReader.EVO_INTEGRITY_ERROR`).

## 4. Defects still reproducible (with fresh evidence)

### WP1 — Telemetry honesty (F-1..F-6) — OPEN, fully re-traced

| ID | Defect | Reproduction evidence (current source) | Consumers |
|---|---|---|---|
| F-1a | Fabricated REST fallbacks in forge.controller impls | `TrainingControllerImpl.java:56-76` (`loss=0.5`,`acc=0.8`, fake event strings), `ModelControllerImpl.java:19,34-61` (`mock-model-id`, "Active Model", hardcoded structure JSON), `DatasetControllerImpl.java:27-46` (size 1000/vocab 5000, "Functional sample text"), `SnapshotControllerImpl.java:29-60` ("Initial Snapshot", all-zero compare), `ExporterControllerImpl.java:41-45` (fake `/tmp/forge_export_*.gguf`, never written) | Wired with `null` services in `controller/orchestration/ServerManager.java:426-431`. Live fabricated endpoints: `/forge/session/{id}/structure`, `/dataset/stats`, `/dataset/sample/{i}`, `/snapshots`, `/snapshots/compare/active/{sid}` → forge.html viz (lines 2213-2366). Training metrics/events fallbacks currently shadowed by real telemetry (unreachable); structure/dataset/snapshot endpoints ARE live fabrication. `ExporterControllerImpl` unwired (dead). |
| F-2 | `runE2EDemo` fake training + DUMMY gguf | `controller/orchestration/ForgeSessionManager.java:756-827` — `Thread.sleep` progress events, writes file containing literal `"DUMMY OLLAMA MODEL CONTENT FOR DEMO"` (line 802) | Live: `POST /forge/session/{id}/demo` (`EvolutionServer.java:2794-2797`), UI trigger creatic.js:196 context menu. No tests. |
| F-3 | Fabricated evaluation in orchestrator | `forge.controller/.../ForgeOrchestratorImpl.java:393` (magic `finalLoss=0.05`), `461` (`valLoss = trainValLoss > 0 ? real : finalLoss*1.05`), `462` (qualityScore heuristic), `463` (hardcoded `overfittingDetected=false`) | Real `trainResult.getValLoss()` available (line 419 already uses it). EvaluationResult currently consumed by NOTHING — dead-but-dishonest. |
| F-4 | Fabricated forging stats; stopForging no-op | `SelfEvoForgingServiceImpl.java:170` (hardcoded `COMPLETE/100/0.05/"DONE"`), `217-219` (stats map remove only) | `GET /forge/session/{id}/forging/stats` (`EvolutionServer.java:806-837`), UI forge.html:2218 + `viz-self-evo.js` (shows "Current Loss: 0.0500", "registered in Ollama" banner unverified). No `/forging/stop` route exists. |
| F-5 | Job progress stuck at 60% during training | `ForgeOrchestratorImpl.java:283` empty `onProgress`; `setProgressPercent` only at stage boundaries (75/10, 86/30, 212/40, 229/60, then 456 after training) | `ForgeJob.progressPercent` not yet exposed over REST — latent. |
| F-6 | Telemetry JSON lacks `progressPercent`; UI field-name mismatches | `ForgeSessionManager.TrainingTelemetry.toJsonObject` (lines 122-163) emits `currentLoss/valLoss/stepsPerSec/estimatedRemainingSeconds`; `view/js/renderer.js:1144,1295-1309` reads `progressPercent/trainingLoss/validationLoss/learningRate/stepsPerSecond/estimatedRemainingMillis` | Training sub-panel shows `'—'` forever; `EvolutionProgressPublisher.toJson` also lacks these fields. |

**Newly confirmed additional fabrications (beyond F-1..F-6):**
- `LLMDarwinEngine.java:1609,1636-1637` (trainLoss sentinel 2.5), `1649` (train loss masquerading as valLoss), `1651-1656` (hardcoded 10.0 on NaN/Inf).
- UI: `forge-viz/viz-interactive-demos.js:198` (`Math.random()` loss), `viz-self-evo.js:442-444,196-204,242` (fake trajectory, "Darwin Score 0.85", unverified Ollama claim), `viz-evolution.js:23` (`Loss: 0.42`), `viz-training.js:248` (accuracy = 1−valLoss), `forge.html:2284-2291` (`renderDemoFallback` fabricated 0.2451/0.8920).
- `RuntimeControllerImpl.java:15-25` (DEPLOYMENT_STARTED before any work — unwired).
- Heuristic defaults (documented analytics, lower severity): `ForgeSourceAnalyzer.java:97,101,186,189,222`, `ForgeDatasetComposer.java:41`.

### WP2 — Acquisition continuation (I-1) — OPEN, root causes confirmed

Four failing tests (`DatasetAcquisitionAndSizeTest`):
1. `testLargeTargetAcquisition` — source exhausts at 75.7% of target; service hard-breaks at `TrainingDataAcquisitionServiceImpl.java:264-268` (`hasExplicitSources` → `break`). No amplification/re-iteration exists.
2. `testTinyFirstShardContinuesToNextShards` — all 3 shards processed (88.2% coverage) but exhaustion terminates below target.
3. `testDeduplicationAccountingContinuesAcquisition` — 93.3% coverage; dedup works (duplicateBytes>0 passes) but exhaustion shortfall persists.
4. `testFilteringDoesNotStopOnRawBytes` — DIFFERENT root cause: `LocalDatasetSource.java:126-145` reads whole `.txt` file as ONE sample → junk lines merged into acceptable samples → `rejectedBytes==0` (size assertion already passes).

Confirmed missing mechanisms: source re-iteration, amplification, service-level retry, acquisition cancellation input (service can only return READY/INSUFFICIENT — never CANCELLED/FAILED, lines 319-326), incremental/resume state. HF sources implement forward paging/retry internally (lines 374-410, 744-758); Local sources are re-initializable (`initialize()` re-entrant, lines 57-69) but the engine never re-invokes it.

Cancellation persistence gap: `EvoDataWriter.java:18-21` and `DatasetAcquisitionTool` create fresh artifacts (default status) and never propagate acquisition-result status; `EvoDatasetArtifact.validateArtifact` rejects 0-sample artifacts (a cancelled run with 0 samples cannot persist at all).

**Critical constraint** (any fix must preserve): 7 currently-passing exhaustion tests codify `INSUFFICIENT_SOURCE_DATA` at source exhaustion with coverage ≤ 40% (`testDatasetExhaustionReporting`, `testSourceExhaustionReturnsInsufficientSourceData`, `testDeduplicationMeasuredAfterFilter`, `testMultiSourceGlobalDeduplication`, HF `testShortfallExhaustionProducesInsufficientSourceDataStatus`, HF `testInsufficientSourceDataWhenSourceExhaustsBeforeTarget`, `testProgressBasedOnUsableContentMinimum`).

### WP9 — I-2 InferenceEngineSwitchingTest — OPEN, root cause confirmed

- Polluter: `LlamaServiceTest.testCopyToModelsDirAndResolution` writes fixture files (`test-evo-models-unit.evo`, `evo.evo`, …) into ALL `LlamaService.resolveAllTargetModelDirs()` including the user's real `~/workspace/models` and the real checkout `eu.kalafatic.evolution.controller/lib/models`; cleanup only removes from one dir → cross-run filesystem pollution.
- `ProjectModelManager.filterModelsByEngine:472-487` name-rewriting (deliberate production behavior used by `ChatMgmtGroup`) resolves "evo"-named inputs to the newest `.evo` file → returns `test-evo-models-unit.evo`.
- Latent expectation defect: even in a clean environment the test's rule-1 expectation (raw input names) contradicts the documented resolution behavior.
- Fix seam: `ProjectModelManager` has no injectable resolver/reset — needs a minimal test seam + hermetic test + polluter fix.

### WP8 — G-3 ENR RCP dependency — OPEN, fully mapped

- `enr/META-INF/MANIFEST.MF`: Require-Bundle controller, servers, forge.model, utils. Controller closure drags SWT/JFace/UI/EGit/m2e/JDT (controller MANIFEST lines 6-37).
- ENR imports from controller: `LlmRouter` (activator bridge), `LlamaService`, `OllamaManager/Service/Model`, `ProjectModelManager` (only 2 path getters!), `TaskContext` (470-line orchestration class for one metadata map), `NativeEvoProvider`, `PortRegistry` (one constant), `ILlmProvider`, `Orchestrator` (EMF, could come from model bundle directly).
- `EnrMain` standalone claim is false today: native path → `ProjectModelManager` → `ResourceManager.getOrchestrator()` → EGit → NoClassDefFoundError on minimal classpath.
- Recommended: pure-Java SPI in enr (`EnrModelPathResolver`, `EnrNativeGenerator`, `EnrLlamaCppGenerator`, `EnrOllamaClient`, `EnrPortResolver`), bridge implementations in controller, move `EnrLlmProvider` to the bridge, drop controller/utils Require-Bundles.

### WP8 — G-4 ENR resource management — OPEN, mapped

- No concurrency control (NanoHTTPD thread-per-connection, no limits), native/llama-cpp `loadModel/unloadModel` are no-ops (`EnrRuntime.java:136-152`), every native request rebuilds the full model in heap (`ReferenceEvoInferenceEngine:94-112`), KV cache per-request only, SSE cancellation supplier hardcoded `() -> false` (`EnrHttpServer.java:244`), llama-cpp path drops request `temperature()`/`maxTokens()` (`EnrRuntime.java:207-210`), no per-model instance config objects, no VRAM/RAM budgeting.
- No existing resource manager to reuse anywhere in EVO (verified: only `ResourceManager` path/EMF facade + port managers). An ENR-local governor is justified (single new component, not a second platform manager).

### WP6 — GPU execution — CONFIRMED NOT IMPLEMENTED (no defect, limitation)

`cuInit` success (verified on this machine) ≠ GPU computation. No kernels/memory allocation/transfers/sync exist anywhere (re-verified). Training and inference are CPU float32 Java. Status strings are now honest (`CPU_HOST_JAVA (CUDA DRIVER PRESENT)`, `CUDA_DRIVER_VERIFIED` means driver initialization only). Baseline position: report as limitation; no fake-GPU claims remain.

### WP7 — External GGUF — NOT VERIFIED (feasible now)

Internal GGUFValidator passes; external acceptance never executed. Environment now has: running Ollama daemon with `evo:latest` (237 MB — a previously exported EVO model), llama-cli at `/home/petr/git/llama.cpp/build/bin/llama-cli`, real trained `.evo` artifacts in `~/workspace/forge-output`. External verification is executable in Phase C/D.

### WP10 — Parquet — no codec library present anywhere (verified)

Only the heuristic `ParquetAdapter` (string harvester). Decision required, with benchmarks on representative data.

## 5. Architectural inconsistencies (current)

1. ENR contract (`EnrInferenceEngine` pure Java) vs bundle wiring (RCP closure) — G-3.
2. `forge.observability` (`EventTracker`) has no implementation — telemetry lives ad-hoc in `ForgeSessionManager`.
3. Two acquisition pipelines (`DatasetPreparationService` authoritative vs `TrainingDataAcquisitionServiceImpl` legacy) with different quality policies (known, documented).
4. `EvaluationResult` produced by orchestrator is consumed nowhere — dead data that is still fabricated.
5. Port conventions drift: AGENTS.md §10 omits ENR 28780 (G-9, doc-only).
6. `forge-lab` prototype duplicates forge APIs as stubs (out of scope, documented).

## 6. Missing integration tests (gap list)

- No REST-level test asserting endpoints return real/absent data rather than fabrications (needed for WP1).
- No acquisition continuation/amplification/cancellation/resume tests (WP2).
- No interrupted-save `.evodata` test; no tokenizer-identity-mismatch rejection test (WP3).
- No interrupted-vs-uninterrupted training determinism comparison test (WP4 — new requirement).
- No external llama.cpp/Ollama load-and-infer test (WP7).
- No ENR resource-limit/concurrent-request/multi-model tests (WP8).
- No test that `LlamaServiceTest`-style fixture writes cannot reach real user directories (WP9).
- No GPU kernel-vs-CPU numerical comparison test (WP6 — impossible until kernels exist; documented).

## 7. Backward-compatibility risks (with mitigations)

| Change | Risk | Mitigation |
|---|---|---|
| Removing fabricated REST fallbacks | UI panels that currently show "data" will show empty/NOT_AVAILABLE | Honest empty states; UI already falls back to `'—'` in places; verify forge.html panels render gracefully |
| Acquisition amplification | Dataset bloat, duplicate examples in training | Coverage gate (≤40% never amplified), explicit amplification metadata on copies, configurable cap, dedup-bypass only on amplification pass |
| LocalDatasetSource record-level sampling for `.txt` | Breaks whole-file sample semantics used by other tests/pipelines | Hybrid recordization only (junk lines become rejectable records; valid content still merged per file) |
| ENR SPI split | Runtime wiring regressions | Bridge delegates to existing classes unchanged; ENR tests (12 HTTP + 9 runtime) as safety net |
| Telemetry schema change | UI field mismatches already exist; adding fields is additive | Add computed `progressPercent` + aliases, keep existing keys |
| ProjectModelManager test seam | Production unchanged | Static resolver field default = existing behavior; reset for tests only |

## 8. Proposed implementation sequence

**Phase B — Critical correctness (WP1, WP2, WP3, WP4, WP5)**
1. WP1 telemetry: honest fallbacks (empty + NOT_AVAILABLE) in forge.controller impls; real evaluation in `ForgeOrchestratorImpl` (real valLoss, remove 0.05/1.05/overfitting=false); `SelfEvoForgingServiceImpl` real stats from ForgeJob/telemetry + real `stopForging`; `onProgressMetrics` → job progress 60-100%; `progressPercent` in telemetry JSON + renderer field alignment; delete/rebuild `runE2EDemo` on the real pipeline; regression tests per defect (REST-level where practical).
2. WP2 acquisition: coverage-gated capped amplification (Option A) + hybrid `.txt` recordization (Option C-2) + cancellation input/status propagation (Option D) + tests (new deterministic suite + fix the 4 failing tests at production level).
3. WP3 dataset integrity: interrupted-save test, tokenizer-identity mismatch rejection, legacy v1.0 loading test (exists implicitly — make explicit), repeated-preparation determinism (exists).
4. WP4 training/checkpoint: interrupted-vs-uninterrupted determinism test; verify resume continuity numerically; scheduler-position test (exists via optStep).
5. WP5 artifact: verify export path end-to-end on a real trained artifact (export → reload → logits equality) incl. sampling config; INT8 storage byte-size assertion (exists); legacy `.evo` v1 fixture test (exists via reader tests — make explicit if missing).

**Phase C — Runtime and compatibility (WP6, WP7, WP8, WP9, WP10)**
6. WP6: document honest GPU status (done); optional `EnrGpuStatus` truthful wiring; NO kernel work.
7. WP7: real trained model → GGUF → `ollama create`/`llama-cli` load → inference; record results; tolerances documented.
8. WP8: ENR SPI + bridge split; ENR-local resource governor (concurrency semaphore, residency/LRU accounting, request-config propagation, SSE cancellation); multi-model tests.
9. WP9: ProjectModelManager resolver seam + hermetic test + LlamaServiceTest polluter fix.
10. WP10: Parquet decision (keep heuristic vs real codec) with benchmark on representative data; document.

**Phase D — Acceptance (WP11, WP12, WP13, WP14)**
11. WP11: two full E2E runs on a real dataset (small but real; existing OASST local fixture or a prepared project corpus), every stage with timestamps/checksums/metrics; `DARWIN-FORGE-END-TO-END-ACCEPTANCE-REPORT.md`.
12. WP12: categorized full test matrix incl. RCP product assembly build; skipped-test documentation.
13. WP13: update test report + compatibility doc + remaining-limitations doc + final stabilization report.
14. WP14: final per-area assessment table and acceptance decision.
