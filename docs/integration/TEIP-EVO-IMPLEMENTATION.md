# TEIP 1.1 — EVO Architecture and Implementation

> **Status note:** describes the EVO-side **1.1** implementation (WP-3.3, Phase 3). The canonical
> specification is `docs/protocol/TEIP-1.1.md`. TEIP 1.1 conformance state:
> - POST /teip/v1/executions returns 202 (new) / 200 (idempotent duplicate).
> - Canonical request fields (`mode`, `grantedToolScopes`, `caller.system`, object
>   `contextReferences`) plus deprecated aliases (`aiMode`, `mcpToolScopes`, `caller.app`, bare
>   path strings).
> - `VERSION_MISMATCH` (400) for unsupported major versions.
> - Canonical top-level error envelope with canonical codes (`AUTHENTICATION_FAILED`,
>   `AUTHORIZATION_DENIED`, `VALIDATION_FAILED`, `NOT_FOUND`, `CONFLICT` (non-retryable,
>   `details.currentStatus`), `CAPABILITY_UNSUPPORTED` (reserved), `INTERNAL_ERROR`,
>   `SERVICE_UNAVAILABLE` (reserved), `TIMEOUT` (reserved), `METHOD_NOT_ALLOWED`).
> - `nextSequence` cursor = last returned event sequence + 1.
> - Mandatory Bearer authentication; explicit dev-mode opt-out only (`evo.teip.devMode=true`
>   AND loopback caller; `evo.teip.auth.token` for the shared token).
> - Persisted idempotency/execution/event state via `TeipPersistenceStore`
>   (`data/evolution.db`: `teip_executions`, `teip_idempotency`, `teip_events`); restart
>   reconciliation transitions in-flight executions to FAILED (TEIP 1.1 §7.4).
> - Capabilities response emits canonical `capabilities[]` and `availableModels[]`
>   (`models` retained as deprecated alias).
> - Result responses carry `patch` and `modifiedFiles`.
> - `TrystMcpClient` sends `Authorization`, `X-Execution-Id` and `X-Correlation-Id` headers,
>   performs the MCP `initialize` handshake (protocol version `2024-11-05`) and enforces the
>   canonical tool→scope mapping fail-closed before transmission.

## 1. Architectural Alignment and Components

The TRYST–EVOLUTION Integration Protocol (TEIP 1.1) integration in EVO provides a secure, decoupled HTTP communication layer between external task orchestration (TRYST) and EVO's cognitive Darwin and LLM execution engine.

### 1.1 Plugin Placement & OSGi Boundaries
- **Core Controller Bundle (`eu.kalafatic.evolution.controller`)**:
  - `eu.kalafatic.evolution.controller.teip.dto`: DTOs with JSON serialization and validation (`TeipHealthResponse`, `TeipCapabilityDescriptor`, `TeipExecutionRequest`, `TeipAcceptedResponse`, `TeipExecutionStatusResponse`, `TeipEventsResponse`, `TeipExecutionResult`, `TeipError`, `TeipVersionMismatchException`).
  - `eu.kalafatic.evolution.controller.teip.store`: `TeipEventStore` (sequenced, cursor-replayable event store) and `TeipPersistenceStore` (persisted idempotency mapping, execution rows and events in `data/evolution.db`).
  - `eu.kalafatic.evolution.controller.teip.service`: `TeipExecutionManager` governing lifecycle transitions, Darwin/SessionContainer dispatch, capability discovery and restart reconciliation.
  - `eu.kalafatic.evolution.controller.teip.controller`: `TeipApiController` routing the `/teip/v1/*` endpoints with mandatory authentication.
  - `eu.kalafatic.evolution.controller.teip.mcp`: `TrystMcpClient` implementing the scoped MCP JSON-RPC 2.0 tool plane with canonical headers and scope mapping.
  - `eu.kalafatic.evolution.controller.orchestration.EvolutionServer`: Routing `/teip/v1/*` endpoints to `TeipApiController`, as well as serving `/teip` and `/teip.html` web dashboard.
  - `eu.kalafatic.evolution.controller.orchestration.teip.html`: Workstation dashboard styled with `evo-style.css` presenting Overview, Architecture SVG, Sessions, Executions, Capabilities, Events, Analytics, Configuration, and Diagnostics.
- **View Bundle (`eu.kalafatic.evolution.view`)**:
  - `eu.kalafatic.evolution.view.editors.pages.TeipProtocolPage`: Workbench editor page extending `AEvoPage`, embedding SWT `Browser` and registering `teipAction` JS bridges to query in-process Java services directly without localhost network hops.
  - `eu.kalafatic.evolution.view.editors.TeipProtocolPageFactory`: Registers the **TEIP Protocol** tab in `MultiPageEditor.java`.
  - `eu.kalafatic.evolution.view.editors.pages.mcpsettings.TrystIntegrationGroup`: Auxiliary TRYST integration controls in `McpSettingsPage`.

### 1.2 Separation of Concerns
1. **Sessions**: TRYST task IDs and session IDs are mapped to EVO `SessionContainer` (`evo-teip-<executionId>`) and `ServerOperation` (`op-<executionId>`) instances, maintaining single-authority execution lifecycle.
2. **Events**: Internal `RuntimeEventBus` and `TaskContext` events are translated into immutable `TeipEvent` records persisted with monotonically increasing sequence numbers and cursor pagination in `TeipEventStore`; sequences survive restarts through `TeipPersistenceStore`.
3. **Fail-Closed Permissions**: Remote execution requests explicitly declare `grantedToolScopes`. Any MCP call to TRYST tools outside this scope is rejected prior to transmission (`TrystMcpClient`).
4. **Data Retrieval**: Local EVO metrics, sessions, executions, and events are served directly via in-process Java calls from `TeipProtocolPage` to avoid unnecessary self-HTTP overhead. Outbound calls to TRYST MCP are executed asynchronously in background Eclipse Jobs.
5. **Idempotency & Recovery**: The `idempotencyKey → executionId` relation is persisted in `teip_idempotency`; on restart, open executions transition to `FAILED` with an explicit diagnostic message (TEIP 1.1 §7.4).
