# TEIP 1.0 — Test Coverage & Verification Matrix

## 1. Automated Test Suite

Automated verification is implemented in `eu.kalafatic.evolution.controller.tests.teip`:

1. **`TeipDtoSerializationTest`**: Verifies bidirectional JSON serialization and validation of all TEIP requests, responses, and events.
2. **`TeipExecutionManagerTest`**: Verifies execution creation, idempotency, status lifecycle transitions, cancellation, and event sequencing.
3. **`TrystMcpClientTest`**: Verifies dynamic tool discovery and fail-closed scoped permission checks.
4. **`TeipApiEndpointTest`**: End-to-end integration test of HTTP endpoints (`/teip/v1/health`, `/capabilities`, `/executions`, `/events`, `/result`, `/cancel`).

## 2. Running Verification

Execute via Maven Tycho:
```bash
./mvnw test-compile
```
And run specific test classes as needed.
