# EVO NATIVE RUNTIME (ENR) — API REFERENCE

Describes the **actual** implemented HTTP API (`EnrHttpServer`, base path `/enr/v1`, default port **28780**, loopback). Status: IMPLEMENTED. Errors use `{"error": "<message>", "detail": ...}` envelopes with proper HTTP codes (400/401/404/409/500).

## 1. Health & status

| Endpoint | Method | Response |
|---|---|---|
| `/health` | GET | `{status: "ok"|"degraded", providers, uptimeMs, modelsLoaded}` |
| `/status` | GET | runtime status summary |
| `/enr/v1/runtime` | GET | `EnrRuntimeStatus`: engine id/version, providers, capabilities, GPU status (honest: `available/device/label`, never claims CUDA execution) |
| `/enr/v1/providers` | GET | provider list with capabilities |
| `/enr/v1/capabilities` | GET | aggregated capabilities |

## 2. Model registry

| Endpoint | Method | Body/params | Response |
|---|---|---|---|
| `/enr/v1/models` | GET | — | model list (id, provider, state, size) |
| `/enr/v1/models/{id}` | GET | — | single model + metadata; 404 unknown |
| `/enr/v1/models/{id}/load` | POST | `{}` | load result; 409 for unsupported ops on native models |
| `/enr/v1/models/{id}/unload` | POST | `{}` | unload result |
| `/enr/v1/models/{id}` | DELETE | — | delete (Ollama only; native → 409) |
| `/enr/v1/models/{id}/pull` | POST | registry params | pull (Ollama only; native → 409) |

## 3. Inference

| Endpoint | Method | Body | Response |
|---|---|---|---|
| `/enr/v1/chat` | POST | `{model, messages:[{role,content}], maxTokens, temperature, topP, topK, stop?}` | `EnrChatResponse` `{model, text, tokens, finishReason}` |
| `/enr/v1/chat/stream` | POST | same | `text/event-stream` SSE: `data: {chunk, token, cumulative, done}` per token |

Model id prefixes route providers: `evo-native:` (native `.evo`, in-JVM CPU), `.gguf`/`llama-cpp:` (llama.cpp), everything else → Ollama.

## 4. Auth

Optional bearer token: system property `evo.enr.auth.token`; requests without matching `Authorization: Bearer <token>` receive 401 when configured.

## 5. Port/registration conventions

- Default port **28780** (`PortRegistry.EVO_ENR`), loopback only, fallback scan +1..+100; override via `evo.enr.port`; disable via `evo.enr.server.enabled=false`.
- Java integration: `EnrLlmProvider` exposes ENR through the Darwin `ILlmProvider`/`LlmRouter`; the HTTP layer is for external clients (EVO Desktop, TRYST, DStudio).

## 6. Known limitations (as implemented)

- No per-request id/queue-depth metrics; no async job polling for chat; cancellation for in-flight native generations is supported in the engine (flag/interrupt) but not yet exposed as an HTTP cancel endpoint.
- Native model delete/pull unsupported (409).
