# TEIP 1.0 — Operations and Administration Guide

## 1. Enabling and Disabling TEIP Integration

TEIP 1.0 is integrated natively into EVO's embedded `EvolutionServer`. When EVO starts, TEIP endpoints are automatically available under `/teip/v1`.

### Configuration Parameters
Configuration can be adjusted in the EVO Workbench via the **TEIP Protocol** editor tab, the **TRYST Integration** UI group, or system properties:
- `evo.teip.enabled`: (`true` / `false`, default: `true`)
- `evo.teip.auth.token`: Static Bearer token for authenticating inbound TEIP requests (masked in UI).
- `evo.tryst.mcp.url`: Default URL for connecting to TRYST's MCP server (e.g., `http://localhost:3000/mcp`).
- `evo.tryst.timeout.seconds`: MCP call timeout (default: 30 seconds).

## 2. Workbench "TEIP Protocol" Tab
The EVO MultiPageEditor includes a dedicated **TEIP Protocol** tab providing full operational visibility:
- **Overview**: Live status badges, active execution counters, and recent activity logs.
- **Architecture**: Interactive SVG topology diagram illustrating the boundary between TRYST Authority and EVO Authority.
- **Sessions**: Integrated session list correlating TRYST task/session IDs with EVO `SessionContainer` instances.
- **Executions**: Real-time executions list with live status badges (`QUEUED`, `RUNNING`, `COMPLETED`, `CANCELLED`, `FAILED`), progress bars, cancellation controls, and result inspector.
- **Capabilities**: Dynamic catalog of supported execution modes, registered agents, local LLM models, and tools.
- **Events**: Monotonically sequenced replayable event logs with cursor-based pagination.
- **Analytics**: Key execution metrics including success/failure rates and average duration.
- **Configuration**: Dynamic editing of TEIP enable status, TRYST URL, masked auth token, and timeouts.
- **Diagnostics**: Multi-probe diagnostic suite verifying local health, capabilities, TRYST MCP connectivity, and event store integrity.

## 3. Health and Status Monitoring

Operators can verify the integration via command line or REST:
```bash
curl -X GET http://localhost:28080/teip/v1/health
```

Expected output:
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "status": "UP",
  "application": "EVOLUTION",
  "ready": true
}
```

Or view the browser interface directly at `http://localhost:28080/teip`.

## 4. Troubleshooting & Diagnostics

- **401 Unauthorized:** Check that the `Authorization: Bearer <token>` header matches the configured token in EVO.
- **404 Not Found on Execution:** Ensure the `executionId` is valid and has not expired or been deleted.
- **MCP Tool Invocation Blocked:** Verify that the request included the required tool name inside `grantedToolScopes`.
- **TRYST Unavailable:** Check that the TRYST MCP server is running at the configured URL; verify diagnostics in the TEIP Protocol tab.
