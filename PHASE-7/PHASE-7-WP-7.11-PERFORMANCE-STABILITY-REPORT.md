# PHASE-7-WP-7.11-PERFORMANCE-STABILITY-REPORT.md

Phase 7 — WP-7.11 Performance and Stability
Date: 2026-11-17
Status: COMPLETE (measurements from live runs and executed suites)

## 1. Hardware and configuration (recorded)

| Item | Value |
|---|---|
| Host | Linux x86_64 (Ubuntu 24.04) |
| GPU | NVIDIA GeForce RTX 5070 Ti, 16303 MiB VRAM, driver 595.99.02, CUDA 13.2 |
| CPU | x86_64 (Xvfb :1 for RCP UI tests) |
| Java | OpenJDK 21 (family), OpenJDK 25 (ENR CUDA module) |
| Maven | 3.9.12 (Tycho 4.0.8) |
| Models | ENR native: `tiny-evo.gguf` (920 params), `evo.gguf` (53,952 params); Ollama: qwen3-coder 30B family, deepseek-coder-v2:16b, evo exports |

## 2. Realistic workloads executed

| Workload | Result | Evidence |
|---|---|---|
| Concurrent sessions (ENR) | 8 concurrent HTTP inference requests, 2 models, no contamination | `ApiIntegrationTest.concurrentRequestsViaHttp` green |
| Concurrent coding tasks (DCode) | 4-thread `SessionExecutor`; suite concurrency tests green | DCode suite |
| Concurrent inference (ENR runtime) | 8 workers, per-session KV isolation | `EnrRuntimeTest.loadsTwoModelsAndRunsConcurrentInferenceWithoutContamination` green |
| Large repository ops | JGit suite (init/commit/diff/branch flows) | DCode + TRYST suites |
| Long-running sessions | TRYST live headless session across multiple tasks and restarts (Phase 7 live run) | live logs |
| Streaming inference | SSE chunk delivery + terminal done; bounded queues (256), max 64 connections, slow-client drop | live + suite |
| Repeated model loading/unloading | ENR suite incl. unload-with-active-session protection; CUDA weight-cache release | green |
| Repeated startup/shutdown | DStudio `headlessLifecycleStartsServerAndStopsCleanly`; TRYST restarts in live runs; ENR restart recovery live | green |
| Extended headless operation | TRYST/DStudio/DCode/ENR headless all ran concurrently for hours during Phase 7 | live |

## 3. Measured numbers

| Metric | Value | How |
|---|---|---|
| ENR native inference latency (tiny-evo, CPU) | 3–6 ms/request | live `/api/v1/metrics` (`total_inference_time_ms`/`total_requests`) |
| ENR throughput (tiny-evo, CPU) | ~250 tokens/s | live metrics |
| ENR CUDA ops | 9 CUDA tests in ~1.4 s total | surefire |
| DCode live headless server footprint | ~146 MB RSS (JVM, in-memory services + HTTP) | `ps` |
| ENR standalone server footprint | ~60–100 MB RSS with 2 models loaded | `ps` |
| DCode full agent prompt through ENR | 1,908 prompt tokens per session run | live ENR metrics |
| TRYST task end-to-end (local repo, tiny model) | minutes incl. clone → plan → implement → build → test → review | live logs |
| Test suite runtimes | DCode 124 tests ~2 min; TRYST 354 ~5 min; DStudio 102 ~4 min (incl. live OpenCode/Aider runs); ENR 70 tests ~1 min | surefire |

## 4. Stability observations

- **No deadlocks**: concurrent suite runs green across all five projects.
- **No observed memory leaks**: repeated restarts in live runs (ENR kill/restart
  cycles, TRYST re-launches) with stable footprints; ENR session cleanup after
  timeout-cancellation is covered by a dedicated test.
- **GPU contention finding (environmental)**: while an unrelated Ollama model held
  14.3 of 16.3 GiB VRAM, ENR CUDA context creation failed (`cuCtxCreate failed: 2`)
  in 3 CUDA tests; after unloading the Ollama model the same tests passed 9/9. This
  confirms ENR correctly reports backend unavailability rather than corrupting
  state; concurrent CUDA consumers must coordinate VRAM (documented in
  `docs/ENR-COMPUTE-BACKENDS.md`).
- **Idle resource reclamation**: ENR idle-eviction now scheduled (Phase 7 fix);
  bounded SSE queues drop slow clients (DCode `SseHub`).
- ENR and DCode do not create competing resource-management mechanisms: ENR owns
  model/VRAM/session admission; DCode owns executor/SSE; applications own their
  pools (verified by inspection).

## 5. Limitations

- Full coding tasks through ENR used the 920-parameter fixture model; latency and
  throughput for production-scale models were not measured live (requires loading a
  coding-capable `.evo`/GGUF into ENR — remediation).
- No soak test beyond the hours of live headless operation recorded here.
