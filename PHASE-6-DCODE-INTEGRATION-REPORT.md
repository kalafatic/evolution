# PHASE-6-DCODE-INTEGRATION-REPORT.md

Phase 6 — DCode (native coding harness) as the default family harness
Date: 2026-11-17

## Status

| Acceptance item | Status |
|---|---|
| DCode works as an independent native harness | **Implemented** (Phase 5), re-verified this phase (117 tests, 0 failures) |
| EVO uses DCode as default harness | **Implemented** — `DCodeCodingHarnessAdapter` + default selection |
| TRYST uses DCode as default harness | **Implemented** — `DCodeCodingHarnessAdapter` + default selection + migration V20 |
| DStudio uses DCode as default harness | **Implemented** — `DCodeHarnessAdapter` + default selection |
| Alternatives remain selectable | **Implemented** — OpenCode/DStudio/Evo adapters stay registered everywhere |
| DCode discoverable through family capabilities | **Implemented** — TEIP capability descriptor `DCODE_HARNESS` (EVO), Darwin `DCodeFamilyCapability`, product-local registries in all three apps |
| DCode never commits in delegated mode | **Preserved** — sessions end in WAITING_FOR_APPROVAL; consuming apps own approval |

## DCode platform (DCoder-121126)

- No product-code changes were required: the existing `/api/v1` REST + SSE surface is the
  conformant FHC 1.0 wire mapping.
- **FHC 1.0 canonicalized** in `PHASE-6-FHC-1.0.md` (DCode is the designated spec
  caretaker): delegation semantics, adapter SPI shape, DCode REST mapping, default-harness
  policy, compatibility rules.
- Deferred (documented, not required by Phase 6): DCode-side TEIP 1.1 server surface
  (DStudio hosts the family TEIP harness platform); 48xxx port-range product-owner
  confirmation.

## EVO integration (evolution-121126)

- `controller/harness/DCodeClient.java` — wire client for the DCode `/api/v1` surface
  (health, workspaces, repositories, sessions, start/cancel, messages, changes).
- `controller/harness/DCodeCodingHarnessAdapter.java` — `APPLICATION_ID=dcode`, type
  `DCODE`, capabilities `CODE_IMPLEMENTATION` + `CODE_ANALYSIS`; executes inside the
  resolved Developer Mode target; polls state with cancellation/timeout; collects
  changed files and summary; maps WAITING_FOR_APPROVAL to a completed result with
  "changes pending review".
- `ToolFactory` registers the adapter; the registry selects DCode when no explicit
  selection exists. `ConnectedApplicationRegistry.findSelectedAdapter()` fallback order
  is now DCode → OpenCode.
- Developer Mode delegates through the existing `CodingHarnessTool` seam — no new
  orchestrator introduced.

## TRYST integration (tryst-121126)

- `infrastructure/dcode/DCodeHttpClient.java` + `DCodeCodingHarnessAdapter.java`
  (implements the TRYST `CodingHarnessAdapter` SPI; wire-only, regex JSON parsing per
  TRYST convention).
- `domain/model/DCodeConfiguration.java` + `domain/repository/DCodeConfigurationRepository.java`
  + `infrastructure/persistence/repository/SqliteDCodeConfigurationRepository.java` +
  migration **V20** (`dcode_configurations`) — persisted connection config, token
  runtime-only (never persisted).
- `ApplicationLifecycleCoordinator`: DCode registered; when nothing is explicitly
  selected, DCode becomes the selected harness (EVO auto-selection preserved only as an
  explicit operator choice).
- TaskWorker/HarnessAiDelegator seam unchanged; TRYST keeps sandbox, review, approval and
  commit ownership. DCode never commits.

## DStudio integration (DStudio-121126)

- `harness/dcode/DCodeHarnessAdapter.java` (implements DStudio `HarnessAdapter` SPI;
  Jackson + JDK HttpClient; config keys endpoint/authToken/providerId/model/timeoutSeconds;
  emits `EXECUTION_STARTED/LOG/COMPLETED/FAILED/CANCELLED` events through the sink;
  returns modified files + diff text for the existing ReviewView flow).
- Registered in `DStudioLifecycleCoordinator.onCoreServiceInitialization()` and selected
  as default when no explicit selection exists (OpenCode/Aider remain registered).
- DCode is manageable through the existing HarnessView (registry-driven) and executable
  through the existing TEIP/execution service; DCode executions persist and reconcile on
  restart exactly like other harnesses.

## Verification

- DCode repo: full reactor build — BUILD SUCCESS, 117 tests, 0 failures, 1 skipped
  (live-model).
- EVO: `DCodeCodingHarnessAdapterTest` 9/9 green (stub wire server).
- TRYST: `DCodeCodingHarnessAdapterTest` 10/10 green (stub wire server + persistence).
- DStudio: `DCodeHarnessAdapterTest` 9/9 green (stub wire server).
- Live cross-product run against the real DCode headless runtime: **not executed in this
  environment** (no live LLM model server available); the wire contracts are covered by
  stub tests and the DCode side is covered by its own Phase 5 suite.

## Known items

1. DCode HTTP and DStudio TEIP share convention port 48080 (loopback, both with fallback
   allocation) — documented; product-owner confirmation pending.
2. The DCode adapter's summary currently uses the last assistant message returned by
   DCode; the FHC result contract allows richer summaries in a future revision.
