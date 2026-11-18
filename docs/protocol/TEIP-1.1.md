# TEIP — Tryst-Evolution-Integration-Protocol 1.1

Canonical Protocol Specification

**Status:** NORMATIVE — this is the single authoritative specification of TEIP 1.1. It supersedes
all TEIP 1.0 specification copies in every repository (the 1.0 copies are retained only as
historical references and are explicitly marked as superseded).

**Governance:** TEIP is a family-level protocol (the family products are EVO, TRYST, DStudio and
DCode). Changes to this specification require: (a) a recorded change with justification, (b) a
conformance wire-test update shipped in the same change, (c) sign-off by all affected product
owners. No product may ship a private normative variant. The consolidation record for the
1.0→1.1 change (verified divergences and their resolutions) is the changes table in §1 below.

**Product names:** EVO (Evolution), TRYST (TRYST), DStudio (Darwin Studio), DCode (Darwin Code —
Native Coding Harness), TEIP (Tryst-Evolution-Integration-Protocol).

---

## 1. Overview and Scope

TEIP defines the contract between family products: task and execution delegation, status and
event exchange, result exchange, capability discovery, correlation, errors, authentication and
scoped tool access. TEIP is a protocol, not a product, runtime or coding harness.

TEIP 1.1 is the bugfix/consolidation of TEIP 1.0. It resolves the verified wire
incompatibilities between the TRYST and EVO implementations (status codes, field names, event
cursors, payload shapes, error model, authentication enforcement) without adding new endpoints.
Family-wide extensions (family task-lifecycle messages, push streaming, additional participant
roles) are 2.0 material and are explicitly out of scope for 1.1.

**1.0 → 1.1 summary of changes:**

| Area | 1.0 state | 1.1 canonical |
|---|---|---|
| Create execution response | divergent: 201/200 (TRYST copy) vs 202 (EVO copy) | **202 Accepted** (new), **200 OK** (idempotent duplicate); clients MUST accept 200/201/202 |
| AI mode field | `aiMode` (TRYST) vs `mode` (EVO) | **`mode`** (`aiMode` accepted as deprecated alias) |
| Scope field | `mcpToolScopes` vs `grantedToolScopes` | **`grantedToolScopes`** (`mcpToolScopes` accepted as deprecated alias) |
| Caller field | `caller.app` (EVO copy) vs `caller.system` (TRYST) | **`caller.system`** (`app` accepted as deprecated alias) |
| Context references | strings vs objects | **objects** `{type, path, description}`; bare strings accepted as path shorthand |
| Events cursor | `latestSequence` vs `nextSequence` | **`nextSequence`** (last event sequence + 1; 0 when no events) |
| Health/capabilities/status/result | divergent shapes | canonical union shapes (§4.1, §4.2, §4.4, §4.6) |
| Error body | top-level (EVO) vs nested `error{}` (TRYST copy) | **top-level** code/message envelope (§5) |
| Error codes | divergent lists | canonical union list (§5) |
| Authentication | opt-in (EVO implementation) / unenforced (TRYST MCP) | **mandatory**; explicit dev-mode opt-out only |
| Idempotency | in-memory (EVO implementation) | **persisted** idempotency mapping |
| MCP `initialize` | divergent support | **required** (protocol version `2024-11-05`) |

## 2. Transport and Serialization

- **Protocol Identifier:** `TEIP`
- **Protocol Version:** `1.1` (carried in the envelope `version` field)
- **Network Transport:** HTTP/1.1 or HTTP/2. TLS in production; plain HTTP allowed for local
  development only.
- **Payload Format:** UTF-8 JSON (`application/json`).
- **API Base Path:** `/teip/v1` (unchanged from 1.0).
- **Timestamps:** ISO 8601 UTC strings (e.g., `2026-11-06T18:00:00Z`).
- **Identifiers:** all externally visible identifiers (`requestId`, `correlationId`,
  `idempotencyKey`, `executionId`, `eventId`, `taskId`, `projectId`, `sessionId`) are opaque
  strings.
- **Extensibility:** receivers MUST ignore unknown optional JSON fields (open-world rule).
- **Version mismatch:** an unsupported major version returns a structured protocol error with
  code `VERSION_MISMATCH` (§5). Minor versions within the same major version are
  backward-compatible: additive changes only, deprecated aliases retained for at least two
  minor releases.

## 3. Common Metadata

Every mutating or contextual TEIP command carries:

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "requestId": "req-12345678-abcd-ef01-2345-6789abcdef01",
  "correlationId": "corr-87654321-4321-4321-4321-cba987654321",
  "idempotencyKey": "idem-task-101-stage-analyze",
  "timestamp": "2026-11-06T18:00:00Z",
  "caller": {
    "system": "TRYST",
    "projectId": "proj-abc",
    "taskId": "task-xyz",
    "operator": "engineer1"
  }
}
```

- `caller.system` is canonical; `caller.app` is a deprecated alias accepted during the 1.0→1.1
  transition.
- `caller.operator` is optional and MUST NOT carry secrets.
- Internal Java DTOs or database entities MUST NOT be used as the wire contract.

## 4. Endpoints and Lifecycle

EVO exposes the endpoints below under `/teip/v1`.

### 4.1 Health Check — `GET /teip/v1/health`

Returns live operational status and protocol compatibility.

**Response (200 OK):**

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "status": "UP",
  "application": "EVOLUTION",
  "appVersion": "2.6.5",
  "engineVersion": "0.1.0",
  "nodeId": "evo-worker-01",
  "uptimeSeconds": 86400,
  "ready": true,
  "timestamp": "2026-11-06T18:00:00Z",
  "details": {
    "serverPort": 28080,
    "activeExecutions": 0,
    "mcpReady": true
  }
}
```

- `status` ∈ `UP` | `DEGRADED` | `DOWN` | `DISABLED`. Servers MUST return `DEGRADED`/`DOWN`
  when essential dependencies are unavailable.
- Required fields for clients: `status`, `protocol`, `version`. All other fields are optional;
  receivers MUST tolerate their absence.
- `engineVersion`, `nodeId`, `uptimeSeconds` are optional additions (from the 1.0 TRYST shape).

### 4.2 Capability Catalog — `GET /teip/v1/capabilities`

Dynamically enumerates execution capabilities, modes, agents, models and tools.

**Response (200 OK):**

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "timestamp": "2026-11-06T18:00:00Z",
  "capabilities": [
    {
      "id": "CODE_IMPLEMENTATION",
      "name": "Autonomous Code Modification",
      "supportedModes": ["DEFAULT", "INCREMENTAL"],
      "requiredScopes": ["read:workspace", "write:workspace", "execute:build"]
    },
    {
      "id": "CODE_ANALYSIS",
      "name": "Codebase Analysis & Inspection",
      "supportedModes": ["DEFAULT", "DEEP_REASONING"],
      "requiredScopes": ["read:workspace", "read:task"]
    }
  ],
  "supportedModes": ["DARWIN_MODE", "SELF_DEV_MODE", "ASSISTED_CODING", "SIMPLE_CHAT", "DEVELOPER"],
  "agents": [
    {"name": "JavaDevAgent", "description": "Java development and refactoring agent"}
  ],
  "availableModels": [
    {
      "id": "llama3.2:3b",
      "name": "Llama 3.2 3B",
      "provider": "Ollama",
      "available": true,
      "contextWindowTokens": 8192,
      "recommended": false
    }
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

- `capabilities[]` and `availableModels[]` are canonical arrays. The 1.0 EVO field name
  `models` is accepted as a deprecated alias and may be dropped in 2.0.

### 4.3 Create Execution — `POST /teip/v1/executions`

Submits a new cognitive or development execution.

**Request headers:** `Content-Type: application/json`, `Authorization: Bearer <token>` (§6),
`Idempotency-Key: <key>` (when an idempotency key is supplied in the body).

**Request body:**

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "requestId": "req-987",
  "correlationId": "corr-876",
  "idempotencyKey": "idem-task-100-step-1",
  "timestamp": "2026-11-06T18:00:00Z",
  "caller": {
    "system": "TRYST",
    "projectId": "proj-tryst",
    "taskId": "task-100"
  },
  "capability": "CODE_IMPLEMENTATION",
  "mode": "DEFAULT",
  "model": "llama3.2:3b",
  "instruction": "Refactor UserService to use connection pooling.",
  "contextReferences": [
    {"type": "FILE", "path": "src/main/java/eu/kalafatic/tryst/UserService.java", "description": "Target interface"}
  ],
  "grantedToolScopes": ["read:workspace", "read:task", "write:workspace"],
  "parameters": {
    "maxTokens": 4096,
    "temperature": 0.2
  }
}
```

- `mode` is canonical (`aiMode` accepted as deprecated alias); `grantedToolScopes` is canonical
  (`mcpToolScopes` accepted as deprecated alias).
- `model` at top level is canonical; `parameters.model` remains accepted as fallback.
- `contextReferences` is an array of objects; bare strings are accepted as path shorthand.
- Request size limit: 5 MB.

**Accepted response (HTTP 202 Accepted — new execution):**

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "executionId": "exec-9a1b2c3d",
  "evoSessionId": "evo-teip-exec-9a1b2c3d",
  "status": "QUEUED",
  "correlationId": "corr-876",
  "createdAt": "2026-11-06T18:00:01Z",
  "links": {
    "status": "/teip/v1/executions/exec-9a1b2c3d",
    "events": "/teip/v1/executions/exec-9a1b2c3d/events",
    "result": "/teip/v1/executions/exec-9a1b2c3d/result",
    "cancel": "/teip/v1/executions/exec-9a1b2c3d/cancel"
  }
}
```

**Duplicate response (HTTP 200 OK — same idempotency key):** the server returns the same
executionId and the status of the existing execution; no duplicate process is created.
`evoSessionId` is optional and implementation-specific.

### 4.4 Get Execution Status — `GET /teip/v1/executions/{executionId}`

**Response (200 OK):**

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "executionId": "exec-9a1b2c3d",
  "status": "RUNNING",
  "correlationId": "corr-876",
  "phase": "ANALYZING_AST",
  "progress": 0.45,
  "statusMessage": "Evaluating candidate variant 2",
  "createdAt": "2026-11-06T18:00:01Z",
  "startedAt": "2026-11-06T18:00:02Z",
  "updatedAt": "2026-11-06T18:00:05Z",
  "lastSequence": 4,
  "details": {}
}
```

External execution states: `QUEUED` | `RUNNING` | `WAITING` | `COMPLETED` | `FAILED` |
`CANCELLED`. `phase`, `statusMessage`, `updatedAt`, `lastSequence`, `details` are optional;
`status` and `progress` are required.

### 4.5 Execution Events — `GET /teip/v1/executions/{executionId}/events?afterSequence={n}&limit={m}`

Returns events in strict sequence order for durable replay. `limit` defaults to 50 and MUST NOT
exceed 500.

**Response (200 OK):**

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "executionId": "exec-9a1b2c3d",
  "events": [
    {
      "eventId": "evt-1",
      "executionId": "exec-9a1b2c3d",
      "sequence": 1,
      "type": "EXECUTION_STARTED",
      "timestamp": "2026-11-06T18:00:02Z",
      "correlationId": "corr-876",
      "data": {"mode": "DEFAULT"}
    }
  ],
  "hasMore": false,
  "nextSequence": 2
}
```

- **Cursor semantics:** `nextSequence` = last returned event's sequence + 1 (0 when no events
  exist). Clients use it verbatim as the next `afterSequence`. `latestSequence` is a deprecated
  alias.
- Event types: `EXECUTION_QUEUED`, `EXECUTION_STARTED`, `PROGRESS_UPDATED`, `EXECUTION_LOG`,
  `TOOL_INVOKED`, `EXECUTION_COMPLETED`, `EXECUTION_FAILED`, `EXECUTION_CANCELLED`.
- Sequences MUST survive server restarts for in-flight executions.

### 4.6 Get Execution Result — `GET /teip/v1/executions/{executionId}/result`

**Response (200 OK, terminal executions):**

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "executionId": "exec-9a1b2c3d",
  "correlationId": "corr-876",
  "status": "COMPLETED",
  "summary": "Completed UserService connection pool refactoring.",
  "patch": "diff --git a/src/main/java/eu/kalafatic/tryst/UserService.java ...",
  "modifiedFiles": ["src/main/java/eu/kalafatic/tryst/UserService.java"],
  "artifacts": [
    {"name": "AuthFilter.java", "type": "SOURCE_PATCH", "content": "..."}
  ],
  "metrics": {
    "durationMs": 4250,
    "iterations": 2,
    "promptTokens": 1280,
    "completionTokens": 450,
    "toolCallsCount": 3,
    "fitnessScore": 0.94
  },
  "completedAt": "2026-11-06T18:00:10Z"
}
```

For non-terminal executions the server returns **409 CONFLICT** with a structured error
(`CONFLICT`, retryable, `details.currentStatus`).

### 4.7 Cancel Execution — `POST /teip/v1/executions/{executionId}/cancel`

Cancellation is idempotent: cancelling an already terminal execution succeeds without error.

**Response (200 OK):**

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "executionId": "exec-9a1b2c3d",
  "status": "CANCELLED",
  "message": "Execution cancellation accepted."
}
```

Unknown execution ids return 404 `NOT_FOUND`.

## 5. Error Model

All error responses use the top-level envelope:

```json
{
  "protocol": "TEIP",
  "version": "1.1",
  "code": "VALIDATION_FAILED",
  "message": "Missing required field: instruction",
  "requestId": "req-1003",
  "correlationId": "corr-876",
  "retryable": false,
  "timestamp": "2026-11-06T18:00:00Z",
  "details": {"field": "instruction"}
}
```

Canonical codes:

| Code | HTTP | Retryable |
|---|---|---|
| `AUTHENTICATION_FAILED` | 401 | no |
| `AUTHORIZATION_DENIED` | 403 | no |
| `VALIDATION_FAILED` | 400 | no |
| `NOT_FOUND` | 404 | no |
| `CONFLICT` | 409 | no |
| `CAPABILITY_UNSUPPORTED` | 422 | no |
| `INTERNAL_ERROR` | 500 | no |
| `SERVICE_UNAVAILABLE` | 503 | yes |
| `TIMEOUT` | 504 | yes |
| `VERSION_MISMATCH` | 400 | no |
| `METHOD_NOT_ALLOWED` | 405 | no |

Clients MUST parse the error body on non-2xx responses and MUST retry (bounded, exponential
backoff) on retryable codes and transport failures.

## 6. Security and Authentication

1. `Authorization: Bearer <token>` is REQUIRED on every TEIP endpoint. Servers MUST enforce it.
   Development-mode opt-out (e.g., loopback-only without a token) MUST be explicit
   configuration, never a silent default.
2. Tools can only be invoked if present in the granted scopes (§7).
3. Secret credentials, absolute filesystem paths outside designated sandboxes, and unrestricted
   shell execution are strictly disallowed.
4. Diagnostic logs and event streams MUST redact tokens, credentials, and sensitive content.
5. Non-negotiable boundary rules (unchanged from 1.0):
   - **No Direct Object Sharing** — products are independent runtimes; neither may load classes
     or manipulate internal state of the other.
   - **Task Lifecycle Sovereignty** — the workflow platform (TRYST) is the sole authority for
     task state, workspace modifications, test execution, review approval and Git push. A
     completed remote execution never marks a local task complete.
   - **Human-in-the-Loop Commit Gating** — AI and remote engines are forbidden from approving
     reviews, creating commits or pushing to remotes.
   - **Sandbox Isolation** — remote engines never receive raw host filesystem paths or
     long-lived Git credentials; all file operations occur via scoped, permission-validated
     MCP tools.

## 7. Idempotency and Recovery

1. Execution requests with the same non-empty `idempotencyKey` MUST return the existing accepted
   execution without creating a duplicate process.
2. The relation between `idempotencyKey` and `executionId` MUST be persisted and MUST survive
   server restarts.
3. Cancellation is idempotent.
4. After an unexpected restart, executions that cannot be resumed cleanly transition to
   `FAILED` or `CANCELLED` with explicit diagnostic messages.
5. Sequence cursors prevent event loss across network interruptions or restarts.

## 8. Model Context Protocol (MCP) Over HTTP (Tool Plane)

TRYST exposes a scoped JSON-RPC 2.0 MCP tool plane under `/api/mcp` for remote engines (e.g.,
the EVO engine executing a delegated task).

1. **Transport:** HTTP POST, `Content-Type: application/json`.
2. **Headers (REQUIRED and ENFORCED):**
   - `Authorization: Bearer <tryst-mcp-token>`
   - `X-Execution-Id: <executionId>`
   - `X-Correlation-Id: <correlationId>` (optional)
3. **Methods:** `initialize` (MCP protocol version `2024-11-05`) — REQUIRED; `tools/list`;
   `tools/call`.
4. **Scopes:** `read:workspace`, `read:task`, `write:workspace`, `execute:build`. The tool plane
   derives granted scopes from the creating execution's record; invocations outside the granted
   scopes are refused with JSON-RPC error `-32001`. Clients MUST refuse out-of-scope calls
   before transmission (fail-closed).
5. **Canonical tool set:**

| Tool | Required scope | Purpose |
|---|---|---|
| `get_project_metadata` | `read:task` | project configuration and branch info |
| `list_workspace_files` | `read:workspace` | list sandbox directory contents |
| `read_workspace_file` | `read:workspace` | read a file inside the sandbox |
| `search_workspace_code` | `read:workspace` | text search inside the sandbox |
| `get_task_context` | `read:task` | task title/description/intake instructions |
| `propose_patch` | `write:workspace` | apply a unified diff to the sandbox |
| `run_build_and_test` | `execute:build` | run build/test verification in the sandbox |

6. Path sanitization: absolute paths, `..` segments, symlink escapes and null-byte injections
   are strictly rejected.

## 9. Default Endpoint

The canonical default EVO endpoint for TEIP clients is `http://localhost:28080` (EVO primary
HTTP port). Products MUST NOT ship defaults pointing to undocumented ports.

## 10. Compatibility Policy

- Within major version 1: additive changes only; deprecated aliases retained for at least two
  minor releases; open-world receivers.
- 2.0 candidates (deferred, not part of 1.1): family task-lifecycle messages, SSE/WebSocket
  push channels, DStudio/DCode participant roles, capability negotiation handshake.
- Every change ships with conformance wire-test updates in the same change.
