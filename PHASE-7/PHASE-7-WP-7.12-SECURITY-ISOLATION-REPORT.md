# PHASE-7-WP-7.12-SECURITY-ISOLATION-REPORT.md

Phase 7 — WP-7.12 Security and Isolation Acceptance
Date: 2026-11-17
Status: COMPLETE

## 1. Isolation guarantees (verified)

| Concern | DCode | TRYST | DStudio | EVO | ENR |
|---|---|---|---|---|---|
| Session isolation | per-session workspace + SQLite scoping (suite) | per-task sandbox `sandboxes/{ownerId}/{taskId}` (suite + live) | execution idempotency keys + per-execution events (suite) | `SessionContainer` per session, `SessionManager` sole authority | per-request KV cache, session admission limit, isolation test green |
| Repository isolation | `~/.dcode/workspaces/{id}` | mirror pool + per-task clones; canonical+lexical containment | project dirs under `~/.dstudio` | Darwin session workspace | never touches repositories |
| Workspace isolation | `ToolEngine.resolveContained` (traversal/symlink rejection) | `PermissionManager.validatePathWithinWorkspace` + per-path locks | execution scoping via project | `GitManager` in-session | n/a |
| Credential handling | AES-GCM `secrets.enc`, POSIX-only key, masked listings, `LogSanitizer` (tests green) | DCode token never persisted (V20 + adapter); env sanitization strips `token/secret/password/auth/key/credential` | config without secrets in responses | network-access configuration tests | no credentials stored |
| Configuration protection | `server.authToken` secret | bearer auth for integration endpoints | TEIP auth token | port/network config EMF | optional bearer token (`evo.enr.auth.token` convention) |
| API authentication | Bearer optional in dev mode; enforced when configured (test green) | Bearer + `Permission.MANAGE_AGENTS`, 401/403, dev-mode loopback fallback (suite green) | TEIP 1.1 auth mandatory — **live-verified** `AUTHENTICATION_FAILED` envelope with clear remediation | TEIP auth + MCP auth | loopback bind; bearer when configured |
| Authorization | review approval required before any commit path | role-based model visibility, commit gated by approved fingerprint | review/approval owns commit decisions | existing permission model | stateless per request |
| Inter-service communication | loopback HTTP (127.0.0.1) for all family ports | loopback | loopback | loopback | loopback (127.0.0.1 default) |
| Tool execution boundaries | containment + RUNNING-session gate for mutating tools | process sandbox: cwd containment, env sanitization, process-group kill on cancel/timeout | CLI adapters via `ProcessExecutionSupport` | MCP security tests green | in-process kernels only (no shell) |
| Runtime process isolation | n/a (in-process JVM) | sandbox provider SPI with built-in default | n/a | n/a | native FFM calls, no subprocess inference |
| Log sanitization | `LogSanitizer` (secrets never logged; tests) | token masking (`****** (configured)`) | masked configs | existing | no secrets logged |

## 2. Cross-application access control

- An application cannot access another application's private task/repository state:
  separate home dirs (`~/.dcode`, `~/.tryst`, `~/.dstudio`, EVO data dir); SQLite
  files are per-product; APIs are loopback with product-local auth; no shared file
  handles or shared registries (WP-7.1).
- Live Phase 7 runs confirmed TRYST tasks operated exclusively inside the TRYST
  sandbox (DCode sessions created by TRYST were pointed at the TRYST workspace; no
  other product state was touched).

## 3. Special attention: DCode tool execution and ENR model resources

- DCode executes development tools under workspace containment with an audit
  `ToolCall` trail and cancellation relay — suite-tested (incl. path traversal
  rejection).
- ENR manages inference processes and model resources: standalone ENR performs
  inference in-process with its own compute backends (no shell, no subprocess);
  model files validated (magic, architecture, checksum/NaN scan) before loading;
  CUDA VRAM accounting bounds device memory.

## 4. Defects

None new. Notes: family services rely on the documented loopback + optional bearer
token convention (TEIP 1.1 §6); production hardening to explicit TLS/auth is a
deployment concern outside this phase's scope.
