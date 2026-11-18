# DARWIN FORGE — DEFECT REGISTER

Date: 2026-11-17. All entries were verified against source at the cited paths (line numbers refer to the audited revision).
Legend: `[V]` verified defect · `[S]` suspected defect (needs runtime confirmation) · `[M]` missing implementation · `[F]` verified functionality (kept for context) · `[L]` architectural limitation · **`[FIXED]`** corrected in this initiative (2026-11-17) · `[PRE-EXISTING]` failure predating this initiative (confirmed untouched code or test drift).

---

## A. Tensor engine (`eu.kalafatic.evolution.forge.math`)

| ID | Class | Title | Evidence |
|---|---|---|---|
| A-1 `[V]` **[FIXED]** | `SimpleTensor(long...)` | No validation of zero/negative dims; `(int) size` overflow silently truncates >2^31 sizes | `core/SimpleTensor.java:12-22` |
| A-2 `[V]` **[FIXED]** | `SimpleTensor(long[],float[])`, `(...,float[],float[])` | No check that `data.length == prod(shape)`; shape/storage can disagree → wrong strides in all row-major ops | `core/SimpleTensor.java:24-38` |
| A-3 `[V]` **[FIXED]** | `getValue`/`setValue`/`getFlatIndex` | No rank check, no per-dimension bounds check; `getValue(0,5)` on [3,4] silently reads wrong element; negative indices → AIOOBE | `core/SimpleTensor.java:168-185` |
| A-4 `[V]` **[FIXED]** | `add/sub/mul/div` | No shape compatibility check, no broadcasting; smaller operand → AIOOBE crash; larger operand → silent truncation; `div` unguarded /0 → Inf/NaN | `core/SimpleTensor.java:188-225` |
| A-5 `[S]` **[REVIEWED — NOT A DEFECT]** | `matmul` | Parallel branch (`m==1 && p>=256`) uses parallel reduction → non-deterministic float rounding across runs | `core/SimpleTensor.java:243-255` |
| A-6 `[L]` | engine | No autodiff (grads only storage); float32 only; no device abstraction; allocations per op | whole file |
| A-7 `[F]` | `matmul`, `transpose`, `softmax`, `sum`, `concatRowWise` | Correct math, correct shape/dimension checks for rank-2 matmul | `core/SimpleTensor.java:228-300,302-324` |
| A-8 `[M]` | tests | No unit tests for SimpleTensor arithmetic/indexing/shape validation anywhere in repo | verified absence |

## B. Model architecture (`eu.kalafatic.evolution.forge.model`)

| ID | Class | Title | Evidence |
|---|---|---|---|
| B-1 `[V]` **[FIXED]** | `EvoLlmModel.forward(int[],float[])` | Attention mask silently discarded — trainer passes padded-sequence masks that never reach attention | `llm/EvoLlmModel.java:175-177`; caller `../forge.trainer/impl/llm/EvoLlmTrainer.java:531` |
| B-2 `[V]` **[FIXED]** | `EvoLlmModel.forward(int[][],float[][])` | Batched overload processes only `batchInput[0]`, ignores mask, builds dead ForwardContext | `llm/EvoLlmModel.java:182-197` |
| B-3 `[V]` **[FIXED]** | `EvoLlmModel.forwardWithCache` + `PositionalEncoding.forward` | Every autoregressive decode step re-encodes its single token with position 0 (no position offset); generation math wrong for positions > 0 | `llm/EvoLlmModel.java:148-170`; `llm/PositionalEncoding.java:31-36`; `inference/ReferenceEvoInferenceEngine.java:231` |
| B-4 `[S]` | `MultiHeadAttention.forwardWithCache` | Cached forward never populates backward activation caches; calling `backward()` after cached forward uses stale/null activations | `llm/MultiHeadAttention.java:161-255` vs `67-83` |
| B-5 `[S]` **[FIXED]** | `Embedding.forward/backward` | OOV fallback hardcodes token id 1, which is BOS in the tokenizer convention, not UNK | `llm/Embedding.java:42,55` |
| B-6 `[S]` **[FIXED]** | `EvoArchitectureDescriptor` | Declares `positional_encoding = "rope"` but model implements additive sinusoidal PE; every `.evo` carries a wrong descriptor | `protocol/EvoArchitectureDescriptor.java:21` |
| B-7 `[S]` **[FIXED]** | `FeedForward`, `EvoLlmModel.initXavier` | Unseeded `Random` → different weights per process; cross-run determinism claims don't hold | `llm/FeedForward.java:16`; `llm/EvoLlmModel.java:95` |
| B-8 `[S]` **[FIXED]** | `PositionalEncoding.forward` | No `seqLen <= maxSeqLen` check → AIOOBE from arraycopy instead of clear error | `llm/PositionalEncoding.java:31-36` |
| B-9 `[S]` | `EvoModelSerializer.parseVal` | Naive indexOf JSON parser breaks on key reorder; weights.bin has no header/checksum | `llm/EvoModelSerializer.java:72-86` |
| B-10 `[S]` | `EvoModelArtifact.generateWeightName` | Hardcodes untied layout (`paramsPerBlock=9`); misnames tensors for tied models | `llm/EvoModelArtifact.java:126-150` |
| B-11 `[V]` **[FIXED]** | `EvoModelExporter.exportSnapshot` | Default implementation builds a fresh random `EvoLlmModel` and never copies snapshot weights → exports random weights | `llm/EvoModelExporter.java:21-38` |
| B-12 `[V]` **[FIXED — F16/BF16/INT8 real storage; INT4 rejected]** | `EvoModelQuantizer` | INT8/INT4 quantize values then store back into float[]; `weightDtypes` never updated → manifest still F32; no storage reduction; "compression ratio" is cosmetic | `llm/EvoModelQuantizer.java:54-122,164-173` |
| B-13 `[F]` | `MultiHeadAttention` | Genuine multi-head scaled-dot-product attention, causal masking, KV cache, correct analytic backward incl. softmax Jacobian | `llm/MultiHeadAttention.java:67-390` |
| B-14 `[F]` | `TransformerBlock`, `RMSNorm`, `FeedForward`, `EvoLlmModel` | Pre-norm residuals, genuine RMSNorm with analytic backward, genuine SwiGLU with correct derivative, tied-head gradient handling | `llm/TransformerBlock.java:23-70`; `llm/RMSNorm.java:27-93`; `llm/FeedForward.java:37-115`; `llm/EvoLlmModel.java:105-225` |
| B-15 `[F]` | `EvoLlmArchitecture`, `EvoModelValidator` | Dimension validation (`dModel%numHeads==0`, positivity, `dff>dModel`); validator scans params for NaN/Inf | `llm/EvoLlmArchitecture.java:22-45`; `inference/EvoModelValidator.java:23-60` |
| B-16 `[V]` **[FIXED]** | `EvoModelWriter` | Header: `sectionCount=5` but 6 string sections written; content hash (64 hex chars) truncated to 32-byte field | `protocol/EvoModelWriter.java:148,151-161`; read side `protocol/EvoModelReader.java:79,82-84` |
| B-17 `[V]` **[FIXED]** | load path | Per-tensor checksums and content hash never verified in production load (`EvoModelArtifact.load`, `ReferenceEvoInferenceEngine.generateFromEvoFile`); `validateEvoFile` called only from tests and one UI page | `llm/EvoModelArtifact.java:260-300`; `inference/ReferenceEvoInferenceEngine.java:89-92` |
| B-18 `[F]` | `EvoModelReader/Writer` | EVO_NAT2 v2 + legacy v1 read; gzip tensors; per-tensor SHA-256; atomic move | `protocol/EvoModelWriter.java:36-171`; `protocol/EvoModelReader.java:46-249` |
| B-19 `[M]` **[FIXED — cancellation flag + thread interrupt]** | inference cancellation | Generation loop has no interrupt/stop checks | `inference/ReferenceEvoInferenceEngine.java:228-271` |
| B-20 `[S]` | inference | Leftover `System.out.printf` debug spam; unseeded sampling Random | `inference/ReferenceEvoInferenceEngine.java:187,206-215,247-252` |

## C. Trainer (`eu.kalafatic.evolution.forge.trainer`)

| ID | Class | Title | Evidence |
|---|---|---|---|
| C-1 `[V]` **[FIXED]** | `EvoLlmTrainer.processMicroBatch` | `LossReduction.SUM` divides by `windowTokens/batchSize` — not a sum reduction; batch-size-dependent gradients; inconsistent on ragged windows | `impl/llm/EvoLlmTrainer.java:555-557` |
| C-2 `[S]` **[FIXED]** | same | `windowValidTokens==0` → `continue` after `optStep++` → LR schedule advances with no optimizer step | `impl/llm/EvoLlmTrainer.java:391,409` |
| C-3 `[V]` | mask plumbing | Trainer builds attention masks but model discards them (see B-1); right-padding + causal masking currently masks the impact | `impl/llm/EvoLlmTrainer.java:602-635` |
| C-4 `[S]` **[FIXED — canonical moment keys + size validation]** | `TrainingCheckpoint` | Moment maps keyed by positional index `p_N`; restore misaligns silently if parameter order changes; per-tensor size mismatch beyond count not validated | `api/TrainingCheckpoint.java:120-163`; `impl/llm/EvoLlmTrainer.java:682-716` |
| C-5 `[M]` | early stopping | `earlyStoppingEnabled`/`targetLossThreshold` never consumed; `EARLY_STOPPING_CRITERIA` never assigned | `forge.controller/api/ForgeJob.java`; `api/TerminationReason.java:9` |
| C-6 `[M]` | config | `warmupSteps`/`timeBudgetHours` never consumed (warmup hardcoded 3%) | `impl/llm/EvoLlmTrainer.java:353` |
| C-7 `[M]` **[FIXED — validation-loss NaN/Inf check]** | NaN checks | No NaN/Inf check on validation loss or gradients (only window train loss) | `impl/llm/EvoLlmTrainer.java:416-424,571-600` |
| C-8 `[S]` | resume | `plannedSteps`/`totalTokensToTrain` computed over full epochs → inflated denominators on resume | `impl/llm/EvoLlmTrainer.java:303-313` |
| C-9 `[V]` **[FIXED]** | dead code | `TRAINING_COMPLETED && completedEpochs<epochs` branch unreachable; misleading pause message | `impl/llm/EvoLlmTrainer.java:377-378,507-511` |
| C-10 `[F]` | `EmbeddedAdamW` | Correct decoupled AdamW: decay before moment update, bias correction, global L2 clip from clipped grads, rank-1 no-decay grouping | `impl/llm/EvoLlmTrainer.java:667-773` |
| C-11 `[F]` | LR schedule | Linear warmup + cosine decay to minLr; resumes mid-curve via step counter | `impl/llm/EvoLlmTrainer.java:637-643` |
| C-12 `[F]` | gradient accumulation | MEAN_PER_TOKEN semantics correct: token grads divided by window token count, summed over window | `impl/llm/EvoLlmTrainer.java:400-429,555-562` |
| C-13 `[F]` | lifecycle | Stop/pause/abort + interrupt; NaN/Inf train-loss → fail + invalidate; checkpoint/restore incl. optimizer m/v/step; validation forward-only | `impl/llm/EvoLlmTrainer.java:324-498` |
| C-14 `[F]` | `TrainingCheckpoint` | Atomic save, magic verification, vocab-size compatibility check, tokenizer vocab persistence | `api/TrainingCheckpoint.java:61-227` |

## D. Data acquisition / `.evodata` (`eu.kalafatic.evolution.forge.data`)

| ID | Class | Title | Evidence |
|---|---|---|---|
| D-1 `[V]` **[FIXED — per-entry SHA-256 + CORRUPTED status]** | `EvoDatasetArtifact` | No checksums written or verified; zip CRCs never checked on load; `CORRUPTED` status never assigned; corrupt-but-parseable payload silently accepted | `api/artifact/EvoDatasetArtifact.java:214-369,371-444` (status enum line 39) |
| D-2 `[M]` **[FIXED — tokenizer identity + formatVersion validation]** | same | Tokenizer identity not stored; `formatVersion` written but never validated on load | write: line 382; read: 214-369 |
| D-3 `[V]` **[FIXED — deterministic timestamps]** | same | `createdTimestamp`/`timestamp` use wall clock → byte-reproducibility claim broken | `api/artifact/EvoDatasetArtifact.java:372,385-386` |
| D-4 `[V]` **[FIXED — honest report claim]** | `DatasetPreparationService` | `report.txt` claims noise/corruption/invalid-QA filtering "APPLIED" but this path runs only empty-check + threshold 0.0 (disabled) + dedup | `impl/service/DatasetPreparationService.java:187-232`; report text `EvoDatasetArtifact.java:494` |
| D-5 `[V]` | `GitAdapter` | `supports()` accepts remote URLs but `convert()` requires local directory → advertised support silently yields zero samples | `impl/source/adapters/GitAdapter.java:43-45,66-108` |
| D-6 `[S]` | `SourceAdapterRegistry` | `JSONLAdapter` shadows `OASST1Adapter` (registry order) → OASST tree reconstruction lost for `.jsonl` inputs | `api/source/SourceAdapterRegistry.java:27-40` |
| D-7 `[S]` | `ParquetAdapter` | Heuristic string harvester (PAR1 magic + sliding-window Snappy decompression); no codec detection; partial/garbled data for dictionary/ZSTD/nested files; no Parquet library | `impl/source/adapters/ParquetAdapter.java:79-125,160-252` |
| D-8 `[M]` | provenance | Collector records git commit/branch/URL/SHA-256 but artifact metadata keeps only sourceType/bytes; `sourceConfig="MULTI_SOURCE"` | `impl/collector/EvoCodebaseDataCollector.java:141-162`; `EvoDatasetArtifact.java:390-397` |
| D-9 `[S]` **[FIXED — content-hash cache key]** | `DatasetPreparationService.computeCacheKey` | Cache key = mtime+size, not content → stale reuse after content-preserving edits | `impl/service/DatasetPreparationService.java:378-398` |
| D-10 `[S]` **[FIXED — sha256 dataset fingerprint in checkpoints]** | `ForgeOrchestratorImpl` | Checkpoint `datasetFingerprint` = file name only, not content digest | `service/impl/ForgeOrchestratorImpl.java:269` |
| D-11 `[M]` | scale | Full in-RAM load (`readAllBytes` + line split) and second full re-read in `validateArtifact`; no sharding/streaming | `api/artifact/EvoDatasetArtifact.java:200,231,287` |
| D-12 `[V]` | `SimpleTokenizer` (core) | Unknown chars silently map to NUL token 0 → lossy round-trip (class unused in production) | `core/SimpleTokenizer.java:25,34` |
| D-13 `[S]` | `EvoTokenizer` | Hand-rolled JSON vocab parser breaks on nested/multiline tokens (class unused) | `impl/EvoTokenizer.java:134-175` |
| D-14 `[V]` **[FIXED — honest per-source log]** | `DatasetPreparationService` | Per-source acceptance log printed before dedup/rejection → inflated `recordsAccepted`/`rejected=0` diagnostics | `impl/service/DatasetPreparationService.java:161-162` |
| D-15 `[F]` | acquisition | HF rows API with pagination, Parquet/Hub fallbacks; deterministic split (seed 42) with leakage exclusion; zip-slip protection; malformed-JSON rejection counters; atomic artifact write; exact+normalized+MinHash dedup | `HuggingFaceDatasetSource.java`; `EvoDatasetArtifact.java:76-119,131-188,224-228,349-355`; `DatasetDeduplicator.java` |
| D-16 `[F]` | `SimpleBPETokenizer` | Special tokens, byte-fallback UTF-8, incremental streaming decoder = batch decode, vocab validation report | `impl/SimpleBPETokenizer.java` |

## E. GPU (`eu.kalafatic.evolution.gpu`) — not part of Tycho build

| ID | Class | Title | Evidence |
|---|---|---|---|
| E-1 `[V]` **[FIXED — no fake GPU claims remain]** | whole bundle | **No GPU execution exists** — no kernels, no memory allocation, no transfers, no sync. All training is pure Java CPU | grep across bundle; `trainer/GpuTrainingProcess.java:86-100` |
| E-2 `[V]` **[FIXED — fake training loop removed]** | `GpuTrainingProcess` | Fake training: hardcoded `loss=4.5` ×0.85/epoch, weight decay by fixed factor; no forward/backward/dataset; artifact contains only `tok_embeddings.weight` + `norm_final.scale` while arch JSON claims layers/heads | `trainer/GpuTrainingProcess.java:105-157` |
| E-3 `[S]` **[FIXED — real cuInit call + honest labels]** | `CudaComputeVerifier` | "CUDA_GPU_VERIFIED" = symbol presence via FFM lookup; no `cuInit`, no device enum, no context | `diag/CudaComputeVerifier.java:86-110` |
| E-4 `[V]` **[FIXED — real content hash + read verification]** | `EvoGpuModelSerializer` | Header content hash written as 32 zero bytes (protocol inconsistency with `EvoModelWriter`); read never verifies checksums | `trainer/EvoGpuModelSerializer.java:148-149,168-251` |
| E-5 `[S]` | `NvidiaGpuDetector` | stdout drained before stderr/waitFor (pipe deadlock hazard); `detectCudaVersionFromNvidiaSmi` ignores waitFor result | `diag/NvidiaGpuDetector.java:84-98,146-169` |
| E-6 `[F]` | `NvidiaGpuDetector` | Genuine nvidia-smi query with timeout/exit-code/field validation | `diag/NvidiaGpuDetector.java:61-144` |
| E-7 `[M]` | wiring | Bundle invoked nowhere in production forge pipeline (own CLI + reflective detector call from ENR only) | grep verified |

## F. Orchestration / telemetry / facades

| ID | Class | Title | Evidence |
|---|---|---|---|
| F-1 `[V]` | `TrainingControllerImpl` etc. | Fabricated REST data when service null: `loss=0.5/acc=0.8`, fake event strings, `mock-model-id`, fake dataset stats, fake snapshot, fake export path | `forge.controller/impl/TrainingControllerImpl.java:56-76`; `ModelControllerImpl.java:19-61`; `DatasetControllerImpl.java:28-46`; `SnapshotControllerImpl.java:30-60`; `ExporterControllerImpl.java:35-45`; wired null in `servers/.../ServerManager.java:426-431` |
| F-2 `[V]` | `ForgeSessionManager.runE2EDemo` | Time-based (`Thread.sleep(1000)`) fake training progress + "DUMMY OLLAMA MODEL CONTENT" file over REST | `controller/orchestration/ForgeSessionManager.java:756-827`; `EvolutionServer.java:2794-2797` |
| F-3 `[V]` | `ForgeOrchestratorImpl` evaluation | Hardcoded `overfittingDetected=false`, heuristic `qualityScore`, fabricated `valLoss=finalLoss*1.05`, default `finalLoss=0.05` | `service/impl/ForgeOrchestratorImpl.java:356,422-429` |
| F-4 `[V]` | `SelfEvoForgingServiceImpl` | Progress 0→100 with hardcoded loss 0.05; `stopForging` doesn't stop the job | `service/impl/SelfEvoForgingServiceImpl.java:170,217-219` |
| F-5 `[V]` | `ForgeOrchestratorImpl` progress | Trainer listener `onProgress` empty → job progress stuck at 60% during training | `service/impl/ForgeOrchestratorImpl.java:244-288` |
| F-6 `[S]` | telemetry | UI reads `progressPercent` that telemetry JSON never emits | `controller/orchestration/ForgeSessionManager.java:70-164`; UI `renderer.js:1144,1295` |
| F-7 `[F]` | `ForgeSessionManager.TrainingTelemetry` | Real trainer-fed telemetry (loss/tok/s/ETA/RAM), throttled sampling | `controller/orchestration/ForgeSessionManager.java:513-590` |
| F-8 `[V]` | legacy stubs | `LinearExecutionEngine` returns empty map; `GraphExecutionEngine.executeNode` returns null; `XmiForgeRepository.load()` returns null; `EvoKernelImpl.routeEvent` empty; `EvolutionaryTrainer.step/evaluateFitness` empty | `forge.runtime/impl/LinearExecutionEngine.java:17-19`; `GraphExecutionEngine.java:80-83`; `XmiForgeRepository.java:19`; `EvolutionaryTrainer.java:28-41` |

## G. Export / ENR

| ID | Class | Title | Evidence |
|---|---|---|---|
| G-1 `[F]` | `OllamaExporter` + GGUF stack | Genuine GGUF v3 little-endian writer with 32-byte alignment, transposed 2D tensors, llama metadata; post-write `GGUFValidator` gate before Modelfile | `forge.agent.api/.../export/OllamaExporter.java:149-154,225-329`; `gguf/GGUFValidator.java` |
| G-2 `[S]` | `OllamaExporter.resolveGgufFileType` | Always returns 0 (ALL_F32) regardless of quantization; quantized exports are simulated (dequantized F32 storage) → no size benefit, no real GGUF quant types; external llama.cpp/Ollama acceptance untested | `OllamaExporter.java:183-186`; `EvoModelQuantizer.java:147-181` |
| G-3 `[S]` | ENR independence | `EnrMain` claims standalone, but bundle requires `eu.kalafatic.evolution.controller` whose closure includes SWT/JFace/UI/EGit → cannot run outside RCP | `enr/META-INF/MANIFEST.MF:8`; `controller/META-INF/MANIFEST.MF:6-39` |
| G-4 `[M]` | ENR resource management | No shared VRAM/RAM manager, no scheduling, no memory-pressure handling; models either resident (native) or delegated to external processes | `enr/EnrRuntime.java` |
| G-5 `[S]` | `EnrHttpServer.health()` | Uses `EnrRuntime.instance()` instead of injected engine | `enr/EnrHttpServer.java:363-372` |
| G-6 `[S]` | `ForgeEvoAgent` | Legacy demo server binds 58081 (legacy portal port convention) | `forge.agent/.../ForgeEvoAgent.java:13` |
| G-7 `[S]` | `forge.agent` | `ModelExporter` shells to `python3 convert_gguf.py` — Python-dependent path contradicting project constraints | `forge.agent/.../ModelExporter.java:17` |
| G-8 `[V]` | forge-lab | Prototype duplicates APIs with empty stubs; `ExporterControllerImpl` fabricates `/tmp/forge_export_*.gguf`; zero tests | `forge-lab/forge-controller/.../ExporterControllerImpl.java:34-41` |
| G-9 `[M]` | docs | AGENTS.md §10 omits ENR port 28780; `PACKAGE_CONTEXT.md` drift | `AGENTS.md` §10 vs `PortRegistry.java:37` |
| G-10 `[F]` | ENR server | `/enr/v1` REST+SSE, auth token, PortRegistry 28780, OSGi wiring, feature inclusion; covered by `EnrHttpServerTest`/`EnrRuntimeTest` | `enr/EnrHttpServer.java:63-141` |

## H. Legacy dead code (kept for record; do not delete without explicit request)

- `llm/Tensor.java` (empty stub, forge.model)
- `EvoTokenizer`, `WordTokenizer`, `WhitespaceTokenizer`, `core/SimpleTokenizer`, `VocabularyBuilder`
- `EvoAdamW` (only used by one legacy test), `EvolutionaryTrainer`, `TrainerEvolutionService` (no impl)
- `SelfEvoForgingServiceImpl2` (fully commented out), `EvoKernelImpl` (partial), `EvolutionServiceImpl` stubs
- `OllamaExporter2/3/DS/DS2/GPT` variants (906-982 lines each; only GPT referenced by one test)
- `forge.observability` (`EventTracker` no impl), `forge.runtime` engines, `forge-lab` prototype

## I. Pre-existing test drift and environment-dependent failures (confirmed during initiative, not caused by the fixes)

| ID | Test | Symptom | Root cause | Status |
|---|---|---|---|---|
| I-1 `[PRE-EXISTING]` | `DatasetAcquisitionAndSizeTest.testLargeTargetAcquisition`, `testTinyFirstShardContinuesToNextShards`, `testDeduplicationAccountingContinuesAcquisition`, `testFilteringDoesNotStopOnRawBytes` | acquisition engine stops at source exhaustion; no continuation/amplification to reach target | acquisition loop in `TrainingDataAcquisitionServiceImpl` (untouched) does not implement source continuation; WP1 remediation required | OPEN — scheduled WP1 |
| I-2 `[PRE-EXISTING]` | `InferenceEngineSwitchingTest.testFilterModelsByInferenceEngine` | expected model names replaced by `test-evo-models-unit.evo` | `ProjectModelManager` merges discovered artifacts from `lib/models` (fixture file committed in repo); test not hermetic | OPEN — test-fixture isolation required |
| I-3 `[FIXED]` | `UniversalDatasetPreparationPipelineTest` (6), `DatasetSelectionAndAdapterIntegrationTest.testForgeSettingsDialogCreateEvodataInForgeInputFolder` | `INSUFFICIENT_DATA` for tiny test datasets | tests used no-arg `DatasetPreparationContext` (500 MB default target); production default is intentional | tests updated to explicit target 0 |
| I-4 `[FIXED]` | `DatasetAcquisitionAndSizeTest.testControlCharactersStrippedInCleaner` | expected `\uFFFD` stripped | production deliberately preserves `\uFFFD` for the corruption quality scorer | test updated to document behavior |
| I-5 `[FIXED]` | `DatasetAcquisitionAndSizeTest.testEvoDatasetArtifactStatisticsPersistence` | expected READY with accepted < requested | production status semantics (INSUFFICIENT_SOURCE_DATA) are correct; test data fixed | test updated |
| I-6 `[FIXED]` | `DatasetAcquisitionAndSizeTest.testConversationalSchemaNormalization` | expected `user:` text format | production canonical format is `### role:` | test updated |
| I-7 `[FIXED]` | `StructuredConversationAndOasstTest.testEvoDatasetArtifactWithConversationAndLegacySamples`, `testOpenAssistantAttachedFileRegression` | sample-order and path/NPE issues | deterministic shuffle reorders samples; fixture path relative to CWD; null context | tests updated |
| I-8 `[FIXED]` | `EvoModelQuantizerTest.testInt4Quantization` | expected fake INT4 artifact | INT4 packed storage not implemented; quantizer now refuses honestly | test updated to assert rejection |
| I-9 `[FIXED]` | `TrainingResumeLifecycleTest.testResumeDoesNotRepeatCompletedEpochs` | re-submitting completed epochs re-trained | trainer did not remember completed epochs across train() calls | production fix: `lastCompletedEpochs` no-op tracking |
| I-10 `[FIXED]` | `EvoArchitectureAndExporterPipelineTest.testTinyPresetRegression` | vocab 284 ≠ target 8000 | tokenizer did not pad vocab to target size | production fix: deterministic `<pad_N>` padding |
| I-11 `[FIXED]` | `EvoModelPersistentArtifactTest.testSaveLoadRoundTripAndMetadataPreservation` | sampling config lost on dir load | `loadFromDirectory` did not parse config.json sampling params | production fix: parsing added |
| I-12 `[FIXED]` | `FinalSnapshotValidationTest.testSnapshotExportAndGGUFValidation` | `weights.bin` missing | exporter produced only GGUF + Modelfile | production fix: raw F32 weights.bin sidecar |
| I-13 `[FIXED]` | `ForgeWorkflowE2ETest` `NoSuchMethodError DateUtils.formatDuration` | stale `eu.kalafatic.utils` bundle in local p2 repo | bundle not rebuilt since morning session | fixed by rebuilding utils+controller |
| I-14 `[FIXED]` | `DatasetSelectionAndAdapterIntegrationTest.testAdapterRegistrySelection` | OASST `.jsonl.gz` resolved to JSONLAdapter | registry order: JSONLAdapter shadowed OASST1Adapter (audit S-1) | production fix: JSONLAdapter yields OASST paths |

## J. Defects found and fixed during the completion phase (2026-11-17, Phase B-D)

| ID | Class | Title | Evidence |
|---|---|---|---|
| J-1 `[FIXED]` | `EvoLlmTrainer` | Epoch shuffles compounded (`shuffle(shuffle(seed43), seed44)`); resume was NOT logically continuous vs uninterrupted run (weights diverged ~1e-4). Now shuffles a fresh canonical-order copy per epoch → bit-exact resume | `impl/llm/EvoLlmTrainer.java` (canonicalEpochOrder) |
| J-2 `[FIXED]` | `HuggingFaceDatasetSource.preflight` | `ResolvedSource` constructor args swapped (`requestedSplit`/`resolvedSplit`) → preflight always reported the requested split, masking fallback resolution | `HuggingFaceDatasetSource.java` preflight success path |
| J-3 `[FIXED]` | `HuggingFaceDatasetSource.extractSampleFromRow` | Branch-3 condition included bare `row.has("response")` → prompt/response rows were rejected before branch 4 could handle them | same file, branch 3/4 |
| J-4 `[FIXED]` | `HuggingFaceDatasetSource.next` | Consumed samples never incremented `acceptedRecords` (only bytes) | same file, `next()` |
| J-5 `[FIXED]` | `EvoModelReader` | Real trained artifacts contain vocab sections with duplicate JSON keys → `new JSONObject` threw; now duplicate-tolerant (last wins). Legacy content hashes were produced by an unreproducible algorithm → tiered integrity (per-tensor checksums enforced; legacy hash informational) | `protocol/EvoModelReader.java` |
| J-6 `[FIXED]` | `LlamaService.resolveEvoModelPath` | Unknown model names silently resolved to default `evo.gguf` artifacts (user dirs + workspace forge-output) — a typo'd model would run a wrong model | `controller/manager/LlamaService.java` (fallbacks gated on evo-variant) |
| J-7 `[FIXED]` | `EvoDatasetArtifact.validateTokenizerCompatibility` | (added) datasets declaring a tokenizer identity are validated against the training tokenizer before training | `forge.data/.../EvoDatasetArtifact.java` + `ForgeOrchestratorImpl` |
| J-8 `[FIXED]` | `EvoModelArtifact` | Binary `.evo` did not persist sampling configuration (temperature/topP/topK/penalties); now in metadata + restored on load | `llm/EvoModelArtifact.java` |
| J-9 `[FIXED]` | `OllamaExporter` | Export did not copy GGUF into the default Ollama models folder (regression vs test contract) and lacked the root-level weights.bin copy | `forge.agent.api/.../OllamaExporter.java` |
| J-10 `[FIXED]` | HF test mocks | Three `HuggingFaceAccountingAndLifecycleTest` mocks returned rows forever → infinite acquisition loops (test defects; made one-shot) | test file |
| J-11 `[FIXED]` | `LlamaServiceTest` polluter | Test wrote fixture files into real user model dirs (`~/workspace/models`, real checkout) with incomplete cleanup → cross-run pollution broke I-2 | `evo.test.modelDirsOverride` seam + hermetic tests |
| J-12 `[FIXED]` | `ProjectModelManager` | No way to resolve artifacts hermetically → added resolver seam (set/reset) used by tests only | `controller/manager/ProjectModelManager.java` |
| J-13 `[FIXED]` | `SelfEvoForgingServiceImpl` | (WP1) fabricated stats 0.05/DONE and non-functional stopForging → real job-driven stats + real stop | `forge.controller/.../SelfEvoForgingServiceImpl.java` |
| J-14 `[FIXED]` | `LLMDarwinEngine` | trainLoss 2.5/10.0 sentinels and trainLoss-as-valLoss → honest NaN + documented penalty path | `controller/orchestration/selfdev/LLMDarwinEngine.java` |
| J-15 `[FIXED]` | `TrainingDataAcquisitionServiceImpl` | No amplification/re-iteration/retry/cancellation (I-1) → coverage-gated marked amplification + cancellation | `forge.data/.../TrainingDataAcquisitionServiceImpl.java` |
| J-16 `[FIXED]` | `LocalDatasetSource` | Whole-file `.txt` sampling hid junk records (zero rejections) → hybrid recordization (≤3-char lines become rejectable records) | `forge.data/.../LocalDatasetSource.java` |
| J-17 `[FIXED]` | `EnrHttpServer` | SSE cancellation hardcoded `() -> false`; unbounded concurrent inference → client-disconnect cancellation + semaphore admission control (429) | `enr/EnrHttpServer.java` |
| J-18 `[FIXED]` | `EnrRuntime` | llama-cpp path ignored request temperature/maxTokens → honored now | `enr/EnrRuntime.java` |
| J-19 `[FIXED]` | `runE2EDemo` | Wrote "DUMMY OLLAMA MODEL CONTENT" into a `.gguf` file with time-based fake progress → removed, honest notification | `controller/orchestration/ForgeSessionManager.java` |
