# DARWIN FORGE — REFACTORING PLAN

Date: 2026-11-17. Maps defect-register IDs to concrete work packages, in implementation order.
Rule: correctness first, then reliability, then performance, then architecture. Every fix must ship with a regression test where feasible. No silent CPU fallback, no fake progress, no invalid model marked READY.

**Progress markers**: `[DONE]` implemented + tested · `[PARTIAL]` partially implemented · `[OPEN]` not started.

---

## PHASE 1 — Correctness (Priority 1)

### 1.1 [DONE] Tensor engine validation (`forge.math`) — defects A-1..A-5
- Add constructor validation: reject zero/negative dims, reject size overflow beyond `Integer.MAX_VALUE`, verify `data.length == prod(shape)` (and grad length in the 3-arg constructor) with clear `IllegalArgumentException`s.
- `getFlatIndex`: validate `indices.length == rank` and `0 <= indices[i] < shape[i]`.
- Elementwise `add/sub/mul/div`: exact shape-match required (explicit error otherwise — no silent truncation/AIOOBE); keep no-broadcast contract.
- Make matmul parallel path deterministic: compute in fixed serial row order (drop the `m==1` parallel branch) or use deterministic reduction.
- **Tests**: new `SimpleTensorValidationTest` (constructor/shape/index/bounds errors), `SimpleTensorMathTest` (matmul vs hand-computed reference incl. >2^31 guard attempt, transpose, softmax, elementwise mismatch errors).

### 1.2 [DONE] Attention mask plumbing (`forge.model`, `forge.trainer`) — B-1, B-2, C-3
- Thread an attention mask through `EvoLlmModel.forward` → `TransformerBlock.forward` → `MultiHeadAttention.forward(x, mask)`; combine caller mask (padding) with causal mask (`score = masked ? -1e9`). Mask convention: `1.0 = attend`.
- Fix batched `forward(int[][], float[][])` to process all batch rows (concatenate along sequence axis), or replace with an explicit exception if batch batching remains out of scope — no fake processing of `batchInput[0]`.
- Keep backward compatible with existing single-arg `forward`.
- **Tests**: padding positions must not influence non-padded positions' logits; with all-mask-0 rows the softmax stays well-defined; trainer-level regression (masked padding tokens contribute zero gradient).

### 1.3 [DONE] Positional encoding under KV cache — B-3, B-8
- Add position-offset-aware encoding: `PositionalEncoding.forwardAtOffset(x, startPos)` copying table rows `[startPos, startPos+seqLen)`; `EvoLlmModel.forwardWithCache` passes `kvCache.getTotalLength()` as offset (0 on prefill).
- Add `seqLen+offset <= maxSeqLen` validation with clear error.
- **Tests**: autoregressive two-token generation must equal the two-token batched forward (logits equality) — this is the regression that proves position offset correctness.

### 1.4 [DONE] Loss reduction — C-1
- `LossReduction.SUM`: gradient per token = `dLoss_raw` (no normalization), loss value = raw sum; `MEAN_PER_TOKEN` unchanged (window token count).
- **Tests**: unit-test both branches with hand-computed gradients for a 1-token micro-model; assert SUM grads are batch-size independent.

### 1.5 [DONE] `.evo` protocol header — B-16
- Fix `sectionCount` to 6 (or drop the field — reader ignores it; choose: correct value with a comment).
- Fix content hash: write full 64-char SHA-256 (either extend header field to 64 bytes with a protocol minor bump, or store the digest hex in metadata JSON and keep the 32-byte field as a compatibility prefix). Decision: bump header to 64-byte hash, keep reader tolerant of 32-byte legacy hashes.
- **Tests**: golden-file test asserting header hash equals recomputed hash and sectionCount matches.

### 1.6 [DONE] Load-time integrity verification — B-17
- `EvoModelArtifact.load` and `ReferenceEvoInferenceEngine.generateFromEvoFile`: verify magic, per-tensor SHA-256, shape/element counts on load; refuse to load corrupted models with a clear diagnostic (use existing `EvoModelContentHash`/`EvoModelIntegrity`).
- **Tests**: corruption test (flip bytes in a tensor payload) must fail load with integrity error.

### 1.7 [DONE] Exporter snapshot weights — B-11
- `EvoModelExporter.exportSnapshot`: copy `snapshot.getParameters()` into the fresh model instead of random weights (add a `ModelParameters`-to-model copy path or construct from snapshot).
- **Tests**: export snapshot → load → logits must equal snapshot model logits.

### 1.8 [DONE] Quantizer honesty — B-12, G-2 (F16/BF16/INT8 real storage; INT4 rejected)
- Either implement real dtype change in storage (F16 half storage; INT8 packed; INT4 nibble-packed + updated manifest dtypes) or restrict the API to schemes that are actually supported and mark others unsupported. Minimum correct step: F16 real half-storage; INT8 real byte storage with scale metadata; INT4 explicitly unsupported until packed storage exists.
- `EvoModelReader/Writer` must handle the new dtypes.
- **Tests**: exported artifact's manifest dtype + payload size reflect the scheme; round-trip value tolerance.

### 1.9 [DONE] Model descriptor accuracy — B-6
- `EvoArchitectureDescriptor.positionalEncoding` default → `"sinusoidal_abs"` (actual implementation); keep `rope` only when RoPE is implemented.

### 1.10 [DONE] UNK fallback — B-5
- Use UNK id from tokenizer conventions (id 0) instead of hardcoded 1 (BOS), with the id injectable from the tokenizer descriptor.

## PHASE 2 — Reliability (Priority 2)

### 2.1 [DONE] GPU bundle honesty — E-1..E-5 (fake loop removed; real cuInit verification; honest labels; serializer hash fixed)
- Do not fake: remove/relabel fake training loop. Either (a) wire `GpuTrainingProcess` to the real `EvoLlmTrainer` (CPU execution explicitly reported as `CPU_HOST_JAVA`, CUDA status separate from execution backend), or (b) retire the fake loop and keep the bundle as diagnostics-only. Decision: (a) — replace simulated loop with real `EvoLlmTrainer` run, report `compute_device=CPU_HOST_JAVA`, `cuda_detected=true/false` separately; delete fake loss decay.
- `CudaComputeVerifier`: relabel to `CUDA_DRIVER_SYMBOLS_PRESENT` and call `cuInit`+`cuDeviceGetCount` when FFM is available; never claim compute capability from symbol presence alone.
- `EvoGpuModelSerializer`: write real SHA-256 header hash (reuse `EvoModelContentHash` semantics); verify checksums on read.
- Fix `NvidiaGpuDetector` stderr/waitFor ordering.
- **Tests**: serializer hash nonzero + round-trip verify; verifier relabel assertions.

### 2.2 [DONE] Trainer reliability — C-2, C-4, C-7, C-9 (plus completed-epochs no-op and tokenizer vocab padding)
- `windowValidTokens==0`: don't advance `optStep`/LR before the skip (restructure so schedule only advances with real steps).
- Checkpoint: key moments by canonical tensor name (`getModelParameters()` names) with legacy positional-key fallback; validate per-tensor sizes on restore.
- Add NaN/Inf validation-loss detection → `TRAINING_FAILED`.
- Remove unreachable dead branch; correct pause log message.

### 2.3 [DONE] Inference cancellation — B-19
- Add interrupt/stop-flag support to `ReferenceEvoInferenceEngine` generation loop; expose via existing request structure; propagate through `NativeEvoProvider`/ENR endpoints.
- **Tests**: long generation cancelled mid-stream terminates promptly and returns partial result with status.

### 2.4 [OPEN] Honest telemetry — F-1..F-6 (not yet addressed)
- Replace fabricated metrics/events in controller impls with real "no active session" responses (empty metrics + state = IDLE), never invented numbers.
- Remove or gate `runE2EDemo` behind explicit demo flag; never emit training events from sleeps.
- `ForgeOrchestratorImpl`: report real validation loss; drop fabricated `qualityScore`/`overfittingDetected` (or compute them from real validation data with documented formulas); `finalLoss` = real last loss or "n/a".
- `SelfEvoForgingServiceImpl`: forward trainer progress; `stopForging` must actually stop the job.
- Wire `onProgress` in the orchestrator trainer listener to job progress.
- **Tests**: idle metrics endpoint returns IDLE state without fake values.

### 2.5 [PARTIAL] `.evodata` integrity + identity + reproducibility — D-1..D-4, D-9, D-14 done; D-8 (per-source provenance) and D-12/D-13 (dead tokenizers) open
- Write per-entry SHA-256 in `metadata.json` (`files{data.jsonl:{sha256,bytes}}`); verify on load → set `CORRUPTED` status on mismatch with diagnostic.
- Add `tokenizer{type,version,vocabSize,specialTokens}` block + `formatVersion` validation (reject unknown majors with clear error; accept 1.x).
- Deterministic timestamps: fixed/derived timestamp (e.g. from content hash) or drop wall-clock fields from `metadata.json` (keep in a separate `runlog` entry excluded from hash).
- Provenance: propagate per-source `sourceType/sourceUrl/revision/timestamp/sha256` into `metadata.sources[]`.
- Quality filter: run `DataCleaner`/quality scorer in the authoritative path (or remove the false report claim); always log true accepted/rejected/duplicate counts per source after dedup.
- Cache key: content hash (SHA-256 over source bytes) instead of mtime+size.
- **Tests**: corrupted artifact load → CORRUPTED status; version rejection; tokenizer identity round-trip; determinism (same inputs → identical bytes); provenance fields present.

### 2.6 [DONE] Checkpoint dataset fingerprint — D-10 (SHA-256 recorded; resume enforcement still open)
- Store SHA-256 of the `.evodata` file in the checkpoint; warn/error on resume when fingerprint mismatches.

## PHASE 3 — Performance (Priority 3; only after Phases 1-2 verified)

### 3.1 Dataset scale — D-11
- Streaming `.evodata` writer/reader (write JSONL incrementally; avoid second full re-read in validate; load with bounded iterator).
- Optional sharding (`data-00000.jsonl`, ...) for large datasets with a `shardCount` metadata field.

### 3.2 Tensor engine
- Reuse buffers for repeated-shape intermediates in hot loops (attention), avoid redundant allocations; optional float16 accumulation paths only with verification.

### 3.3 Measurements
- Training throughput telemetry already exists (tok/s, steps/s); add CPU/GPU memory sampling where available; publish per-stage timings.

## PHASE 4 — Architecture (Priority 4)

### 4.1 Format specifications (docs)
- `EVO-DATA-FORMAT-SPECIFICATION.md` — actual `.evodata` v1.1 layout (post-2.5 changes).
- `EVO-MODEL-FORMAT-SPECIFICATION.md` — actual `.evo` EVO_NAT2 v2 layout (post-1.5 changes).
- `DARWIN-FORGE-TRAINING-ARCHITECTURE.md` — actual trainer/orchestrator flow.
- `EVO-NATIVE-RUNTIME-ARCHITECTURE.md` + `EVO-NATIVE-RUNTIME-API.md` — current ENR API (actual, not aspirational).
- `EVO-NATIVE-MODEL-COMPATIBILITY.md` — legacy V1 read support, checkpoint compat, GGUF conversion limits.

### 4.2 ENR boundary
- Decouple `EnrRuntime` from the controller bundle: extract needed interfaces (TaskContext usage, LlamaService/NativeEvoProvider contracts) into `enr`-local adapters or an api bundle; make the RCP dependency explicit in docs until separation is complete (do not silently claim standalone).
- Shared resource bookkeeping: introduce a small `RuntimeResourceTracker` inside ENR that records per-model residency and memory estimates; expose in `/enr/v1/runtime` metrics. Keep Ollama/llama.cpp as external providers.

### 4.3 Parquet
- Keep heuristic adapter as fallback but label it in `DatasetInspection`/reports; investigate real codec later (no new heavy dependency without approval).

### 4.4 Ports/docs
- Fix AGENTS.md/PACKAGE_CONTEXT port drift (ENR 28780).

## PHASE 5 — End-to-end acceptance (WP7)

Execute and record (report in `DARWIN-FORGE-TEST-REPORT.md`):
1. Synthetic pattern dataset (e.g. `next char = prev char+1`) → prepare `.evodata` → validate.
2. Train tiny model (EVO_FAST) → assert loss decreases → checkpoint → resume → `.evo` finalize.
3. Load `.evo` in reference engine → inference (assert learned pattern on trained inputs).
4. Export GGUF → validate with `GGUFValidator` → (external runtime check if llama.cpp available, else documented as not verified).
5. ENR: register two models, concurrent inference requests, streaming, cancellation, unload.
6. Regression: full `controller.tests` suite (Tycho) must pass.

## Explicitly NOT in scope (unless requested)
- Rewriting forge-lab prototype; deleting legacy dead code; RoPE/Rotary implementation; real CUDA kernels; new external ML libraries; Ollama replacement.
