# TEIP 1.1 — Test Coverage & Verification Matrix (EVO side)

> **Status note:** describes the **1.1** test suite (WP-3.3/WP-3.5, Phase 3). Canonical
> specification: `docs/protocol/TEIP-1.1.md`.

## 1. Automated Test Suite

Automated verification is implemented in `eu.kalafatic.evolution.controller.tests.teip`:

1. **`TeipDtoSerializationTest`**: Verifies bidirectional JSON serialization and validation of
   all TEIP 1.1 requests, responses and events; canonical `version: "1.1"`; canonical
   `capabilities[]`/`availableModels[]` arrays; `patch`/`modifiedFiles` in results; deprecated
   alias acceptance (`aiMode`, `mcpToolScopes`, `caller.app`, bare-string contextReferences);
   `VERSION_MISMATCH` for unsupported major versions.
2. **`TeipExecutionManagerTest`**: Verifies execution creation, duplicate detection
   (`CreateOutcome.duplicate()`), status lifecycle transitions, cancellation idempotency,
   `nextSequence` cursor semantics (last returned sequence + 1), and persisted idempotency
   mapping.
3. **`TrystMcpClientTest`**: Verifies canonical scope mapping (`read:workspace`,
   `write:workspace`, `execute:build`, `read:task`), fail-closed scope rejection before
   transmission, required `Authorization`/`X-Execution-Id`/`X-Correlation-Id` headers, and the
   MCP `initialize` handshake (protocol version `2024-11-05`).
4. **`TeipApiEndpointTest`**: End-to-end integration test of HTTP endpoints
   (`/teip/v1/health`, `/capabilities`, `/executions`, `/events`, `/result`, `/cancel`) against
   a live `EvolutionServer`; verifies 202/200 semantics, idempotent cancel, 409
   `details.currentStatus`, mandatory authentication (401 without token, explicit
   `evo.teip.devMode=true` loopback opt-out, bearer enforcement when configured).
5. **`TeipProtocolUiIntegrationTest`**: In-process UI bridge and session/operation correlation
   tests.

## 2. Running Verification

Execute via Maven Tycho (JDK 21, Maven 3.9+):

```bash
./mvnw -o integration-test -pl eu.kalafatic.evolution.controller.tests \
  -Dtest='TeipDtoSerializationTest,TeipExecutionManagerTest,TrystMcpClientTest,TeipApiEndpointTest,TeipProtocolUiIntegrationTest'
```

## 3. Cross-Repository Conformance (WP-3.5)

The TRYST-side suite lives in the TRYST repository (`tests/eu.kalafatic.tryst.tests`):
`TeipClientTest` (mock EVO server; 202/200 acceptance, `Idempotency-Key` header, `nextSequence`
cursor + `latestSequence` alias, canonical error parsing), `TeipProtocolContractTest`
(canonical request serialization), `McpServerSecurityTest` (MCP initialize lifecycle,
scoping, path traversal), `EvoPersistenceAndRecoveryTest`, `TeipLifecycleIntegrationTest` and
`SchemaMigrationTest` (V16 `granted_scopes`).

Live delegated execution end-to-end:
1. Start EVO with a configured shared token (`evo.teip.auth.token`) on port 28080.
2. In TRYST, configure the EVO endpoint (`http://localhost:28080`) and the same token via the
   AI Integration Center.
3. Create an execution through TRYST (`/api/integration/evo/executions`) and observe the
   canonical 202 accepted response, persisted execution record, event replay via
   `nextSequence`, and the TRYST MCP tool plane authorizing EVO's `X-Execution-Id` calls
   against `evo_executions.granted_scopes`.
