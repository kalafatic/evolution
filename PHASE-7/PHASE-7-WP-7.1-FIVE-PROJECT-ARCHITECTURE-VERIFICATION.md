# PHASE-7-WP-7.1-FIVE-PROJECT-ARCHITECTURE-VERIFICATION.md

Phase 7 — WP-7.1 Five-Project Architecture Reconciliation
Date: 2026-11-17
Status: COMPLETE
Method: repository inspection across all five repositories (EVO, TRYST, DStudio,
DCode, ENR) with evidence from source files, bundle manifests, build files and
protocol documents. No execution evidence is claimed where only inspection was
possible.

## 1. The five projects

| Project | Repository | Nature |
|---|---|---|
| EVO | `evolution-121126` | Eclipse RCP/OSGi/Tycho, Java 21, EMF. AI orchestration (Darwin). |
| TRYST | `tryst-121126` | Eclipse RCP/OSGi/Tycho, Java 21, SQLite. Remote development platform. |
| DStudio | `DStudio-121126` | Eclipse RCP/OSGi/Tycho, Java 21, SQLite. Harness platform + TEIP 1.1 provider. |
| DCode | `DCoder-121126` | Eclipse RCP/OSGi/Tycho, Java 21, SQLite. Native coding harness (FHC 1.0). |
| ENR | `evo-native-runtime-161126` | Plain Maven, JDK 21 (CUDA module: JDK 25). Native inference runtime. |

## 2. Dependency direction

Verified from `Require-Bundle`/`pom.xml` across all repositories:

| From \\ To | EVO | TRYST | DStudio | DCode | ENR (standalone) |
|---|---|---|---|---|---|
| EVO | — | none | none | none | none |
| TRYST | none (wire TEIP) | — | none (wire TEIP) | none (wire FHC 1.0 `/api/v1`) | none (wire ENR 1.0 `/enr/v1`) |
| DStudio | none | none | — | none (wire FHC 1.0 `/api/v1`) | none (wire ENR 1.0 `/enr/v1`) |
| DCode | none | none | none | — | none (wire ENR 1.0 `/enr/v1`, added in Phase 7) |
| ENR | none (read-only audit docs only) | none | none | none | — |

- Grep evidence: zero `eu.kalafatic.(evolution|tryst|dstudio|dcode)` imports in the
  ENR standalone repo; zero `ai.enr` imports in EVO/TRYST/DStudio/DCode; zero
  family imports in DCode; zero cross-product imports in DStudio/TRYST bundles.
- All cross-product integration is wire-only: TEIP 1.1 (`/teip/v1`), FHC 1.0
  (DCode `/api/v1`), ENR 1.0 (`/enr/v1`). No circular dependencies exist.

## 3. Module ownership

| Capability | Owner | Evidence |
|---|---|---|
| Coding harness (agent loop, tools, files, execution, events) | DCode | `plugins/eu.kalafatic.dcode.harness` (`ToolCallingAgentLoop`, `ToolEngine`, `SessionExecutor`) |
| Coding tool orchestration | DCode | `harness.tool` registry, 24 tools (11 base + 13 JDT/PDE) |
| Default model inference | ENR | standalone repo kernels + EVO `eu.kalafatic.evolution.enr` facade |
| Model lifecycle (load/unload/discovery) | ENR | `enr-runtime` `ModelManager`, embedded `EnrRuntime` |
| Inference streaming | ENR | `/enr/v1/chat/stream` (SSE) + `/api/v1/inference/stream` |
| Model/backend management | ENR | `Backends` SPI (CPU/CUDA), `ComputeBackend` |
| Application workflows | EVO / TRYST / DStudio | each product's own orchestrators |
| Application sessions | EVO (`SessionManager`), TRYST (`tasks` SQLite), DStudio (`executions` SQLite) | ENR 1.0 §8 |
| Persistence | per product | DCode `dcode.db`, TRYST `tryst.db`, DStudio `dstudio.db`, EVO EMF + data dir |
| Family integration contracts | TEIP 1.1 (EVO `docs/protocol/TEIP-1.1.md`), FHC 1.0 (DCode `PHASE-6-FHC-1.0.md`), ENR 1.0 (EVO `docs/protocol/ENR-1.0.md`) | normative docs |

## 4. DCode integration status (EVO/TRYST/DStudio)

All three applications integrate DCode through their product-local adapter SPIs and
wire clients — no duplicated harness implementation:

- EVO: `DCodeCodingHarnessAdapter` + `DCodeClient` (controller/harness), registered in
  `ToolFactory`, default-selected in `ConnectedApplicationRegistry` (persisted in
  `data/connected-applications.json`); exposed as `DCodeFamilyCapability` and TEIP
  capability `DCODE_HARNESS`.
- TRYST: `DCodeCodingHarnessAdapter` + `DCodeHttpClient` (infrastructure/dcode),
  registered in `ApplicationLifecycleCoordinator`; auto-selected when nothing is
  selected; config persisted in SQLite V20 (`dcode_configurations`); HTTP-managed via
  `/api/integration/applications`.
- DStudio: `DCodeHarnessAdapter` (harness/dcode), registered in
  `DStudioLifecycleCoordinator`; default-selected when nothing is selected;
  `DefaultHarnessRegistry` persistence in `~/.dstudio/dstudio.db`.

Alternatives remain registered everywhere (OpenCode, DStudio TEIP, EVO TEIP, Aider).
No product contains its own agent loop competing with DCode (verified: TRYST's
built-in `CodeAgent` is a pre-existing local fallback explicitly not a connected
application; EVO's Darwin engine is the orchestrator, not a harness).

## 5. ENR integration status

Two ENR implementations exist under the name "ENR":

1. **Embedded EVO bundle** `eu.kalafatic.evolution.enr` — in-process OSGi facade that
   unifies EVO's existing Ollama/llama.cpp/native-EVO stack; serves the family
   `/enr/v1` surface on port 28780 (`EnrMain` standalone entry point exists). Consumed
   by TRYST (`EnrProvider`, provider id `enr-default`, default at startup) and DStudio
   (`EnrInferenceEngineAdapter`, engine id `enr`, default at startup).
2. **Standalone ENR repo** — independent native runtime (own CPU/CUDA kernels, model
   loaders for `.evo` v1/v2 and GGUF, tokenizer, HTTP server). No OSGi, no Ollama, no
   llama.cpp. Served `/api/v1` only until Phase 7.

EVO: ENR wired through the `LlmRouter` ENR slot (`EnrLlmProvider`) — selectable via
`provider=enr`/`enr:` metadata. **ENR is not the silent default for EVO's pre-existing
inference paths**; this is a documented product-owner policy (no-silent-switching rule,
Phase 6 finalization report). Recorded as limitation L-1 below.

DCode: **Phase 7 correction** — DCode previously had no ENR provider at all. Phase 7
adds `EnrProvider` (DCode config bundle, `eu.kalafatic.dcode.config.provider`),
registered as the default provider in `ConfigActivator` alongside Ollama. See
`PHASE-7-WP-7.3-DCODE-ENR-NATIVE-INTEGRATION.md`.

## 6. Duplication audit (non-negotiables)

| Rule | Result |
|---|---|
| No architectural redesign | PASS — no new parallel pipelines introduced |
| No duplicate service implementations | PASS — one provider registry per product (DCode `ProviderRegistry`, DStudio `InferenceEngineRegistry`, TRYST `AiModelRegistryService`), all pre-existing |
| No competing session managers | PASS — EVO `SessionManager` remains sole Darwin session authority; TRYST/DStudio/DCode own their own domain sessions |
| No competing model managers | PASS — ENR owns model lifecycle; DStudio/TRYST only cache discovery metadata |
| No competing coding harnesses | PASS — all products delegate to DCode by default |
| No application-specific replacement for DCode/ENR | PASS |
| No parallel tool registries | PASS — product-local registries per FHC 1.0 §4 |
| No unnecessary direct coupling | PASS — wire-only cross-product integration |
| No circular dependencies | PASS — dependency matrix above is acyclic |
| Java 21 / RCP / OSGi / Tycho preserved | PASS — all RCP projects; ENR plain JDK by design |
| Existing communication/persistence contracts preserved | PASS — TEIP 1.1, FHC 1.0, ENR 1.0 unchanged |

## 7. Findings (defects/limitations discovered)

| ID | Severity | Finding |
|---|---|---|
| D-1 | P1 | DCode had no ENR provider — the mandatory DCode→ENR default path was missing entirely (FIXED in Phase 7, see WP-7.3) |
| D-2 | P1 | Standalone ENR served only its private `/api/v1` surface; family consumers (DCode, TRYST, DStudio) expect ENR 1.0 `/enr/v1` — two incompatible "ENR 1.0" surfaces (FIXED in Phase 7 by adding the `/enr/v1` family surface to the standalone server) |
| D-3 | P2 | Standalone ENR: request timeouts and idle eviction declared but never enforced (FIXED in Phase 7 — housekeeper scheduler + timeout arming) |
| D-4 | P2 | Standalone ENR tokenizer throws an unboxing NPE for out-of-vocabulary input instead of a clear error (FIXED in Phase 7) |
| D-5 | P2 | Port convention collision: DCode HTTP and DStudio TEIP both default to 48080 (flagged to Product Owner; not changed) |
| D-6 | P3 | TRYST doc `TRYST_OPERATIONAL_CONFIGURATION.md` states TCP default 9090; code uses 38280 (doc inconsistency) |
| D-7 | P3 | Standalone ENR CUDA build requires JDK 25 (`maven.compiler.release=25`), while the rest of the family is Java 21 (documented behavior, see enr.sh) |

## 8. Architecture verdict

The five projects conform to the approved family architecture: DCode is the default
coding harness, ENR is the default inference runtime, EVO/TRYST/DStudio consume both
through established contracts, and no project duplicates another's responsibilities.
After the Phase 7 corrections listed above, the mandatory default-path wiring is
complete and verified end-to-end (see WP-7.3, WP-7.4, WP-7.7).
