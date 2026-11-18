# PHASE-7-WP-7.2-FAMILY-BUILD-INTEGRATION.md

Phase 7 — WP-7.2 Build and Dependency Integration
Date: 2026-11-17
Status: COMPLETE (EVO full test suite completed separately, see WP-7.10)

## 1. Build toolchain

| Item | Value |
|---|---|
| Host | Linux x86_64, NVIDIA GeForce RTX 5070 Ti (16 GiB), driver 595.99.02 / CUDA 13.2 |
| Java | OpenJDK 21 (family default), OpenJDK 25 (ENR CUDA module) |
| Maven | 3.9.12 (wrapper dist). NOTE: system `mvn` 3.8.7 cannot build the Tycho 4.0.8 projects (`tycho-packaging-plugin` requires Maven >= 3.9.0) — environment finding, not a code defect |
| Tycho | 4.0.8 (all RCP projects) |

## 2. Clean build results (commands actually executed)

| Project | Command | JDK | Result | Tests |
|---|---|---|---|---|
| DCode | `mvn -o clean verify` (Maven 3.9.12) | 21 | **SUCCESS** | 124 run, 0 fail, 0 err, 1 skip (117 baseline + 7 new `EnrProviderTest`) |
| TRYST | `mvn -o clean verify` | 21 | **SUCCESS** | 354 run, 0 fail, 0 err, 0 skip |
| DStudio | `mvn -o clean verify` | 21 | **SUCCESS** | 102 run, 0 fail, 0 err, 0 skip |
| ENR | `mvn -o clean install` | 25 (CUDA requires JDK 25; all other modules Java 21) | **SUCCESS** | 68 run, 0 fail, 0 err, 0 skip (incl. 9 CUDA, 5 new family-surface tests) |
| EVO | `mvn -o -pl eu.kalafatic.evolution.enr,eu.kalafatic.evolution.controller,eu.kalafatic.evolution.controller.tests -am clean install -DskipTests` | 21 | **SUCCESS** (compile + packaging) | — |
| EVO | `mvn -o -pl eu.kalafatic.evolution.controller.tests verify` | 21 | see WP-7.10 | see WP-7.10 |

## 3. Dependency integration verification

- **Java 21 compatibility**: all RCP projects compile and run on JDK 21
  (`Bundle-RequiredExecutionEnvironment: JavaSE-21` in every bundle). ENR core is
  Java 21; only the CUDA compute module requires JDK 25 by design (FFM final API),
  per `enr.sh` and `docs/ENR-COMPUTE-BACKENDS.md`.
- **OSGi resolution**: Tycho resolved all target platforms offline against the
  Eclipse 2024-03 release plus JUnit 5.10.2 / JGit 6.9; no resolution errors.
- **Cross-project runtime dependencies**: none at build level — verified by the
  dependency matrix in WP-7.1. Family integration is wire-only (TEIP 1.1, FHC 1.0,
  ENR 1.0).
- **Native libraries**: ENR CUDA backend loads `libcuda.so.1` through FFM and
  embedded PTX kernels (`enr_kernels.ptx`); 9 CUDA tests green after VRAM was freed
  from an unrelated Ollama model load (14.3 GiB) — environmental constraint
  documented in WP-7.11.
- **Runtime packaging**: DCode product (`product/`), DStudio product, TRYST product
  all built; ENR CLI (`enr-cli`, `enr.sh`) launches the standalone server.
- **Startup configuration**: DCode `-Ddcode.home`, ports 48080/48380/48680 with
  fallback; DStudio TEIP 48080/48180; TRYST HTTP 38080/TCP 38280 with dynamic
  fallback 38080–38580; ENR standalone 8111 (`enr.yaml`); ENR family surface 28780
  (EVO `PortRegistry.EVO_ENR`). Known convention conflict DCode/DStudio 48080 → see
  defect D-5.

## 4. Independent operation verification

- Each project builds without the others (separate reactors, no shared reactor).
- ENR standalone server started independently of EVO and served live requests
  (evidence: live run in WP-7.3, WP-7.7).
- DCode headless runtime tests (`HeadlessApiEndToEndTest`, `OsgiWiringTest`) start
  the full harness in Equinox with HTTP server bound — green.
- TRYST `TrystServerApplication` headless start covered by its test suite — green.
- DStudio headless (`DStudioServerApplication` / `-headless`) covered by
  `DStudioLifecycleCoordinatorTest.headlessLifecycleStartsServerAndStopsCleanly` — green.

## 5. Remaining issues

- ENR CUDA module cannot compile under JDK 21 (release 25) — documented design
  decision; documented in D-7.
- System Maven 3.8.7 is insufficient for Tycho 4.0.8 — use the repo-bundled wrapper
  Maven 3.9.12.
