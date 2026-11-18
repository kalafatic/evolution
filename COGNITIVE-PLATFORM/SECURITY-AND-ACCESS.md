# SECURITY AND ACCESS
Cognitive Services security model as implemented

## 1. Authentication

- **MCP (28280)**: `Authorization: Bearer <token>` required for all cognitive
  tools/resources. `BearerTokenAuthenticator` accepts two classes:
  1. Application tokens (`IdentityService`): SHA-256 hashed, bound to
     (application, user, scopes), revocable, optional expiry. Plaintext shown
     exactly once at issuance.
  2. EVO user session tokens (`AuthService`): scopes derived from the user role
     (ADMIN gets write + `*.any` + ecosystem.admin).
- Legacy demo tools on 28280 keep their existing behavior; protected
  (cognitive) tools are hidden from `tools/list` for unauthenticated callers
  and reject calls without a valid token.
- Internal SWT/loopback paths are unchanged (SYSTEM bypass for non-cognitive
  paths only).

## 2. Authorization

- Dispatcher level: existing `AuthorizationService.authorize(principal,
  "MCP_TOOL", toolName, "EXECUTE")` for every call, plus per-tool scope
  requirements (`registerToolScope`): context.get, context.search,
  context.record, memory.read/write, user.read, user.preferences.read/write,
  session.create/read/write, decision.read/write, project.read/write.
- Prompts: optional authz gate (`enforcePromptAuthorization` enabled on 28280).
- Scope semantics: `ecosystem.admin` implies all scopes; cross-owner access
  requires `identity.read.any`/`session.read.any`/`memory.read.any`/
  `project.read.any` (admin-only by default).

## 3. Identity parameter validation (params never carry identity)

- `userId` in tool arguments is honored only when it equals the token
  principal's username (or the caller holds ecosystem.admin); otherwise the
  principal is used. Verified by `ContextReconstructionTest.identityParamsDoNotOverridePrincipal`.
- `applicationId` always resolves to the token's application.
- Memory writes are tagged with the principal's user/application — the caller
  cannot tag content as belonging to another user.

## 4. Isolation rules (service layer, not only MCP)

| Scope | Rule |
|---|---|
| USER | owner user only (+ `*.any`) |
| APPLICATION | sessions/memory tagged with the app id are visible to that app's token |
| PROJECT | project-ref equality in queries |
| SESSION | session owner (user+app); related sessions need `session.read.any` |
| ECOSYSTEM | authenticated apps read public knowledge |

## 5. Auditing

- Every MCP tool call: actor, tool, outcome (SUCCESS/DENIED/ERROR) → `audit_log`.
- Token issuance/revocation, application registration/update/deletion → `audit_log`.
- Memory mutations: MemoryVersion JSONL records (content-level lineage) +
  index validity transitions; retraction never hard-deletes.
- `context_get` requests are logged without returned content; context content
  is stored only via explicit `context_record`.

## 6. Security controls that can NEVER be disabled

Enforced unconditionally (independent of the validation policy):
authentication, authorization, application identity verification, user data
access control, application data isolation, project/session access control,
input safety, SQL injection protection (prepared statements everywhere),
unauthorized write protection, database integrity (WAL + transactions),
resource/input limits (budgets, truncation), cross-application session
protection, auditing of security-sensitive operations.
The validation UI marks these rules SECURITY FIXED and refuses changes.

## 7. Token handling

- Tokens stored as SHA-256 hashes only; plaintext returned exactly once.
- No tokens or secrets are logged anywhere; memory validators reject
  token/password-shaped content from entering memory as plain secrets
  (sensitive flag support reserved).
- Revocation takes effect immediately (validated per call; no token cache).

## 8. Verified test matrix (see TEST-RESULTS.md)

Invalid token, revoked token, unknown application, invalid scopes, missing
scope (-32002), missing token (-32001), hidden tools for anonymous callers,
spoofed userId rejection, security-fixed validation rules.
