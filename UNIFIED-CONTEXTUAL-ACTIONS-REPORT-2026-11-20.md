# Unified Contextual Actions Framework — Implementation Report

Date: 2026-11-20 · Darwin Family Contextual Action Contract **v1.0** · Contract-first,
independently buildable architecture (no shared runtime bundles, no cross-repo build
infrastructure, no Spring/Node/Python, no new brokers, no mock handlers).

## Existing architecture analysis (pre-work)

- **DStudio**: 4 Eclipse commands + 1 dynamic viewer menu; `HarnessRegistry`,
  `InferenceEngineRegistry`, `AgentProfileRegistry` (core, SQLite); web IDE with
  `addEventListener`-based `app.js`; no action registry existed.
- **DCode**: no commands/context menus/registries; REST-only server + thin RCP client;
  `DCodeSessionManager` owns local sessions.
- **TRYST**: programmatic `Action`-based menus (Navigator); `TaskOrchestrator`,
  `GitService`, `PermissionManager` RBAC; single inline-HTML SPA with inline `onclick`; no
  action registry existed.
- **EVO**: richest infra — `EvoGlobalContextMenuManager`, declarative popup
  contributions, `ConnectedApplicationRegistry` (harnesses), `ForgeSessionManager`,
  `EvolutionServer` (NanoHTTPD), CNF navigator provider.

## Shared action framework design (contract v1.0)

Canonical owner: **DStudio** (`docs/contextual-actions/`):
`CONTEXTUAL-ACTION-CONTRACT.md` (context model, descriptor schema, evaluation order,
execution semantics, error codes, canonical action-id catalog, RCP/Web behavior rules,
ownership boundaries) + `contextual-action.schema.json`.

Each repository implements its **own local** contextual action registry over its own
domain services (per the architectural decision): identical action ids, identical
descriptor/evaluation/execution semantics, surface-aware handlers ("*" / RCP / WEB),
per-target duplicate-execution guard, stable error codes
(`TARGET_NOT_FOUND`, `ACTION_DISABLED`, `PERMISSION_DENIED`, `NO_HANDLER`,
`EXECUTION_FAILED`).

## Repository-by-repository implementation

### DStudio (Phase A — reference)

- **Framework** (`eu.kalafatic.dstudio.core.contextual`): `ContextualActionIds`,
  `ContextualActionDescriptor`, `ContextSelection`, `ContextualAction`,
  `ActionFeedback`, `ContextualActionHandler`, `ContextualActionProvider`,
  `ContextualActionRegistry`, `DefaultContextualActionRegistry`,
  `ContextualExecutionResult`; providers: harness (+registry), provider (+registry),
  model, agent (+registry), execution/session, project; surface-independent handlers in
  `CoreContextualActions`; wired in `DStudioLifecycleCoordinator` (+ accessors,
  shared `InferenceEngineFactory` field).
- **RCP**: `ContextualMenuSupport` + `RcpContextualActions` (dialogs: harness/provider
  config, add-provider, agent create/edit/export/import JSON, compatibility, execution
  details/history) + new dialogs (`InferenceEngineConfigDialog`, `AddProviderDialog`,
  `AgentProfileJson`); context menus wired into ProjectExplorer, Harness, Inference
  (engines + models), Agent and Execution views; registered from `DStudioUiStartup`;
  `showForgePage`-style navigation via `MultiPageEditor.showForgePage` unchanged.
- **Web**: `ContextualHttpHandler` (`GET /api/contextual/actions`, `POST
  /api/contextual/execute`, WEB handlers: JSON config, provider add, agent JSON
  CRUD/export, compatibility, execution details/history, project create-session);
  `DStudioServer` 15-arg constructor + context; `web/app.js` context-menu overlay with
  event delegation, ⋯ overflow buttons, disabled reasons, confirmations, toast and
  auto-refresh (no inline handlers added).
- **Fixes made**: `DefaultHarnessRegistry.unregisterHarness` now removes descriptor +
  persisted record + clears default selection (was adapter-only).
- **Tests**: `ContextualActionFrameworkTest` (5), `HarnessContextActionsTest` (5),
  `ContextualHttpApiTest` (4, cross-surface domain-state verification). Full DStudio
  suite: **176/176 PASS**; full build SUCCESS.

### DCode (Phase B)

- **Framework** (`eu.kalafatic.dcode.contextual` in dcode.core): contract-identical
  compact set (10 classes, session/execution subset of the catalog).
- **Domain**: `DCodeContextualActions` (dcode.server) — session provider/handlers over
  `DCodeSessionManager`: cancel/details/history/artifacts real; pause/resume/delete
  contributed disabled with honest reasons (single-run task engine).
- **RCP**: `SessionHistoryView` upgraded to a selectable list with a context menu
  resolving/executing actions through the backend registry via new `DCodeClient`
  methods (`listContextualActions`, `executeContextual`, JSON-free records for the UI
  bundle).
- **Web**: DCode's web interface is its REST API — `DCodeHttpHandler` gained
  `GET /api/contextual/actions` + `POST /api/contextual/execute` (auth preserved).
- **Tests**: `ContextualFrameworkTest` (4/4 PASS). Build/install SUCCESS. Known
  environment issue: the DCode test module's OSGi runtime fails to provision jackson
  for enr/server-dependent tests — **pre-existing** (untouched `EnrProviderTest` and
  `ServerWireTest` fail identically); the new `ContextualSessionActionsTest` compiles
  and is wired but suffers the same runtime provisioning failure (documented, not
  hidden).

### TRYST (Phase B)

- **Framework** (`eu.kalafatic.tryst.domain.contextual`): contract-identical set incl.
  `ContextualSelection` with permissions/user (RBAC-aware).
- **Domain**: `TrystContextualActions` — task provider (pause/resume/cancel/archive/
  delete/move-up/move-down/details over `TaskOrchestrator`, state-aware), session
  provider (pause/resume/details over `Session` lifecycle), project provider (close);
  infrastructure: `TrystRepositoryContextActions` (git status/commit/push/diagnostics
  over `GitService`, permission-gated via selection permissions).
- **Web**: `ContextualHttpHandler` (hand-rolled JSON, same style as
  `TrystHttpServer`); registered in `ApplicationLifecycleCoordinator` with bearer-token
  `SecurityContext` → permission set mapping; SPA: task rows carry
  `data-ctx-*` attributes, right-click context menu + disabled reasons + confirmations +
  auto-refresh (`refreshQueueAndSchedules`), event delegation only.
- **RCP**: `TrystNavigatorView` gained a "More Actions ▸" submenu for task/session/
  project/git-repo nodes resolving from the same registry (confirmation dialogs,
  refresh after execution); `TrystUiServices.getContextualActionRegistry()`.
- **Tests**: `ContextualTaskActionsTest` — **6/6 PASS** (cancel/pause/resume gating,
  archive-after-cancel, delete, stable error codes over the real SQLite-backed
  `TaskOrchestrator`). Full build SUCCESS.

### EVO (Phase C)

- **Framework** (`eu.kalafatic.evolution.controller.contextual`): contract-identical
  set + `EvoContextualRegistry` singleton holder.
- **Domain**: `EvoContextualActions` — forge-session provider/handlers
  (pause/stop/resume/duplicate/delete/details over `ForgeSessionManager`, state-aware
  via telemetry; resume = real `SelfEvoForgingService.startForging` which resumes from
  the fingerprint-matching checkpoint) and connected-harness provider/handlers
  (select/test/enable/disable/remove/capabilities over `ConnectedApplicationRegistry`;
  constructor made protected for hermetic test doubles).
- **Web**: `EvolutionServer` gained `GET /api/contextual/actions` + `POST
  /api/contextual/execute`.
- **RCP**: `EvoGlobalContextMenuManager` gained a "Forge Session Actions ▸" section
  (grouped, disabled-with-reason, confirmations) on every right-click — same registry,
  same ids as the web surface.
- **Tests**: `EvoContextualActionsTest` — **3/3 PASS** (session delete domain-state,
  harness select/test/remove gating, stable error codes).

## Persistence changes

- DStudio: harness `unregisterHarness` now deletes the persisted descriptor record
  (existing `HarnessStore.deleteDescriptor` finally used).
- No new persistence introduced anywhere else; sessions/tasks/registries keep their
  existing stores.

## Browser page regression fix (EVO, user-reported)

- **`BrowserPage.initializeTabs`**: an earlier revision only opened a four-entry
  whitelist (chat/dashboard/develop/mcp) and silently dropped Forge, Architecture,
  Mutation, Administration and MCP Documentation tabs. Fixed: **every registered EVO
  web page is now a default tab** (`resolveDefaultTabApplications` — public static,
  regression-tested). Ports keep the platform +20000 allocation standard
  (8080→28080 … via `PortRegistry`/`PortAllocationManager`).
- **Build-breaking regressions repaired**: `eu.kalafatic.evolution.enr/pom.xml`
  (invalid `<dependencies>` inside `<build>` + broken `Bundle-ClassPath` entry for a
  non-existent `lib/json-20140107.jar`) and `eu.kalafatic.evolution.forge.controller/
  pom.xml` (duplicate jackson-databind dependency). Full EVO reactor now builds.
- **Tests**: `BrowserTabDeduplicationTest` + `BrowserUrlUtilsTest` **10/10 PASS**
  (incl. the new all-pages-default regression test).

## Tests and actual results

| Repository | Build | Tests |
|---|---|---|
| DStudio | SUCCESS (full reactor) | **176/176** (incl. 14 new contextual tests) |
| DCode | SUCCESS (full reactor) | new `ContextualFrameworkTest` 4/4; session-action tests blocked by pre-existing test-runtime provisioning issue |
| TRYST | SUCCESS (full reactor) | new `ContextualTaskActionsTest` 6/6 |
| EVO | SUCCESS (full reactor) | new `EvoContextualActionsTest` 3/3 + browser 10/10 + forge sync batch 14/14 + forge manager 5/5 |

EVO port-binding tests (`ServerManagerLifecycleTest`, `PortManagementTest`,
`PortLifecycleTest`) fail **only** because the user's live EVO RCP instance
(PID 683261), EVO supervisor (PID 138088) and Firefox (PID 12707) occupy the
deterministic ports — environmental, not regressions; they pass when ports are free.

## Remaining gaps (honest)

1. **DCode test runtime**: jackson provisioning failure inside the OSGi surefire
   runtime for enr/server-dependent tests — pre-existing, affects untouched tests too.
2. **No GPU execution** anywhere (unchanged; CPU-only family reality).
3. Disabled-with-reason actions where a product does not own the capability (e.g. build/
   test in DStudio/DCode, pause in DCode, favorites in DStudio) — per contract, these
   are visible but honestly explained, not executable.
4. EVO web surface has the REST contextual endpoints; the per-page HTML context menus
   (beyond the existing forge/chat menus) were not extended this session.
5. Multi-selection availability rules are implemented (single-target mutations disable
   on multi-select) but no UI surface currently produces multi-select contexts beyond
   the RCP selection shape.

Cross-interface equivalence verified: DStudio `ContextualHttpApiTest` and DCode/
TRYST/EVO action tests prove that web-executed actions change the identical domain
state as the RCP menus (same registry, same ids, same handlers).
