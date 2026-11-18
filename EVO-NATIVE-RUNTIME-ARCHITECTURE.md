# EVO NATIVE RUNTIME (ENR) — ARCHITECTURE

Describes the **actual** implemented ENR (bundle `eu.kalafatic.evolution.enr`). Status: IMPLEMENTED (partial vs. target state; see §6).

## 1. Components

```
EnrActivator (OSGi) ── starts EnrHttpServer (NanoHTTPD, port 28780 default, loopback)
EnrHttpServer  /enr/v1/* REST + SSE
   └─► EnrRuntime (default runtime, singleton)
        ├─► evo-native:    NativeEvoProvider + ReferenceEvoInferenceEngine (in-JVM CPU, forge.model)
        ├─► .gguf/llama-cpp: LlamaService (external llama.cpp process)
        └─► other:         OllamaManager / OllamaService (external Ollama)
EnrLlmProvider ── bridges ENR into Darwin ILlmProvider / LlmRouter
PortRegistry: EVO_ENR = 28780 (ResourceManager-registered; AGENTS.md §10 table omits it — doc drift G-9)
```

## 2. Runtime contracts (`EnrInferenceEngine` v1.0)

- `EnrHealth`, `EnrModel`, `EnrProgress`, `EnrChatMessage/Request/Response/Chunk`, `EnrProviderInfo`, `EnrRuntimeStatus`, `EnrGpuStatus`, `EnrCapability`.
- Model discovery: Ollama tags + `.evo`/`.gguf` artifacts under `forge-output`, `forge-models`, `models` subdirectories.
- Delete/pull: Ollama only; native providers answer 409 Conflict.
- Bearer auth optional (`evo.enr.auth.token`); server enable/disable (`evo.enr.server.enabled`); port override (`evo.enr.port`).

## 3. Model boundary with Darwin Forge

- Forge owns: dataset consumption, training, validation, checkpointing, `.evo` finalization, export.
- ENR owns: model loading, inference, model lifecycle, multi-model addressing (via providers), metrics/health.
- Boundary artifact: the `.evo` model file (see EVO-MODEL-FORMAT-SPECIFICATION.md) + Ollama/llama.cpp model registries for external providers.
- Native inference reuses `forge.model` (ReferenceEvoInferenceEngine: KV cache, temperature/topK/topP sampling, penalties, synchronous token streaming, EOS/stop handling, cancellation flag + thread-interrupt support since 2026-11-17).

## 4. Verified behaviors (tests: EnrHttpServerTest 12, EnrRuntimeTest 9, PortRegistryEnrTest)

- Health/status/models list/get/404 handling; load/unload; chat; error envelopes (400/404/409/500); SSE streaming; auth enforcement; providers/capabilities listing; offline-safe degradation when Ollama unreachable.

## 5. Honesty notes

- ENR does **not** fake GPU inference; native provider executes on CPU (float32 Java) and reports it as such via `EnrGpuStatus` (GPU detection is reflective NvidiaGpuDetector diagnostics only).
- The reference engine now supports cancellation (termination reason `CANCELLED`).

## 6. Implemented vs. target state

| Target requirement (WP5) | Status |
|---|---|
| Native `.evo` loading | IMPLEMENTED (integrity-verified since 2026-11-17) |
| CPU inference, KV cache, sampling, streaming, cancellation | IMPLEMENTED |
| HTTP + SSE API, health/registry/inference endpoints | IMPLEMENTED |
| Multi-model addressing (EVO-CHAT/CODER/REASONER style) | PARTIAL — via provider dispatch (`evo-native:` / `.gguf` / Ollama); no independent per-instance config/context objects for native models |
| Shared VRAM/RAM resource management, scheduling, memory-pressure handling | NOT IMPLEMENTED (G-4) |
| Runtime independence from Eclipse RCP/SWT | NOT IMPLEMENTED — bundle requires `eu.kalafatic.evolution.controller` (G-3); standalone claim in `EnrMain` javadoc is aspirational |
| Quantized native execution (F16/INT8 kernels) | NOT IMPLEMENTED — CPU float32 only |
| Metrics endpoint with per-model tok/s, TTFT, queue depth | PARTIAL — runtime status only |

## 7. Planned evolution (in priority order)

1. Decouple ENR from the controller bundle (extract `LlamaService`/`NativeEvoProvider`/`TaskContext` contracts behind local adapters).
2. Central `RuntimeResourceTracker` (per-model residency, memory estimates, pressure signals) exposed in `/enr/v1/runtime`.
3. Per-model instance descriptors (independent config/context/lifecycle for multiple native models).
4. Inference metrics (tok/s, TTFT, queue depth) per request/model.
