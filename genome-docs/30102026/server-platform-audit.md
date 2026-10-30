# EVO Server Platform & Asynchronous Execution Audit

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/server-platform-audit.md`
**Scope:** EVO Server, Asynchronous Execution Model, REST Endpoints, MCP Integration, Authorization & Persistence

---

## 1. Overview & Core Purpose

`EvolutionServer` (built on NanoHTTPD) serves as the central server exposing EVO capabilities asynchronously to external clients, Eclipse RCP views, and MCP integrations. It provides REST endpoints for authentication, administration, project management, task execution, and MCP tool dispatching.

---

## 2. REST API Endpoint Matrix

| Method | Endpoint | Responsible Controller / Service | Purpose |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | `AuthController` / `AuthService` | User authentication & session token issuance |
| `GET` | `/server/status` | `EvolutionServer` | Server status and health ping |
| `GET` | `/server/osgi` | `EvolutionServer` | OSGi bundle resolution status check |
| `POST` | `/server/project/create` | `EvolutionServer` | Creation of `EvoProject` & session binding |
| `POST` | `/task` | `OrchestratorServiceImpl` | Asynchronous inference / task submission |
| `GET` | `/server/conversation/:id` | `ConversationOutputController` | Polling conversation logs & task execution state |
| `GET` | `/api/admin/*` | `AdminApiController` | System administration, users, sessions, RBAC |
| `POST` | `/mcp` | `McpServer` / `JsonRpcDispatcher` | Model Context Protocol JSON-RPC tool calls |

---

## 3. Asynchronous Execution Model & Durable States

To prevent HTTP connection timeouts during long-running inference, coding, or self-development operations:
1. **Request Acceptance**: When a request is posted to `/task` or triggered via the server, `OrchestratorServiceImpl` immediately generates a stable `taskId` and `sessionId`, stores the operation state in memory/database, and enqueues execution into an `ExecutorService`.
2. **Immediate Response**: The HTTP client receives an immediate response with `status: "ACCEPTED"` and the associated `sessionId` / `taskId`.
3. **Durable States**: Long-running operations transition through explicit states:
   - `CREATED` -> `ACCEPTED` -> `RUNNING` -> `WAITING_FOR_INPUT` -> `COMPLETED` / `FAILED` / `CANCELLED`.
4. **Disconnection Recovery**: If an HTTP client disconnects, the background thread continues execution, persisting progress and output to `SessionLogger` (`session_<sessionId>.log`). The client can reconnect at any time and poll status via `/server/conversation/<sessionId>`.

---

## 5. Model Context Protocol (MCP) Integration

- **Dispatcher**: `McpServer` uses `JsonRpcDispatcher` to parse incoming JSON-RPC requests, routing them to registered `ITool`, `Resource`, or `Prompt` implementations in `ToolRegistry`.
- **Tool Delegation**: MCP tools delegate directly to established EVO controller tools (`GitTool`, `MavenTool`, `ShellTool`), avoiding parallel process management frameworks.

---

## 6. Authorization & Role-Based Access Control (RBAC)

- `AuthorizationService` enforces RBAC across system resources using SQLite database persistence (`users`, `roles`, `permissions`, `resource_access`).
- **Conflict Resolution**: Explicit `DENY` policies in `ResourceAccessPolicy` override role-based `ALLOW` grants.
- **System Principal**: Background process executions execute under dedicated `SYSTEM` credentials.
