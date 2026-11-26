# Package Context — eu.kalafatic.evolution.controller.hardware

## Purpose
Centralized, shared hardware-diagnostic services for EVO. This package is the
single authoritative source for CUDA backend testing and Forge GPU training
verification. All three UI locations (Settings Hardware panel, Forge page,
AI Commander Forge panel) consume these services — there is no duplicated
implementation.

## Contents
- `DiagnosticStatus` — overall test states (NOT_TESTED, RUNNING, PASS,
  PARTIAL, FAILED, UNSUPPORTED).
- `StageStatus`, `DiagnosticStage` — per-stage structured results with
  timestamps and durations (no log-text parsing).
- `CudaDiagnosticReport` — structured 12-stage CUDA diagnostic result
  (detection → driver → backend class load → native init → context → alloc →
  H2D → kernel → D2H → numerical verification → cleanup → final).
- `CudaDiagnosticService` — executes the CUDA diagnostic asynchronously on a
  shared daemon worker; bounded history; running-state dedup; listeners.
- `ForgeGpuVerificationReport`, `ForgeGpuVerificationService` — canonical
  training-path verification with an isolated tiny model; records policy,
  selection, actual backend, parameter-change verification and independent
  CUDA kernel/memcpy counter evidence.
- `DiagnosticWorker` — shared single-threaded executor for both services.

## Architectural constraints
- GPU-bundle access is REFLECTIVE through the forge.trainer class loader: the
  `eu.kalafatic.evolution.gpu` classes are compiled for stable Java 22 FFM
  while this bundle targets Java 21 (Tycho 4.0.5 limitation). On Java 21 the
  backend classes fail with `UnsupportedClassVersionError`, which the service
  records explicitly; CPU-only operation must never be affected.
- The CUDA diagnostic never closes the process-wide `CudaDevice` singleton;
  it only releases the device buffers it allocated (the CUDA context stays
  alive for training).
- The Forge verification refuses to run while a Forge training session is
  active (`ForgeSessionManager.getActiveTrainingSessionId()`).
- No new persistence/database: results live in bounded in-memory histories.
