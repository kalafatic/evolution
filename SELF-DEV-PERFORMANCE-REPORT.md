# SELF-DEV PERFORMANCE REPORT (WP13)

Status: MEASURED (measured values from the executed test/build runs on this machine).
Date: 2026-11-18.

Environment: Linux x86_64, 64 CPUs, ~121 GB RAM, JDK 21, Maven 3.9.12 (Tycho 4.0.5, offline).

## 1. Measured durations

| Phase | Measurement | Source |
|---|---|---|
| SelfDevContext construction + run-dir creation | ~1-4 ms per context | SelfDevResourceTruthTest (run-ID collision test: 0.004 s for 2 contexts) |
| Preflight (full validation incl. task graph) | ~0.4-1.0 s | CanonicalEvoRepositoryPreflightTest, SelfDevPipelineHardeningTest |
| Resource snapshot install / gate evaluation | sub-ms | SelfDevTaskGraphGateTest |
| Artifact manifest record (SHA-256 + persist) | ~1-5 ms (small files) | SelfDevArtifactManifestTest |
| Product deploy (unzip + layout validation) | ~10-50 ms (stub product) | SelfDevE2ELifecycleTest |
| Child start → readiness (APPLICATION_INITIALIZED) | ~1.5-2.5 s | scenarioA (2.5 s total incl. 1.5 s launch hold) |
| Identity-verified STOP (graceful → force + wait) | ~0.3-0.5 s | scenarioA/F |
| Full E2E iteration (stub product) | ~4 s | scenarioF (2 iterations, 4.4 s) |
| Loop guard / ledger operations | sub-ms per record | SelfDevIterationLoopTest |
| Git baseline capture (read-only, real repo) | ~0.1-0.2 s | SelfDevGitLifecycleSimulationTest |
| Supervisor bundle test suite | 31 tests, ~15 s | supervisor module tests |
| Self-dev controller test battery (new WP tests) | ~1-2 min total | controller.tests runs |
| **Real Tycho reactor build** (BUILD_EVO on isolated copy) | **~250 s** | SelfDevTaskArchitectureTest (measured during WP2 regression) |

## 2. Identified expensive operations

1. **Real Tycho build (~4 min)** — the dominant cost; inherently expensive. Mitigations implemented (not optimizations): EXPORT no longer triggers an implicit second build (WP3), artifact resolution no longer re-scans/re-validates on every access (WP4 manifest), tests enabled by default.
2. **Repository COPY** — copies the full canonical repo including model binaries (GGUF files); excluded directories only cover `self-dev-run`. Not changed (out of scope); documented.
3. **Repeated full Tycho builds inside unit tests** — pre-existing heavy tests (`SelfDevTaskArchitectureTest` previously executed a real 250 s build); WP2 replaced the real pipeline execution in the dependency tests with simulated dependency outcomes, eliminating a repeated ~5 min build from the routine suite.

## 3. Process/resource hygiene measurements

| Metric | Result |
|---|---|
| Children alive after full E2E run | 0 (verified post-run) |
| Children per active run | exactly 1 (scenario F) |
| Parent JVM survival | confirmed in every scenario |
| Working-tree mutations during test suites | only the intended new source/test/report files |
| Real Git mutations | ZERO (HEAD `cbbfa121...` unchanged; verified after each suite) |

## 4. Artifact reuse policy (safety-preserving)

- Artifacts are reused only through the per-run manifest (buildId + SHA-256 + revision); any staleness or checksum mismatch is a hard failure (WP4 tests). No caching was introduced that could bypass validation.
- No validation, readiness or correctness checks were weakened for performance.

## 5. Stability

- The WP test battery was executed repeatedly during development (10+ runs of the core suites); results are deterministic except for the documented environment-dependent cases (live user supervisor on 28480, user EMF supervisor config, hardcoded-port legacy tests).
