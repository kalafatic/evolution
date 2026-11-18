# SECURITY VERIFICATION
Runtime + test evidence for the security model.

## 1. Authentication (live)

| Check | Result |
|---|---|
| Missing token → -32001 | PASS (live) |
| Invalid token → -32001 | PASS (live) |
| Revoked token → -32001 | PASS (live) |
| Valid token executes | PASS (live) |
| Token values in logs | never (grep-verified) |

## 2. Authorization and scopes (live)

| Check | Result |
|---|---|
| decision_record without decision.write → -32002 | PASS |
| user_preferences_update without user.preferences.write → -32002 | PASS |
| user_preferences_update without user.preferences.read → -32002 | PASS |
| Anonymous tools/list hides protected tools | PASS (5 legacy tools only) |
| Authenticated tools/list exposes granted tools | PASS (29 tools) |

## 3. Identity-parameter validation (live)

- context_get with spoofed userId returns the token principal's identity. PASS
- Memory writes are tagged with the token's user/application (params cannot
  override). PASS (unit + live inspection)

## 4. Isolation (live E2E)

- Application isolation: DStudio token cannot read TRYST sessions or
  conversations (-32002). PASS
- Session isolation: session-scoped memory only for the owning session. PASS
- Project isolation: cross-project knowledge excluded. PASS (unit)
- User isolation: USER-scope entries of other users excluded. PASS (unit +
  live spoof test)

## 5. Validation policy security invariants

- Security rules (authentication, authorization, application identity,
  user data access, app isolation) are SECURITY FIXED: effective mode
  always STRICT, cannot be disabled (unit test; UI refuses changes).
- Default remains Strict Validation = DISABLED (runtime DB verified:
  global.strict DISABLED).

## 6. Auditing (live)

- Every MCP tool call audited (SUCCESS/DENIED/PERMISSION_DENIED/
  INVALID_ARGUMENTS/ERROR/NOT_FOUND) — 393 rows in audit_log.
- Token issuance/revocation audited (IdentityService).
- Memory mutations append version records (content-level lineage).

## 7. Tokens

- SHA-256 hashes only in storage (fixture seeding used pre-hashed values;
  issuance API shows plaintext once).
- No plaintext tokens in logs, DB dumps, or documentation.

## 8. Tests

- CognitiveSecurityTest 8/8, CognitiveMcpSecurityTest 6/6,
  ContextReconstructionTest isolation cases, DarwinCognitiveIntegrationTest,
  live matrix + E2E negatives — all PASS.
