# TEIP — TRYST–EVOLUTION Integration Protocol 1.0
Normative Protocol Specification

## 1. Overview and Scope

The TRYST–EVOLUTION Integration Protocol (TEIP) defines the contract between TRYST (the workspace, git, sandbox, and review authority) and EVOLUTION (EVO, the cognitive execution, Darwin search, and LLM authority).

TEIP 1.0 establishes:
- Transport and payload serialization formats.
- Common request and command metadata.
- RESTful HTTP execution endpoints and lifecycle management.
- Dynamic capability discovery.
- Structured external status model and execution results.
- Sequenced, replayable event subscriptions and pagination.
- Predictable error representations.
- Idempotency guarantees and crash recovery semantics.
- Security and scoped authorization boundaries.
- Model Context Protocol (MCP) tool integration over standard HTTP transport.

The normative specification in this document is shared identically across TRYST and EVO implementations.

---

## 2. Transport and Serialization

- **Protocol Name:** TRYST–EVOLUTION Integration Protocol
- **Protocol Identifier:** `TEIP`
- **Protocol Version:** `1.0`
- **Network Transport:** HTTP or HTTPS only. Production deployments should use HTTPS.
- **Payload Format:** UTF-8 encoded JSON (`application/json`).
- **API Base Path:** `/teip/v1`
- **Timestamps:** ISO 8601 UTC string format (e.g., `2026-11-06T18:00:00Z`).
- **Identifiers:** All externally visible identifiers (`requestId`, `correlationId`, `idempotencyKey`, `executionId`, `eventId`, `taskId`, `projectId`, `sessionId`) are opaque strings.
- **Versioning:** API versioning is explicit in the URL path (`/teip/v1`). Backward compatibility is maintained within major version 1.
- **Extensibility:** Unknown optional JSON fields MUST NOT break a compatible receiver.
- **Error Handling:** Unsupported major versions or incompatible requests MUST return a structured protocol error.

---

## 3. Common Metadata

Every TEIP command or execution request MUST include the following metadata fields where applicable:

| Field | Type | Description |
|---|---|---|
| `protocol` | String | Fixed string `"TEIP"` |
| `version` | String | Protocol version, e.g. `"1.0"` |
| `requestId` | String | Globally unique identifier for this individual HTTP request |
| `correlationId` | String | Opaque identifier shared across related requests and events |
| `idempotencyKey` | String | Stable key supplied by caller for retry-safe command dispatch |
| `timestamp` | String | ISO 8601 UTC creation timestamp |
| `caller` | Object | Calling application identity (`app`: `"TRYST"`, `projectId`, `taskId`, `operator`) |

Internal Java DTOs or database entities MUST NOT be used as the wire contract.

---

## 4. Endpoints and Lifecycle

EVO exposes the following endpoints under `/teip/v1`:

### 4.1 Health Check
- **Route:** `GET /teip/v1/health`
- **Description:** Returns the live operational status of EVO and integration readiness.
- **Response Schema:**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "status": "UP",
  "application": "EVOLUTION",
  "appVersion": "2.6.5",
  "timestamp": "2026-11-06T18:00:00Z",
  "ready": true,
  "details": {
    "serverPort": 28080,
    "activeExecutions": 0,
    "mcpReady": true
  }
}
```
*Note:* Must return status `"DEGRADED"` or `"DOWN"` if essential dependencies are unavailable.

### 4.2 Capability Catalog
- **Route:** `GET /teip/v1/capabilities`
- **Description:** Dynamically enumerates active execution capabilities, AI modes, agents, models, and tools.
- **Response Schema:**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "timestamp": "2026-11-06T18:00:00Z",
  "supportedModes": [
    "DARWIN_MODE",
    "SELF_DEV_MODE",
    "ASSISTED_CODING",
    "SIMPLE_CHAT",
    "INTENT_RECONSTRUCTION",
    "HYBRID_MANUAL_EXPORT",
    "FORGE"
  ],
  "agents": [
    {"name": "JavaDevAgent", "description": "Java development and refactoring agent"},
    {"name": "TesterAgent", "description": "Unit and integration test synthesis agent"}
  ],
  "models": [
    {"id": "llama3.2:3b", "available": true, "provider": "Ollama"}
  ],
  "tools": [
    {"name": "list_files", "description": "List repository files", "readOnly": true}
  ],
  "features": {
    "asyncExecution": true,
    "eventStreaming": true,
    "cancellation": true
  }
}
```

### 4.3 Create Execution
- **Route:** `POST /teip/v1/executions`
- **Description:** Submits a new cognitive or development execution request.
- **Request Schema:**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "requestId": "req-1001",
  "correlationId": "corr-5001",
  "idempotencyKey": "idem-task-42-try-1",
  "timestamp": "2026-11-06T18:00:00Z",
  "caller": {
    "app": "TRYST",
    "projectId": "proj-evo-client",
    "taskId": "task-789",
    "operator": "engineer1"
  },
  "capability": "CODE_IMPLEMENTATION",
  "mode": "DARWIN_MODE",
  "instruction": "Implement user authentication filter according to specification",
  "parameters": {
    "maxIterations": 3,
    "temperature": 0.2
  },
  "grantedToolScopes": [
    "tryst:read_file",
    "tryst:list_files",
    "tryst:run_tests"
  ],
  "contextReferences": [
    {"type": "FILE", "path": "src/auth/Filter.java", "description": "Target interface"}
  ]
}
```
- **Accepted Response Schema (HTTP 202 Accepted):**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "executionId": "exec-9a1b2c3d",
  "status": "QUEUED",
  "correlationId": "corr-5001",
  "createdAt": "2026-11-06T18:00:00Z",
  "links": {
    "status": "/teip/v1/executions/exec-9a1b2c3d",
    "events": "/teip/v1/executions/exec-9a1b2c3d/events",
    "result": "/teip/v1/executions/exec-9a1b2c3d/result",
    "cancel": "/teip/v1/executions/exec-9a1b2c3d/cancel"
  }
}
```

### 4.4 Get Execution Status
- **Route:** `GET /teip/v1/executions/{executionId}`
- **Response Schema:**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "executionId": "exec-9a1b2c3d",
  "status": "RUNNING",
  "correlationId": "corr-5001",
  "progress": 0.45,
  "statusMessage": "Evaluating candidate variant 2",
  "createdAt": "2026-11-06T18:00:00Z",
  "startedAt": "2026-11-06T18:00:01Z",
  "completedAt": null
}
```

### 4.5 Get Execution Result
- **Route:** `GET /teip/v1/executions/{executionId}/result`
- **Response Schema (Completed):**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "executionId": "exec-9a1b2c3d",
  "correlationId": "corr-5001",
  "status": "COMPLETED",
  "summary": "Implemented authentication filter and validated test suites",
  "completedAt": "2026-11-06T18:01:30Z",
  "artifacts": [
    {"name": "AuthFilter.java", "type": "SOURCE_PATCH", "content": "..."}
  ],
  "metrics": {
    "durationMs": 90000,
    "iterations": 2,
    "fitnessScore": 0.94
  }
}
```

### 4.6 Cancel Execution
- **Route:** `POST /teip/v1/executions/{executionId}/cancel`
- **Request Schema:**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "requestId": "req-1002",
  "reason": "Cancelled by operator via TRYST review UI"
}
```
- **Response Schema:**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "executionId": "exec-9a1b2c3d",
  "status": "CANCELLED",
  "message": "Execution successfully cancelled"
}
```

### 4.7 Execution Events & Subscription
- **Route:** `GET /teip/v1/executions/{executionId}/events?afterSequence={n}&limit={m}`
- **Response Schema:**
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "executionId": "exec-9a1b2c3d",
  "events": [
    {
      "eventId": "evt-1",
      "executionId": "exec-9a1b2c3d",
      "sequence": 1,
      "type": "EXECUTION_STARTED",
      "timestamp": "2026-11-06T18:00:01Z",
      "correlationId": "corr-5001",
      "data": {"mode": "DARWIN_MODE"}
    },
    {
      "eventId": "evt-2",
      "executionId": "exec-9a1b2c3d",
      "sequence": 2,
      "type": "PROGRESS_UPDATED",
      "timestamp": "2026-11-06T18:00:15Z",
      "correlationId": "corr-5001",
      "data": {"progress": 0.3, "message": "Analyzing dependencies"}
    }
  ],
  "hasMore": false,
  "nextSequence": 3
}
```

---

## 5. Lifecycle and Status Model

The external status model is strictly defined by:
- `QUEUED`: Accepted and scheduled for execution.
- `RUNNING`: Actively processing steps, iterations, or tool operations.
- `WAITING`: Temporarily blocked awaiting input, approval, or external decision.
- `COMPLETED`: Successfully finalized with structured results.
- `FAILED`: Terminated with an error or unrecoverable condition.
- `CANCELLED`: Voluntarily aborted via explicit cancellation request.

---

## 6. Error Model

All error responses return a standardized JSON schema:
```json
{
  "protocol": "TEIP",
  "version": "1.0",
  "code": "VALIDATION_FAILED",
  "message": "Missing required field: instruction",
  "requestId": "req-1003",
  "correlationId": "corr-5001",
  "retryable": false,
  "timestamp": "2026-11-06T18:00:00Z",
  "details": {
    "field": "instruction",
    "issue": "NotBlank"
  }
}
```

Standard Error Codes:
- `AUTHENTICATION_FAILED` (401)
- `FORBIDDEN` (403)
- `VALIDATION_FAILED` (400)
- `NOT_FOUND` (404)
- `CONFLICT` (409)
- `UNSUPPORTED_CAPABILITY` (422)
- `EXECUTION_FAILED` (500)
- `SERVICE_UNAVAILABLE` (503)
- `TIMEOUT` (504)

---

## 7. Idempotency and Recovery

1. Execution requests with the same non-empty `idempotencyKey` MUST return the existing accepted execution without creating a duplicate process.
2. The relation between `idempotencyKey` and `executionId` is persisted.
3. Cancellation is idempotent; calling cancel multiple times on an already cancelled or terminal execution succeeds without error.
4. After an unexpected restart, executions that cannot be resumed cleanly transition to `FAILED` or `CANCELLED` with explicit diagnostic messages.

---

## 8. Security and Scoping

1. Requests must carry an authorization token (e.g. `Authorization: Bearer <token>`).
2. Tools can only be invoked if present in `grantedToolScopes`.
3. Secret credentials, absolute filesystem paths outside designated sandboxes, and unrestricted shell execution are strictly disallowed.
4. Diagnostic logs and event streams MUST redact tokens, credentials, and sensitive content.

---

## 9. Model Context Protocol (MCP) Over HTTP

1. EVO connects to the TRYST MCP server using standard JSON-RPC 2.0 over HTTP.
2. Standard methods supported:
   - `initialize` (MCP version `2024-11-05`)
   - `tools/list`
   - `tools/call`
3. Tool invocations MUST be associated with:
   - `executionId`
   - `caller.taskId`
   - `correlationId`
4. Invocations of tools outside `grantedToolScopes` MUST be refused by the client adapter prior to transmission.
