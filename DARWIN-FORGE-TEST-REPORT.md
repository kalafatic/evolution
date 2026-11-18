# DARWIN FORGE — TEST REPORT

Date: 2026-11-17
Environment: Java 21 (OpenJDK 21, `/usr/lib/jvm/java-21-openjdk-amd64`), Tycho 4.0.5 offline build, eclipse-test-plugin `eu.kalafatic.evolution.controller.tests`.
GPU bundle tests: standalone Maven module `eu.kalafatic.evolution.gpu` (`mvn -o test`, `--enable-preview`).

## 1. New regression suite

**`NativePipelineRegressionTest` (25 tests)** — added by this initiative. Execution command:

```
./mvnw -o -pl eu.kalafatic.evolution.controller.tests verify -Dtest=NativePipelineRegressionTest
```

| # | Test | Expected | Result |
|---|---|---|---|
| 1 | `tensorConstructorRejectsNegativeDimension` | IllegalArgumentException | PASS |
| 2 | `tensorConstructorRejectsZeroDimension` | IllegalArgumentException | PASS |
| 3 | `tensorConstructorRejectsShapeDataMismatch` | IllegalArgumentException | PASS |
| 4 | `tensorIndexOutOfBoundsThrows` | IndexOutOfBoundsException | PASS |
| 5 | `tensorWrongIndexRankThrows` | IllegalArgumentException | PASS |
| 6 | `tensorElementwiseShapeMismatchThrows` | IllegalArgumentException | PASS |
| 7 | `tensorMatmulMatchesReference` | hand-computed reference values | PASS |
| 8 | `attentionMaskPreventsAttendanceOnPaddingPositions` | masked position cannot influence query 3 logits; unmasked control differs | PASS |
| 9 | `fullyMaskedSequenceProducesFiniteLogits` | no NaN/Inf | PASS |
| 10 | `maskedPositionGradientIsZero` | zero embedding grad for masked token | PASS |
| 11 | `autoregressiveGenerationMatchesBatchedForwardPositionalEncoding` | KV-cache step logits == batched forward last-row logits (PE offset correctness) | PASS |
| 12 | `sumLossReductionProducesBatchSizeIndependentGradients` | SUM loss/grads identical for batch 1 vs batch 4 | PASS |
| 13 | `evoFileHeaderHashAndSectionCountAreCorrect` | 64-char hash, full hash in metadata, verifyIntegrity OK | PASS |
| 14 | `corruptedEvoPayloadFailsLoadWithIntegrityError` | IOException with `EVO_INTEGRITY_ERROR` | PASS |
| 15 | `evoRoundTripPreservesLogitsExactly` | exact logits after save/load | PASS |
| 16 | `defaultExporterSnapshotPreservesTrainedWeights` | exported weights == snapshot weights | PASS |
| 17 | `f16QuantizedArtifactStoresRealF16DtypeAndShrinks` | manifest dtype F16, file smaller, value tolerance | PASS |
| 18 | `int8QuantizedArtifactRoundTripsWithScale` | manifest dtype INT8, `validateEvoFile` OK | PASS |
| 19 | `int4QuantizationIsHonestlyUnsupported` | UnsupportedOperationException | PASS |
| 20 | `generationCancellationTerminatesPromptly` | CANCELLED, 0 tokens, fast | PASS |
| 21 | `evodataRecordsTokenizerIdentityAndRoundTrips` | tokenizer type/version/vocab round-trip, READY, no diagnostics | PASS |
| 22 | `corruptedEvodataIsDetectedAndMarkedCorrupted` | CORRUPTED status + checksum diagnostics | PASS |
| 23 | `evodataIsByteReproducible` | byte-identical artifacts | PASS |
| 24 | `unsupportedEvodataFormatVersionIsRejected` | CORRUPTED + "format version" diagnostics | PASS |
| 25 | `checkpointMomentsUseCanonicalTensorNames` | `n_` keys restored | PASS |

**Result: 25/25 PASS.**

## 2. Existing suites re-verified (batch run 2026-11-17)

```
./mvnw -o -pl eu.kalafatic.evolution.controller.tests verify -Dtest='<33 classes>'
```

| Class | Tests | Result |
|---|---|---|
| NativePipelineRegressionTest | 25 | 25 PASS |
| EvoNativeProtocolTestSuiteTest | 4 | PASS |
| EvoNativeModelCoreTest | 3 | PASS |
| EvoNativeInferenceEngineTest | 10 | PASS |
| EvoNativeGoldenEndToEndTest | 3 | PASS |
| EvoModelQuantizerTest | 3 | PASS (INT4 updated to honest rejection) |
| EvoModelPersistentArtifactTest | 8 | PASS |
| ForgeForensicTrainingAuditTest | 22 | PASS |
| TrainingResumeLifecycleTest | 3 | PASS |
| ForgeLifecycleStopPauseResumeAbortTest | 3 | PASS |
| CheckpointDeterminismTest | 1 | PASS |
| EvoArchitectureAndExporterPipelineTest | 4 | PASS |
| FinalSnapshotValidationTest | 2 | PASS |
| GGUFReaderAndValidatorTest | 13 | PASS |
| EvoNativeEndToEndSmokeTest | 3 | PASS |
| MultiHeadAttentionMathematicalTest | 4 | PASS |
| PositionalEncodingMathematicalTest | 3 | PASS |
| SimpleBPETokenizerDecoderTest | 9 | PASS |
| AlpacaEvodataTrainingPipelineTest | 3 | PASS |
| DatasetQualityAndLeakageVerificationTest | 6 | PASS |
| TrainingDataPreparationPipelineTest | 5 | PASS |
| AcquisitionRefactoredComponentsTest | 4 | PASS |
| DarwinDataPrepTest | 6 | PASS |
| ForgeDataPipelineEndToEndTest | 9 | PASS |
| UniversalDatasetPreparationPipelineTest | 13 | PASS |
| StructuredConversationAndOasstTest | 8 | PASS |
| DatasetSelectionAndAdapterIntegrationTest | 7 | PASS |
| LLMDarwinEngineOptimizationTest | 6 | PASS |
| TinyStoriesDiagnosticExperimentTest | 1 | PASS |
| ForgeWorkflowE2ETest | 1 | PASS |
| ForgeQuantizationPipelineTest | 3 | PASS |
| InferenceEngineSwitchingTest | 6 | 1 FAIL (I-2, environment-dependent) |
| DatasetAcquisitionAndSizeTest | 26 | 4 FAIL (I-1, pre-existing WP1 acquisition gaps) |

**Batch total: 225 run, 5 failures — all pre-existing/environment-dependent (see defect register section I).**

## 3. GPU bundle (`eu.kalafatic.evolution.gpu`)

Command: `cd eu.kalafatic.evolution.gpu && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -o test`

| Test | Result |
|---|---|
| GpuTrainingProcessTest.testExecuteTrainingIsolated | PASS (rewritten: asserts diagnostics-only honesty, no fake artifact, no fake epochs) |
| EvoGpuModelSerializerTest.testModelSerializationRoundtrip | PASS (now includes real SHA-256 header hash + read-time verification) |
| NvidiaGpuDetectorTest | PASS |
| CudaComputeVerifierTest | PASS |

Runtime evidence (this machine): GPU `NVIDIA GeForce RTX 5070 Ti` detected; **real `cuInit(0)` succeeded** → label `CUDA_DRIVER_VERIFIED`; process honestly reports `CPU_HOST_JAVA (CUDA DRIVER PRESENT)`, `nativeTrainingExecuted=false`, and produces no model artifact.

## 4. Build verification

- Tycho `install -DskipTests` for all affected bundles: forge.math, forge.model, forge.tokenizer, forge.data, forge.trainer, forge.controller, forge.agent.api, eu.kalafatic.utils, eu.kalafatic.evolution.controller — **SUCCESS**.
- Standalone javac verification of the same bundles against org.json — SUCCESS.
- GPU module `mvn -o compile` — SUCCESS.

## 5. Known remaining failures (not hidden)

1. `DatasetAcquisitionAndSizeTest`: `testLargeTargetAcquisition`, `testTinyFirstShardContinuesToNextShards`, `testDeduplicationAccountingContinuesAcquisition`, `testFilteringDoesNotStopOnRawBytes` — pre-existing; acquisition engine does not continue acquiring after source exhaustion/dedup shortfall (defect register I-1; WP1 remediation).
2. `InferenceEngineSwitchingTest.testFilterModelsByInferenceEngine` — pre-existing; non-hermetic fixture interaction with `lib/models` directory (defect register I-2).
3. Not executed (require network or heavy compute): HuggingFace live-endpoint tests, full end-to-end training of a real dataset, external llama.cpp/Ollama load of exported GGUF (no llama.cpp/Ollama runtime verification performed — GGUF validated with the internal `GGUFReader`/`GGUFValidator` only).

---

## 6. Completion-phase additions (2026-11-17, Phase B-D)

New regression classes (all in `eu.kalafatic.evolution.controller.tests`):

| Class | Tests | Coverage |
|---|---|---|
| `TelemetryHonestyRegressionTest` | 9 | WP1: no fabricated REST metrics/model/snapshot/export data; telemetry JSON progressPercent + aliases; real orchestrator evaluation; stopForging; demo no longer writes dummy files |
| `AcquisitionContinuationRegressionTest` | 5 | WP2: coverage-gated amplification (marks + gate + disable), cancellation → CANCELLED, hybrid recordization |
| `TrainingAndArtifactLifecycleVerificationTest` | 5 | WP3/WP4/WP5: legacy v1.0 `.evodata`, tokenizer-identity mismatch errors, atomic save hygiene, **bit-exact resume continuity (0 differing floats)**, trained-model `.evo` fidelity incl. sampling config |
| (updated) `InferenceEngineSwitchingTest` | 6 | hermetic resolver seam; passes in isolation, in-suite, repeatedly |
| (updated) `LlamaServiceTest` | 9 | hermetic model-dir override; no writes to real user dirs |
| (updated) `HuggingFaceAccountingAndLifecycleTest` | 10 | one-shot downloader mocks (previously infinite loops); downloaded-bytes accounting assertions corrected to the honest two-page model |
| (updated) `EvoModelQuantizerTest` | 3 | INT4 honest rejection |

Final battery (Tycho, offline): **297/297 tests PASS** across 43 classes (0 failures, 0 errors).
GPU module (standalone Maven, `--enable-preview`): **4/4 PASS**.

External verification (not part of the deterministic suite):
- `llama-cli` loads and executes GGUF exported from real trained models (218MB and 3.1MB artifacts) — no errors.
- `ollama create` + `/api/generate` on the E2E-trained model: `done=true`, generated text with learned `user:`/`assistant:` structure.

Skipped/not executed: live HuggingFace network tests (offline build); RCP product UI harness; a second full 13-stage E2E run (first run recorded in `DARWIN-FORGE-END-TO-END-ACCEPTANCE-REPORT.md`; deterministic stages are covered by the suite).
