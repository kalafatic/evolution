# ENR AND DCODE VERIFICATION

## 1. ENR (eu.kalafatic.evolution.enr)

### Contract (wire tests, EnrHttpServerTest 12/12 PASS + EnrRuntimeTest 9/9 PASS)

- `GET /enr/v1/models` serializes `contextLength` + `contextLengthKnown`
  per model (new wire assertions PASS).
- Ollama models advertise the conservative default (4096,
  `contextLengthKnown=false`); native/llama-cpp artifacts report 0/false —
  consumers never mistake estimates for verified limits.
- `POST /enr/v1/chat` returns `promptTokens` and `completionTokens`
  (wire assertion PASS). Counters come from Ollama's `prompt_eval_count` /
  `eval_count` via `OllamaService.chatWithUsage`; 0 means "unknown", never
  "zero consumed".
- Backward compatibility: previous record fields unchanged; existing
  EnrProvider-style consumers compile and behave identically.
- ENR remains stateless: no new persistence added (source inspection).

### Test fix

EnrHttpServerTest's FakeEngine used the pre-extension record constructors
(NoSuchMethodError). Updated to the extended contract — the fix was in test
code, not production code.

## 2. DCode (DCoder-121126)

### Endpoint `POST /api/v1/sessions/{id}/context`

- Implemented in `DCodeApi.ingestPlatformContext`: validates contextId +
  sections, persists as a SYSTEM message with
  `{"source":"EVO_COGNITIVE_SERVICES",...}` envelope, returns messageId +
  tokenEstimate.
- Ingested context does not overwrite user instructions: it is a separate
  SYSTEM message; the loop renders it as a dedicated "Platform context
  (EVO)" section and excludes it from the history window.

### metadata_json round-trip fix

- `SqliteSessionService.mapSession` now parses `metadata_json`
  (previously dropped). Verified by `PlatformContextIngestPersistenceTest`
  (2/2 PASS): metadata survives restart; ingested platform context SYSTEM
  message survives restart.

### Loop integration

- `ToolCallingAgentLoop` extracts platform-context messages, renders the
  sections, and builds history without them.

### Verification

- DCode reactor test suite (with persistence/server/harness in the build):
  **128 tests, 0 failures, 0 errors, 3 skipped** (live-LLM tests).
- No competing long-term memory implementation introduced (inspection).

## 3. Honesty note

Token usage is reported as the backend provides it; when the backend cannot
report (native path), the fields are 0 with the documented "unknown"
semantics. No precision is claimed.
