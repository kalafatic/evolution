# DCode AND ENR INTEGRATION

How the harness (DCode) consumes reconstructed context and how the inference runtime (ENR) contributes metadata without owning memory. Boundaries preserved: EVO owns cognitive infrastructure; MCP exposes it; DCode consumes it; Darwin orchestrates with it; ENR provides inference.

## 1. DCode consumption model

DCode stays an execution harness (FHC 1.0 caretaker). It gets context in two additive ways:

### 1.1 Context ingest endpoint (DCode change, additive)

- New `POST /api/v1/sessions/{id}/context` (`DCodeApi`/`DCodeHttpServer`): body `{contextId, schemaVersion, sectionsJson, tokenEstimate}`. Persisted to the `messages` table as a SYSTEM message with `metadata.contextId` (no new table) OR into a new `session_context` table — smallest change: SYSTEM message + `Session.metadata.contextId` update.
- `ToolCallingAgentLoop.run` consumes it: before building `[system, user]`, if the session has an ingested context, the loop injects `system("Platform context (EVO):\n" + sections)` and reduces `historySummary` window proportionally to `tokenEstimate` (budget-aware: window = f(contextWindowTokens, tokenEstimate)).
- Fix verified defect: `SqliteSessionService.mapSession` must round-trip `metadata_json` (currently dropped, line 227) so `Session.metadata.contextId` survives restart.

### 1.2 External context via MCP client (optional, DCode-side)

- Minimal JSON-RPC-over-HTTP client (mirrors EVO `McpClient`/TRYST `TeipClient` style) calling `context_get`/`memory_search`; used when DCode runs without a delegating parent (standalone headless). Config via existing config bundle (`DCodeConfiguration` style) with `evoMcpEndpoint` + `evoMcpToken`. No hardcoded EVO→DCode dependency; DCode keeps its provider SPI.

## 2. ENR metadata extension (ENR change, additive, statelessness preserved)

| Change | Where | Detail |
|---|---|---|
| Token usage in responses | `EnrChatResponse` + `EnrHttpServer.chat`/stream | add `promptTokens`, `completionTokens`; source: Ollama `prompt_eval_count`/`eval_count` (already returned by `OllamaService` layer), 0 when engine cannot report (native path estimates later) |
| Context window metadata | `EnrModel` + `/enr/v1/models` | add `contextLength` (tokens): Ollama model details; GGUF/`.evo` artifact metadata via existing `LlamaService` model files; default 4096 when unknown, with `contextLengthKnown: false` flag |
| Embeddings (Phase 2) | new `POST /enr/v1/embeddings` + capability `ENR_EMBEDDINGS` | delegates to Ollama `/api/embeddings` when the served model supports it; returns vectors; ENR remains stateless (no vector storage) |
| Structured output (Phase 2) | chat request field `responseFormat` passthrough | advertised via capability flag only when the backing engine supports it |

Impact: DCode `EnrProvider` (reads `promptTokens`/`contextLength` defensively today and gets 0) starts receiving real values with zero DCode code change for the usage path; `ModelDefinition.contextWindowTokens` becomes meaningful → the loop can enforce budgets.

## 3. Budget chain (end-to-end)

```text
EVO context_get → tokenEstimate (TokenEstimationService)
        │ (contextId, tokenEstimate, sections)
        ▼
DCode POST /sessions/{id}/context → stored; loop computes available = contextWindowTokens − tokenEstimate
        │ history window trimmed accordingly; maxTokens for generation set from remaining budget
        ▼
ENR /enr/v1/chat → returns promptTokens/completionTokens → DCode ChatResponse round-trip
        │ (optional) reported back to EVO context_record for per-project token accounting
        ▼
EVO consolidation: token stats stored in ImplementationHistory metadata
```

## 4. Darwin participation

- When EVO itself runs ASSISTED_CODING/DEVELOPER through the DCode adapter (`controller` `DCodeClient`/`DCodeCodingHarnessAdapter`, existing), `KernelFactory`/`DarwinFlow` path gains one additive step: before delegation, `CognitiveContextService.getContext(...)` → sections pushed to DCode via the new ingest endpoint. Darwin stays the orchestrator; DCode stays the executor.
- ENR engagement unchanged: `LlmRouter` slot (`EnrLlmProvider`, existing) — Darwin never talks to ENR for memory; memory always goes through the native services.

## 5. What must NOT happen

- DCode must not import EVO classes (wire-only: FHC 1.0 REST + MCP).
- ENR must not store memory/context/history (spec §8 preserved; all new fields are per-request/per-model metadata).
- No new provider abstraction in DCode for context (the ingest endpoint is transport-neutral JSON).

## 6. Acceptance criteria

1. `POST /api/v1/sessions/{id}/context` round-trips across DCode restart (metadata fix verified by persistence test).
2. `GET /api/v1/models` shows non-zero `contextWindowTokens` when served via ENR+Ollama.
3. `/enr/v1/chat` returns promptTokens/completionTokens matching Ollama counters.
4. DCode loop with ingested context + window reduction produces a valid run (existing loop tests extended).
5. ENR has zero new persistent state (grep: no new file/DB writes).
