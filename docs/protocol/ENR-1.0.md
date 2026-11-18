# ENR 1.0 — Evolution Neural Runtime Contract

**Status:** NORMATIVE (Phase 6)
**Caretaker:** EVO (`eu.kalafatic.evolution.enr`)
**Related:** TEIP 1.1 (`docs/protocol/TEIP-1.1.md`), FHC 1.0 (canonical copy in the DCoder repository)

## 1. Purpose

ENR (Evolution Neural Runtime) is the family inference layer. It provides one stable
contract for LLM inference: model discovery, model lifecycle, generation, streaming,
provider management and runtime health. ENR does **not** own application sessions, coding
tasks, repository workflows or harness execution — those remain with the calling
application (EVO, TRYST, DStudio) or the coding harness (DCode).

ENR is a **default family component**, never an exclusive dependency: applications may use
alternative inference engines (Ollama directly, OpenAI-compatible remote endpoints, future
providers), and ENR itself unifies the existing EVO inference stack (Ollama, llama.cpp,
native EVO engine) without replacing it.

## 2. Architecture

```
Application -> DCode Harness -> ENR Inference Engine -> Selected Model
Application -> ENR Inference Engine (direct inference, no harness)
```

- ENR runtime: `eu.kalafatic.evolution.enr.EnrRuntime` (default implementation).
- Stable in-process contract: `eu.kalafatic.evolution.enr.EnrInferenceEngine` (published
  as an OSGi service by the ENR bundle activator).
- Wire contract: HTTP under `/enr/v1` (this document).
- Darwin bridge: `EnrLlmProvider` registers into the existing `LlmRouter` as the
  selectable ENR provider slot; direct Ollama/native paths remain unchanged.

## 3. Endpoints

Base: `http://localhost:28780` (family convention `PortRegistry.EVO_ENR`, overridable via
system property `evo.enr.port`). Bind address defaults to loopback.

### 3.1 Health and status

| Method | Path | Description |
|---|---|---|
| GET | `/health`, `/enr/v1/health` | `{application:"ENR", version:"1.0", status:"UP"\|"DEGRADED", ready, timestamp}` |

### 3.2 Models

| Method | Path | Description |
|---|---|---|
| GET | `/enr/v1/models` | Model discovery: `[{id, name, provider, engine, sizeBytes, available}]` |
| GET | `/enr/v1/models/{id}` | Single model; 404 `NOT_FOUND` when unknown |
| POST | `/enr/v1/models/{id}/load` | Load/warm model; `{model, success}` |
| POST | `/enr/v1/models/{id}/unload` | Unload model; `{model, success}` |
| DELETE | `/enr/v1/models/{id}` | Delete model (Ollama-registered models only; 409 `CONFLICT` otherwise) |
| POST | `/enr/v1/models/pull` | `{model}` — pull from provider registry (Ollama only); `{model, success}` |

### 3.3 Generation

| Method | Path | Description |
|---|---|---|
| POST | `/enr/v1/chat` | `{model, messages:[{role,content}], temperature?, maxTokens?}` → `{content, finishReason, engine, model, durationMs}` |
| POST | `/enr/v1/chat/stream` | Same request; `text/event-stream` SSE. Each event: `data: {"chunk": "...", "engine": "...", "done": false}`; terminal `event: done`. Errors: `event: error`. |

Engine routing is derived from the model id: `ollama:` prefix / Ollama registry names →
Ollama; `evo-native:` / names containing `evo`/`forging` → native EVO engine;
`llama-cpp:` / `*.gguf` artifacts → llama.cpp.

### 3.4 Runtime and discovery

| Method | Path | Description |
|---|---|---|
| GET | `/enr/v1/runtime` | CPU (processors, load average), heap, GPU status (when the EVO GPU detector bundle is present), active engines |
| GET | `/enr/v1/providers` | `[{id, name, type, baseUrl, local, available}]` |
| GET | `/enr/v1/capabilities` | `[{id, name, description, features}]` — family capability descriptors |

## 4. Errors

All non-2xx responses use the family TEIP error envelope shape:

```json
{"code": "...", "message": "...", "retryable": false, "timestamp": "..."}
```

Codes: `AUTHENTICATION_FAILED` (401), `VALIDATION_FAILED` (400), `NOT_FOUND` (404),
`CONFLICT` (409), `INTERNAL_ERROR` (500).

## 5. Authorization

Bearer token required only when configured via system property `evo.enr.auth.token`
(family dev-mode convention, TEIP 1.1 §6). Without a configured token the API is
loopback-dev-mode.

## 6. Configuration

| Key | Default | Description |
|---|---|---|
| `evo.enr.port` | `28780` | HTTP port (first free port in +100 fallback range used on conflict) |
| `evo.enr.auth.token` | — | Optional bearer token |
| `evo.enr.server.enabled` | `true` | Disable HTTP server in-process |
| `evo.enr.router.enabled` | `true` | Wire ENR into the Darwin `LlmRouter` |
| `evo.ollama.url` / `EVO_OLLAMA_URL` | `http://localhost:11434` | Ollama provider endpoint |

## 7. In-process contract

`EnrInferenceEngine` (Java, exported package `eu.kalafatic.evolution.enr`):

- `health()`, `listModels()`, `findModel(id)`, `loadModel(id)`, `unloadModel(id)`,
  `deleteModel(id)`, `pullModel(id, progress)`
- `chat(EnrChatRequest)` → `EnrChatResponse`
- `streamChat(request, chunkConsumer, cancellationSupplier)` — final chunk has `done()==true`
- `listProviders()`, `runtimeStatus()`, `capabilities()`

## 8. Ownership and fallback

- ENR owns inference runtime state (provider connections, model lifecycle).
- Applications own sessions/tasks/workspaces; ENR is stateless per request.
- ENR unavailable ⇒ alternative inference providers remain usable; the Darwin router
  falls back to its direct Ollama/native provider paths unchanged.
- Model unavailable ⇒ the caller selects another discovered model; ENR reports the
  failure with the engine name.

## 9. Versioning

ENR 1.0. Breaking wire changes require a 2.0 document; additive endpoints are allowed
within 1.x.
