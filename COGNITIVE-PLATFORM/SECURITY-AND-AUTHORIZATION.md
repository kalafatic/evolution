# SECURITY AND AUTHORIZATION

Hardening the existing auth stack for multi-application cognitive access. Reuses: `AuthService`, `AuthorizationService`, `Role`/`Permission`/`ResourceAccessPolicy`, `AuditLogEntry`, `BCryptUtils`, SQLite `data/evolution.db`.

## 1. Threat model (top of list)

1. External MCP client spoofing `userId`/`projectId` in tool params.
2. Unauthenticated MCP access via unverified `x-evo-user` header (current demo behavior).
3. Cross-application data bleed (memory/session of one app visible to another).
4. Untrusted clients injecting content into memory that later enters prompts (prompt injection via memory).
5. Lost/leaked tokens; unrevoked app access.

## 2. Authentication

### Internal (SWT runtime / loopback)
- Unchanged: `ServerSettings.isAuthenticate()`, SWT bypass, session cookie/bearer validation (`AuthController`).

### External MCP clients
- **Bearer token required** for all cognitive tools/resources. Two token classes:
  - User session token (existing `AuthService` login session) — represents a user; scopes derived from role + resource_access.
  - Application token (new `applications`/`application_tokens` tables, issued by admin API): binds (applicationId, userId, grantedScopes[], expiry). SHA-256 hash stored (same pattern as TRYST `user_sessions.token_hash`); plaintext shown once.
- `IdentityService.validateToken(token)` returns `Principal{userId, applicationId, scopes}` or rejects.
- MCP dispatch: `JsonRpcDispatcher` gains a `setAuthenticator(...)` hook (mirrors existing `setAuthorizationService`); on 28280 both authenticator and authorization service are installed. The `x-evo-user` header is ignored for cognitive tools (never trusted as identity).

## 3. Authorization model

- Principle: **params never carry identity**. `userId`, `applicationId`, `projectId` in tool arguments are validated against the Principal: `userId` must equal principal (or scope `identity.read.any`), `applicationId` must equal principal's app (or `ecosystem.admin`), `projectId` must be in the principal's accessible projects.
- Scopes (per (app,user) grant):

| Scope | Allows |
|---|---|
| `context.get` | `context_get`, `context_search` |
| `memory.read` / `memory.write` | memory_search/get vs memory_store/update/retract |
| `user.read` / `user.preferences.read` / `user.preferences.write` | user_get / preferences_get / preferences_update (own user) |
| `session.create` / `session.read` / `session.write` | session_create / session_get/search / snapshot, restore |
| `decision.read` / `decision.write` | decision_search/get / decision_record |
| `project.read` / `project.write` | project_context_get, project_knowledge_search / project_state_update |
| `session.read.any`, `memory.read.any`, `project.read.any` | cross-owner read (admin/OPERATOR only by default) |

- Enforcement path: MCP tools call `AuthorizationService.authorize(principal.username, "MCP_TOOL", toolName, "EXECUTE")` (existing) **plus** `CognitiveAuthorizer.checkScope(principal, "memory.write")`. Resource reads check the URI root: `evo://memory/*` → `memory.read`; `evo://projects/*` → `project.read`; `evo://users/{uid}/*` → own uid or `user.read.any`.
- Prompts: add authz gate (`("PROMPT", promptName, "READ")`) — currently unprotected (verified).

## 4. Isolation rules by scope

| Data scope | Visibility rule |
|---|---|
| USER | owner user only (+ `*.any` scopes) |
| APPLICATION | sessions/memory tagged with applicationId visible to that app's token only; EVO internal sees all |
| PROJECT | project path hash + explicit project grants; `project.read` requires membership or `project.read.any` |
| SESSION | session owner (user+app); `RELATED` sessions visible only with `session.read.any` |
| TASK | task-scoped entries inherit the session's owner |
| ECOSYSTEM | genome docs, architecture summaries, public decisions — read-all for authenticated apps |

Isolation is enforced at service level (UserContextService/ProjectKnowledgeService/MemoryService queries carry the Principal filter), not only at the MCP adapter — internal callers with no principal use the SYSTEM bypass (existing behavior).

## 5. Audit logging

- Reuse `AuthorizationService.logAudit` → `audit_log` (actor, action, resource, outcome, details) for: every MCP tool call (outcome + tool + principal), every memory mutation (store/update/retract + entryId + version), every decision record, every session snapshot/restore, every token issuance/revocation.
- Memory mutations additionally append `MemoryVersion` records (JSONL) — content-level audit beyond SQL.
- `context_get` requests are logged without the returned content (privacy); `context_record` snapshots are content-bearing and stored only when the caller explicitly requests.

## 6. Sensitive data handling

- Memory entries may be marked `sensitive: true` (metadata) → excluded from `context_get` unless scope `memory.read.sensitive`; never exported in genome exports.
- Secrets: no tokens/passwords stored in memory entries (validator rejects regex patterns); application tokens stored hashed; existing `password123`/`admin:admin` defaults flagged for mandatory first-run change (config-driven, non-breaking).
- Prompt-injection defense: consolidation and extraction results are stored with `source=CONSOLIDATION`/`INFERENCE` and are rendered in a dedicated section, so tool-output-sourced content cannot masquerade as user instructions (reuses the AttachmentInjector contradiction-suppression idea).

## 7. Memory deletion and correction

- `memory_retract` (validity=RETRACTED) is the user-facing deletion; hard delete only via admin API with audit.
- Retraction propagates: entries with `DERIVES_FROM` the retracted entry are flagged for re-verification (not auto-deleted).
- Correction path: `memory_update` bumps version and keeps full lineage (`provenanceChain`).

## 8. Concurrency, transactions, recovery

- SQLite WAL + `busy_timeout=30000` (existing `DatabaseManager` PRAGMAs) — single-writer semantics suffice for EVO scale; write serialization per service via synchronized blocks (existing MemoryService pattern).
- Transaction boundaries: `session_snapshot` writes (manifest row + file) wrapped in one transaction where possible; memory version append + index row update in one transaction; failure → rollback + error envelope (no partial manifests).
- Persistence failure policy: memory read failures degrade to empty context + warning (never block inference); memory write failures return error to caller + audit DENIED/ERROR (existing `AuthorizationService` outcome vocabulary extended).
- Recovery: DB file corruption → existing swallow-exception migration behavior documented as risk; JSONL stores are append-only and self-healing per line.

## 9. External app onboarding flow

1. Admin (or first-run wizard) creates ApplicationIdentity (`POST /api/admin/applications`) with scopes.
2. Token issued → app stores it (encrypted in its own vault; TRYST/DStudio/DCode already have credential vault patterns).
3. App calls MCP with Bearer; server validates principal + scopes per tool; all calls audited.
4. Revocation: `DELETE /api/admin/applications/{id}/token` → token hash row removed; active sessions unaffected (user tokens) — app tokens validated per call, no session cache.
