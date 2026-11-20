# DARWIN FORGE — TRAINING TELEMETRY, MONITORING & SETTINGS DIALOG AUDIT & IMPROVEMENT REPORT

Date: 2026-11-20. Scope: FORGE training-pipeline correctness, authoritative live telemetry, and the Forge Settings dialog turned into a live Training Control & Telemetry Dashboard. Every claim below is backed by executed builds and tests (Java 21, Tycho 4.0.5, offline). No simulated training, no fabricated metrics, no fake progress.

---

## 1. Original architecture

- **Training engine**: `EvoLlmTrainer` (forge.trainer) — pure-Java CPU float32 decoder-only transformer, real attention/backprop, embedded AdamW, deterministic seeded shuffling, atomic `.chk` checkpoints, resume by dataset fingerprint.
- **Orchestration**: `ForgeOrchestratorImpl` (forge.controller) stages ANALYZING → PREPARING → TRAINING → VALIDATING → FINALIZING → export (.evo/GGUF), registering the trainer with `ForgeSessionManager` reflectively.
- **Authoritative telemetry**: `ForgeSessionManager.TrainingTelemetry` (controller) — volatile scalars, `history` (1000 MetricSamples), `logStream` (200 events), JSON persistence per session in `workspace/forge-state`, REST `/forge/session/{id}/training/metrics`.
- **UI**: Forge Page (HTML/JS polling), Forge Status Group (AI Commander, 500 ms), Forge Settings Dialog (2245-line static configuration form + SVG-in-Browser graphs fed by the `ForgeTrainingEstimationAgent`).

## 2. Discovered defects (audit register, with severity)

| # | Severity | Finding |
|---|---|---|
| C1 | CRITICAL | Loss Trajectory graph drew a **hardcoded projected curve** (`initialProjectedLoss=8.87`, `exp(-3.5t)`) presented as the training trajectory; no live telemetry was ever read by the dialog. |
| C2 | CRITICAL | **SMALL (25.0M) vs Small (50M) discrepancy**: three conflicting parameter-count sources (estimation agent table 25M, `ModelSizePreset` 48.9M, display name 50M). |
| C3 | CRITICAL | Resource monitor presented **formula-derived estimates as measurements** (RAM "usage", throughput, "Quality & Health Index" were fabricated gauges). |
| C4 | CRITICAL | Dialog epoch progress = `history.length/maxEpochs` of a stale constructor array — fake progress. |
| H1 | HIGH | `warmupSteps` config was dead: trainer always used internal 3%-of-total-steps schedule. |
| H2 | HIGH | Learning rate was never published to telemetry (listener re-passed `telemetry.learningRate`, which nothing ever wrote → permanently −1). |
| H3 | HIGH | Time budget evaluated **only at epoch boundaries**; a long epoch could not be interrupted. |
| H4 | HIGH | Resumed token accounting approximated (`startEpoch × epochTokenCount`, ignoring masks) — skewed ETA/tok/s. |
| H5 | HIGH | `ProgressListener.onProgress` legacy callback never invoked → `LLMDarwinEngine` progress logging was dead code. |
| H6 | HIGH | `cpuPercent`/`gpuStatus` dead fields; RAM sampled only inside `updateMetrics`; no real CPU measurement anywhere. |
| H7 | HIGH | Metric history/log stream not persisted → graphs unrecoverable after application restart. |
| H8 | HIGH | No ETA from actual measurements; analytical estimate used a fixed 0.5 GFLOPs/core constant presented without a preliminary label. |
| H9 | HIGH | No Refresh functionality; no auto-refresh; no staleness indication; Stop&Finalize/Pause/Abort used reflection; no Resume/Start actions in the dialog. |
| M1 | MEDIUM | `lossHistory` unbounded in the trainer. |
| M2 | MEDIUM | GPU detection message implied GPU training ("GPU Hardware: Detected") while the backend is CPU-only. |
| M3 | MEDIUM | Loss axis fixed 0–10; no validation series; no LR/throughput/hardware/memory/timeline graphs. |
| M4 | MEDIUM | No run separation: telemetry history from a previous run mixed into a new run's graphs. |
| M5 | MEDIUM | Dataset candidate stable IDs not whitespace-normalized (`"hugging face:"` vs `"huggingface:"`) — pre-existing, fixed opportunistically. |
| L1 | LOW | Dead variable `actualCompletedSteps`; console-only approximations. |

## 3. Changes implemented

### 3.1 Training engine (forge.trainer)
- `EvoLlmTrainer`: configured warmup (`setWarmupSteps`) actually applied; LR passed to listeners via a new 11-arg `onProgressMetrics(…, learningRate)` (default bridges to the 10-arg overload — legacy listeners unaffected); **time budget also checked at accumulation-window boundaries** (long epochs interruptible, partial epochs still never counted as completed); `lossHistory` capped at 4096; removed dead `actualCompletedSteps`; `lastProcessedTokens` tracked and seeded from checkpoint on resume.
- `TrainingCheckpoint`: new exact `processedTokens` counter with a backward-compatible trailer (`EVO_CHK_TOK` written after the legacy field block; legacy files load with 0 → old approximation fallback).

### 3.2 Orchestration (forge.controller)
- `ForgeOrchestratorImpl`: wires `warmupSteps`; registers the trainer with `freshStart` = no-resume-checkpoint (clears previous run history); records the **real measured throughput** of every completed run into the estimation-agent measurement store.
- `ForgeTrainingEstimationAgent`: measured-throughput store (bounded per model size, persisted as JSON, reloaded after restart); `getMeasuredTokensPerSecond`, `getMeasurementCount`, `getThroughputConfidence`; analytical estimates explicitly labeled PRELIMINARY; parameter-count table aligned with `ModelSizePreset` (SMALL = 48.9M — the 25M/50M discrepancy is gone); param-count overloads for baseline TPS and duration.
- **New `TrainingEtaEstimator`** (pure logic): rolling-window (120 s) EMA throughput over timestamped measurements, token- then step-based ETA, full-history fallback, explicit UNAVAILABLE for zero evidence, confidence 0..1, rejects NaN/negative counters by clamping — never fabricates a rate.

### 3.3 Telemetry (controller)
- `MetricSample` carries `learningRate`; the session listener publishes the trainer LR; history/log cleared on fresh starts (`registerActiveTrainer(sid, trainer, freshStart)`).
- Real **process CPU %** measured via `ThreadMXBean` delta (throttled ≥1 s).
- `persistSessionState` persists LR + last 500 timestamped history samples; `recoverFromRestart` restores them — graphs survive application restarts.
- `LLMDarwinEngine`: fixed the dead progress listener (consumes `onProgressMetrics`).

### 3.4 Forge Settings Dialog → Live Training Control Center (view)
- **New `ForgeTelemetrySnapshot`**: immutable capture of the authoritative telemetry + uiState config with honest derived values (overall/epoch progress, resolved throughput with basis MEASURED_LIVE / MEASURED_HISTORY / ANALYTICAL_PRELIMINARY / UNAVAILABLE, measured ETA + confidence, budget consumption and ON TRACK / AT RISK / EXCEEDS BUDGET / COMPLETED / UNKNOWN states, stable fingerprint for change detection).
- **Layout**: TOP status strip (session, status, phase, last-updated, staleness indicator, Auto Refresh + interval, Refresh Now); LEFT configuration grouped into Preset / Model & Training (incl. honest fixed AdamW + sequence length) / Termination Criteria / Checkpoint & Export / Hardware & Backend (honest CPU-only backend note); CENTER live monitor (progress bar, EPOCH X/Y – XX%, step, losses, LR, tokens, elapsed/ETA/completion/confidence) + dark-theme SVG graphs; RIGHT datasets & limits (configured vs measured stats); BOTTOM tabbed graphs: **Loss | Learning Rate | Throughput | Hardware | Memory | Timeline**.
- **Graph 1 Loss Trajectory**: measured actual loss (timestamped, auto-scaled axes), validation series, target threshold from configuration, pulsing current marker, historical persisted series labeled, and a **projection fitted only from the last ≤8 measured points (≥3 required)** — no hardcoded curves, insufficient data renders an honest placeholder.
- **Graph 2 Capacity Budget**: target budget, actual elapsed, measured remaining, estimated total, consumption %, status badge; pre-training planning uses the labeled preliminary estimate.
- **Graph 3 Progress & Resource Monitor**: real step progress, real JVM RAM, real measured throughput (with PRELIMINARY label when unmeasured), real process CPU — N/A when unavailable.
- **Refresh Now**: reloads session info, configuration, lifecycle state, all telemetry history and every graph from `ForgeSessionManager`; safe during training; never restarts/resets/modifies anything; last-valid snapshot preserved when telemetry is unavailable.
- **Auto refresh** (default 3 s, 1–10 s selectable, UI-thread `timerExec`), re-render only on fingerprint change.
- **Actions**: Estimate, Start Training (real REST start), Pause, Resume, Cancel (abort), Open Training Report (run folder), Stop & Finalize, OK — enabled per lifecycle state; reflection replaced with direct service calls.
- **Epoch/batch accounting**: step-based overall progress from telemetry; batch/sample counters explicitly shown as N/A (not exposed by the trainer) instead of fabricated values.

### 3.5 Data (forge.data)
- `DatasetCandidate.createStableId` whitespace normalization (pre-existing defect, one-line fix).

## 4. ETA methodology

1. Prefer live EMA throughput from telemetry while training.
2. Else persisted measured history for the model size (median; confidence from sample count).
3. Rolling-window (last 120 s of timestamped samples) token-based ETA; step-based fallback; full-history fallback.
4. Only with no measurements: ANALYTICAL PRELIMINARY (labeled, confidence 0 %).
Completion time = now + ETA; budget consumption = (elapsed + ETA)/budget.

## 5. Telemetry architecture

One authoritative source — `ForgeSessionManager.TrainingTelemetry` — consumed by REST, Forge Page, Forge Status Group and the new dialog snapshot layer. Trainer → `ProgressListener` (LR-aware) → telemetry `updateMetrics` → `MetricSample` history (capped, timestamped, persisted). No competing counters; per-session isolation; fresh-start history clearing; terminal states published by the orchestrator with the real `TrainingResult`-derived status.

## 6. Build and test results (actually executed)

- `./mvnw -o clean verify -DskipTests` (Java 21): full 30-module reactor — **BUILD SUCCESS**.
- Forge regression batch (325 tests, incl. forensic training audit, native pipeline regression, checkpoint/resume/lifecycle/quantization/GGUF/sync suites): **325 run, 0 failures, 0 errors — BUILD SUCCESS**.
- New suites (all green): `TrainingEtaEstimatorTest` (7), `ForgeTrainingEstimationMeasurementsTest` (5), `TrainingCheckpointTokenAccountingTest` (3, incl. legacy-format compat), `EvoLlmTrainerWarmupBudgetLrTest` (6), `ForgeTelemetryLrHistoryTest` (4), `ForgeTelemetrySnapshotTest` (6), `ForgeSettingsDialogLiveMonitorTest` (8, incl. no-fabricated-projection and real-values-in-HTML assertions), plus updated `ForgeSettingsDialogHardwareEstimationTest` (14) and `ForgeTrainingIntegrityAndSyncTest` (14, budget test updated to the corrected window-boundary semantics).
- Full controller.tests module (1267 tests): all forge-related classes green; the remaining failures are the pre-existing unrelated subsystem failures (CognitiveLoop/Develop/Mediated/SelfDev/Port/Server and port-28080 environment interference) documented in `DARWIN-FORGE-AUDIT-REPAIR-REPORT-2026-11-20.md`.

## 7. Verified behaviors

- Training actually optimizes parameters with decreasing loss (forensic + integrity tests).
- Warmup config applied; budget interrupts mid-epoch without overstating completed epochs; pause/resume across executions (stable run folder + fingerprint) still works — a defect found during this initiative (measurement-store file inside the scanned project tree changed the corpus fingerprint and silently broke resume) was fixed by moving the store to `~/.evo-forge`.
- Checkpoint round-trip with exact processed tokens; legacy checkpoints still load.
- Telemetry LR publication, CPU measurement code path, history persistence/restore, fresh-start clearing.
- Dialog graphs: real measured values appear verbatim; empty states honest; projection requires ≥3 measured points; monitoring tabs render all six graphs.

## 8. Remaining limitations (honest)

- **Runtime GUI verification not performed** (no interactive RCP session available): the dialog's SWT layout, Browser rendering, timer loop and button states are compile- and logic-verified only.
- GPU training remains CPU-only pure-Java; the dialog labels this explicitly. No real CUDA kernels (pre-existing).
- Batch/sample counters are not exposed by the trainer — displayed as N/A rather than estimated (deliberate).
- Epoch-granular early stopping is unchanged (threshold check on completed epochs); the time budget additionally guards window boundaries.
- A full multi-hour training E2E was not re-run; deterministic orchestrator-level resume and training-integrity suites cover the changed paths.
- `qualityFilter` still not consumed by the backend (labeled in the dialog, pre-existing).
