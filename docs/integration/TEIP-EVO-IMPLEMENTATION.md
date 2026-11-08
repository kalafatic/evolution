# TEIP 1.0 — EVO Architecture and Implementation

## 1. Architectural Alignment and Components

The TRYST–EVOLUTION Integration Protocol (TEIP 1.0) integration in EVO provides a secure, decoupled HTTP communication layer between external task orchestration (TRYST) and EVO's cognitive Darwin and LLM execution engine.

### 1.1 Plugin Placement & OSGi Boundaries
- **Core Controller Bundle (`eu.kalafatic.evolution.controller`)**:
  - `eu.kalafatic.evolution.controller.teip.dto`: Pure POJO DTOs with JSON serialization and validation (`TeipHealthResponse`, `TeipCapabilityDescriptor`, `TeipExecutionRequest`, `TeipExecutionStatusResponse`, `TeipEventsResponse`, `TeipExecutionResult`).
  - `eu.kalafatic.evolution.controller.teip.store`: `TeipEventStore` and `TeipExecutionRepository` managing execution persistence, event logging, and idempotency key mapping.
  - `eu.kalafatic.evolution.controller.teip.service`: `TeipExecutionManager` governing lifecycle transitions, Darwin/SessionContainer dispatch, and capability discovery.
  - `eu.kalafatic.evolution.controller.teip.mcp`: `TrystMcpClient` extending standard MCP JSON-RPC 2.0 with scoped permissions.
  - `eu.kalafatic.evolution.controller.orchestration.EvolutionServer`: Routing `/teip/v1/*` endpoints to `TeipApiController`, as well as serving `/teip` and `/teip.html` web dashboard.
  - `eu.kalafatic.evolution.controller.orchestration.teip.html`: Workstation dashboard styled with `evo-style.css` presenting Overview, Architecture SVG, Sessions, Executions, Capabilities, Events, Analytics, Configuration, and Diagnostics.
- **View Bundle (`eu.kalafatic.evolution.view`)**:
  - `eu.kalafatic.evolution.view.editors.pages.TeipProtocolPage`: Workbench editor page extending `AEvoPage`, embedding SWT `Browser` and registering `teipAction` JS bridges to query in-process Java services directly without localhost network hops.
  - `eu.kalafatic.evolution.view.editors.TeipProtocolPageFactory`: Registers the **TEIP Protocol** tab in `MultiPageEditor.java`.
  - `eu.kalafatic.evolution.view.editors.pages.mcpsettings.TrystIntegrationGroup`: Auxiliary TRYST integration controls in `McpSettingsPage`.

### 1.2 Separation of Concerns
1. **Sessions**: TRYST task IDs and session IDs are mapped to EVO `SessionContainer` (`evo-teip-<executionId>`) and `ServerOperation` (`op-<executionId>`) instances, maintaining single-authority execution lifecycle.
2. **Events**: Internal `RuntimeEventBus` and `TaskContext` events are translated into immutable `TeipEvent` records persisted with monotonically increasing sequence numbers and cursor pagination in `TeipEventStore`.
3. **Fail-Closed Permissions**: Remote execution requests explicitly declare `grantedToolScopes`. Any MCP call to TRYST tools outside this scope is rejected prior to transmission.
4. **Data Retrieval**: Local EVO metrics, sessions, executions, and events are served directly via in-process Java calls from `TeipProtocolPage` to avoid unnecessary self-HTTP overhead. Outbound calls to TRYST MCP are executed asynchronously in background Eclipse Jobs.
