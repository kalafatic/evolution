# TEIP 1.0 — EVO Architecture and Implementation

## 1. Architectural Alignment and Components

The TRYST–EVOLUTION Integration Protocol (TEIP 1.0) integration in EVO provides a secure, decoupled HTTP communication layer between external task orchestration (TRYST) and EVO's cognitive Darwin and LLM execution engine.

### 1.1 Plugin Placement & OSGi Boundaries
- **Core Controller Bundle (`eu.kalafatic.evolution.controller`)**:
  - `eu.kalafatic.evolution.controller.teip.dto`: Pure POJO DTOs with JSON serialization and validation.
  - `eu.kalafatic.evolution.controller.teip.store`: `TeipEventStore` and `TeipExecutionRepository` managing execution persistence, event logging, and idempotency key mapping.
  - `eu.kalafatic.evolution.controller.teip.service`: `TeipExecutionManager` governing lifecycle transitions, Darwin/SessionContainer dispatch, and capability discovery.
  - `eu.kalafatic.evolution.controller.teip.mcp`: `TrystMcpClient` extending standard MCP JSON-RPC 2.0 with scoped permissions.
  - `eu.kalafatic.evolution.controller.orchestration.EvolutionServer`: Routing `/teip/v1/*` endpoints to `TeipApiController`.
- **View Bundle (`eu.kalafatic.evolution.view`)**:
  - `eu.kalafatic.evolution.view.editors.pages.mcpsettings`: `TrystIntegrationGroup` in `McpSettingsPage` providing operators with connection configuration, live diagnostics, MCP discovery, and execution monitoring.

### 1.2 Separation of Concerns
1. **Sessions**: TRYST task IDs and session IDs are mapped to EVO `SessionContainer` and `ServerOperation` instances.
2. **Events**: EVO internal `RuntimeEventBus` and `TaskContext` events are observed and translated into immutable `TeipEvent` records persisted with monotonically increasing sequence numbers.
3. **Fail-Closed Permissions**: Remote execution requests explicitly state `grantedToolScopes`. Any MCP call to TRYST tools outside this scope is rejected.
