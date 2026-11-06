# TEIP 1.0 — Operations and Administration Guide

## 1. Enabling and Disabling TEIP Integration

TEIP 1.0 is integrated natively into EVO's embedded `EvolutionServer`. When EVO starts, TEIP endpoints are automatically available under `/teip/v1`.

### Configuration Parameters
Configuration can be adjusted in the EVO Workbench via the **TRYST Integration** UI panel or system properties:
- `evo.teip.enabled`: (`true` / `false`, default: `true`)
- `evo.teip.auth.token`: Static Bearer token for authenticating inbound TEIP requests.
- `evo.tryst.mcp.url`: Default URL for connecting to TRYST's MCP server (e.g., `http://localhost:3000/mcp`).
- `evo.tryst.timeout.seconds`: MCP call timeout (default: 30 seconds).

## 2. Health and Status Monitoring

Operators can verify the integration at any time:
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

## 3. Troubleshooting & Diagnostics

- **401 Unauthorized:** Check that the `Authorization: Bearer <token>` header matches the configured token in EVO.
- **404 Not Found on Execution:** Ensure the `executionId` is valid and has not expired or been deleted.
- **MCP Tool Invocation Blocked:** Verify that the request included the required tool name inside `grantedToolScopes`.
