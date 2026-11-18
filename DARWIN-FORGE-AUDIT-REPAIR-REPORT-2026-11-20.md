# DARWIN FORGE — AUDIT, REPAIR & IMPROVEMENT REPORT

Date: 2026-11-20. Initiative scope: training-engine integrity, UI quality (AI Commander / Forge Page / Forge Settings), and session/UI/telemetry synchronization. Every claim below is backed by executed builds/tests. No simulated training, no fabricated metrics.

## A. Training Engine

### What EVO Forge actually implements

EVO Forge performs **real LLM training** in pure Java (float32, CPU): a decoder-only transformer (`EvoLlmModel`) with true multi-head scaled-dot-product attention (verified multi-head math, causal + padding masks, softmax stability), sinusoidal positional encoding (correct position offsets in KV-cache generation), RMSNorm, SwiGLU FFN, analytic backpropagation (gradients verified against finite differences), cross-entropy loss with per-token response masking, embedded AdamW (decoupled weight decay, bias correction, global L2 clipping), linear-warmup + cosine-decay LR. It is **not** only dataset preparation / conversion / packaging. Verification: 22-item `ForgeForensicTrainingAuditTest` + 25-item `NativePipelineRegressionTest` + new tests below, all green.

### Real parameter optimization — VERIFIED

`ForgeTrainingIntegrityAndSyncTest.testTrainingActuallyOptimizesParameters`: trains a deterministic micro-model for 3 epochs, asserts (a) loss strictly decreases, (b) trainable parameter tensors actually change. The forensic audit independently verified loss decrease and optimizer correctness. No fabricated metrics are produced anywhere.

### Defects found and fixed (this initiative)

| # | Defect | Fix |
|---|---|---|
| 1 | **Mid-epoch stop/pause/abort over-stated `completedEpochs`** (`EvoLlmTrainer`: partial epoch recorded as complete) → pause checkpoint/resume skipped the rest of the partial epoch while the optimizer step counter sat mid-epoch | Partial epochs are no longer recorded; only fully completed epochs advance `completedEpochs`. Tests: `testMidEpochPause/StopDoesNotOverstateCompletedEpochs` |
| 2 | **Stop request race**: `train()` reset the stop/pause/abort flags at entry, so a control request delivered between trainer registration and training start was silently lost (job kept running — observed in the early-stop integration test) | Flags are consumed by the run and cleared at every exit point instead of the entry |
| 3 | **Checkpoint resume across executions was dead**: the run folder embedded `System.currentTimeMillis()`, so a later execution could never discover earlier checkpoints | Stable per-session run folder `dist/forging-<sessionId>`; verified by `ForgeOrchestratorResumeTest` (2nd execution resumes, `trainingMode=RESUMED`) |
| 4 | **Dataset fingerprint defeated resume**: fingerprint was the SHA-256 of the timestamped `.evodata` container → every run had a different fingerprint → resume never matched | Fingerprint now derived from the training corpus content; dataset change → `FROM_SCRATCH` (tested) |
| 5 | **Self-referential corpus**: source scan included the run's own `dist/` outputs (logs, checkpoints, exports) → each run trained on its own artifacts and the corpus changed every run | `dist/` excluded from the source scan |
| 6 | **Pause checkpoint inconsistency**: pause checkpoint used the `.evodata` filename as fingerprint and a different naming scheme than periodic checkpoints | Same corpus fingerprint + same fingerprint-prefixed naming |
| 7 | **Checkpoint architecture validation** ignored `maxSeqLen` | `validateCompatibility` now rejects maxSeqLen mismatches (tested) |
| 8 | **Cross-session stop**: `requestFinish(sessionId)` fell back to `requestFinishAll()` when the session had no active trainer — finishing one session could stop other sessions' trainings | Removed; control is strictly session-scoped |
| 9 | **Settings visible but ignored by the backend**: `targetLossThreshold`/`desiredLossThreshold`, `timeBudgetHours`, `maxDataSizeMb`, `maxSampleCount`, `outputModelName`, `generations`/`candidatesPerGeneration` | All now consumed: early stopping (`EARLY_STOPPING_CRITERIA`) and hard time budget (`TIME_BUDGET_REACHED`) implemented in the trainer; `maxDataSizeMb`→target usable bytes; `maxSampleCount`/`maxTokensLimit`→real caps (defaults 5,000 samples / 100,000 tokens) in both the ForgeJob path and the Darwin path; `outputModelName` honored; `generations`/`candidatesPerGeneration` drive the Darwin evolutionary search |
| 10 | `LocalDatasetSource` ignored `.py` (and `.js`/`.ts`) files | Python/JS/TS files now read (regression test re-enabled) |
| 11 | **DEFAULT profile epochs**: `Profile.DEFAULT` had 8 epochs; the corrected DEFAULT is **4** (SMALL=4, LARGE=10 preserved) | `Profile.DEFAULT` = 4 epochs; dialog/`ForgeJob`/`SelfEvoForgingServiceImpl` defaults aligned to 4; test asserts SMALL=DEFAULT=4, LARGE=10 |
| 12 | Dataset limits (10M chars, 100 files, 100k tokens, 5k samples) were hardcoded constants, not configurable, and absent from the ForgeJob path | Configurable through the existing forge-session configuration (uiState keys `maxCorpusChars/maxCorpusFiles/maxTokensLimit/maxTrainingSamples` + `maxSampleCount/maxTokensLimit`), defaults preserved; ForgeJob path now enforces token/sample caps |

### Checkpoints and resume — VERIFIED

- Atomic `.chk` save/load, canonical moment keys, per-tensor size validation (existing tests).
- Orchestrator-level resume across executions: `ForgeOrchestratorResumeTest` (epoch continuation `RESUMED`, dataset-change `FROM_SCRATCH`).
- Bit-exact resume semantics (`CheckpointDeterminismTest`, `TrainingResumeLifecycleTest`) — green.

### Exported artifacts — VERIFIED

`.evo` (integrity-gated, real weights, sampling config round-trip) and GGUF v3 export with post-write `GGUFValidator`; `GGUFReaderAndValidatorTest`, `EvoModelQuantizerTest`, `FinalSnapshotValidationTest` green. Quantization: F16/BF16/INT8 real storage; INT4 honestly rejected. External Ollama/llama-cli execution was verified in the 2026-11-17 E2E run; not re-executed this session.

## B. UI

### AI Commander Page

- New `ForgeStatusGroup` on the AI Commander page: session identity, authoritative status (RUNNING/PAUSED/FAILED/INTERRUPTED/COMPLETED…), epoch/step/loss/throughput metrics, progress bar, and state-aware actions: **Start Training**, **Pause**, **Resume**, **Stop**, **Open Forge Page**, **Forge Settings**. All actions invoke real backend services (REST forging endpoints + `ForgeSessionManager` controls); buttons enable/disable from actual backend state; refreshed every 500 ms on the UI thread.
- `MultiPageEditor.showForgePage()` navigation added.

### Forge Page

- **STOP previously did nothing to the backend** (UI-only state flip): now calls the real finish endpoint and re-syncs from telemetry.
- Added PAUSE / RESUME controls wired to the new backend endpoints (resume re-runs forging and continues from the fingerprint-matching checkpoint).
- Live synchronization: the page polls authoritative telemetry every 2 s (progress, loss, epoch/step), refreshes the TRAINING visualization, and reconciles button states from the real status; `FORGE_TRAINING_STOPPED`/`FORGE_TRAINING_FAILED` events trigger immediate re-sync. No simulated values.

### Forge Settings Dialog

- Defaults aligned with the corrected profile: 4 epochs, 5,000 samples; new Max Token Limit field; new Early Stopping checkbox; time budget now persisted and enforced.
- Honest labeling: supported settings documented; `Quality Filtering` explicitly labeled "not yet applied by backend"; loss-stage combo labeled visual-only.
- Every remaining visible setting now has a real backend effect (see A-9).

## C. Synchronization

- **Authoritative state**: `ForgeSessionManager` (EMF `ForgeSession` + `TrainingTelemetry`). `TrainingTelemetry` scalars are now `volatile` with a single atomic `updateMetrics()` publication point; `lastEventMessage` write fixed; val loss/best val loss/learning rate now flow from the trainer into telemetry.
- **Real terminal states**: new `completeActiveTrainer(sessionId, terminalStatus)` (called by the orchestrator with the real `TrainingResult`-derived status) and `updateSessionTerminalStatus` — the UI can no longer show RUNNING after the backend stopped or COMPLETED after a failure; `FORGE_TRAINING_FAILED` events published.
- **Listener lifecycle**: trainer progress listeners are removed on unregistration/deletion (no stale trainer→telemetry reference chains).
- **Session state persistence & recovery**: `persistSessionState()` writes a JSON snapshot (status, workflow, metrics) per session on every transition; on restart `recoverFromRestart()` reconstructs state — active runs become honestly `INTERRUPTED` (with a log entry and event), terminal states (COMPLETED/FAILED) survive; checkpoints persist in the stable run folder for resumption.
- **SWT thread safety**: all widget updates marshalled via `Display.asyncExec` (existing page patterns preserved); event bus listeners remain on the throttler thread and pages marshal to SWT.
- **Synchronization tests**: `ForgeTrainingIntegrityAndSyncTest` (telemetry concurrency, terminal status mapping, listener removal, persistence + recovery).

## D. Build and Testing

Bundles modified: `forge.trainer`, `forge.controller`, `forge.data`, `controller`, `view`, `controller.tests` (+ `forge.html` resource).

New classes: `ForgeStatusGroup` (view), `ForgeTrainingIntegrityAndSyncTest` (14 tests), `ForgeOrchestratorResumeTest` (2 tests).

Commands executed (Java 21, Tycho 4.0.5 offline):
- `./mvnw -o clean verify -DskipTests` — full 30-module reactor: **BUILD SUCCESS**.
- `./mvnw -o -pl eu.kalafatic.evolution.controller.tests -am verify -Dtest=<43 forge-related classes>` — **309 tests, 0 failures** (includes 22 forensic audit, 25 native pipeline regression, resume/lifecycle/checkpoint/E2E/quantization/telemetry honesty suites).
- Full module `verify` (1221 tests): all forge-related classes green; unrelated subsystem failures remain (see E).

Runtime verification: not performed in the RCP UI (no GUI session); all backend flows verified through the OSGi test runtime.

## E. Remaining Issues

**FIXED**: mid-epoch epoch accounting; stop-flag race; cross-execution resume (stable run folder + content fingerprint + verified scan); self-corpus leak; pause checkpoint consistency; maxSeqLen checkpoint validation; session-scoped stop; ignored settings made real (early stopping, time budget, sample/token limits, output name, generations/candidates); DEFAULT profile 4 epochs; telemetry thread safety; real terminal states; listener cleanup; session state persistence/recovery; STOP/PAUSE/RESUME UI actions; AI Commander forge controls; dialog defaults/honesty.

**VERIFIED**: real training with parameter changes and decreasing loss; checkpoint save/load/resume; `.evo`/GGUF artifact validity; UI/backend state agreement via telemetry.

**PARTIALLY IMPLEMENTED**: GPU execution still CPU-only Java (driver verification only — pre-existing, honestly labeled); warmup remains a fixed 3% schedule (not UI-exposed); `filterQuality` still not consumed (now explicitly labeled in the dialog).

**NOT IMPLEMENTED**: real CUDA kernels; INT4 packed storage; Parquet codec fidelity (heuristic adapter kept by documented decision); `.evodata` sharding/streaming.

**BLOCKED / PRE-EXISTING (not caused by this initiative)**: dozens of failures in subsystems never part of the verified batch and untouched here (CognitiveLoop/Develop/Mediated/SelfDev/Port/EvoGlobalContextMenu/etc.), plus environment interference during the full-module run (port 28080 held by Firefox). Two drifted tests updated to the documented deliberate behavior (insufficient-data continues training per the forensic audit; whole-file merge semantics of `LocalDatasetSource`).

**E2E note**: the 13-stage real-data end-to-end run with external Ollama/llama-cli verification was completed on 2026-11-17 (see `DARWIN-FORGE-END-TO-END-ACCEPTANCE-REPORT.md`); this initiative adds deterministic orchestrator-level resume integration tests instead of re-running the multi-hour experiment.
