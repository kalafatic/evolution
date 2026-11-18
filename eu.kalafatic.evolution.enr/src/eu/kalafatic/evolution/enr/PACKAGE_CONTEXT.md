# ENR — Evolution Neural Runtime (Phase 6)

Package: `eu.kalafatic.evolution.enr`

ENR is the family inference layer. It unifies the existing EVO inference stack — Ollama,
llama.cpp and the native EVO engine — behind one stable, published contract
(`EnrInferenceEngine`, ENR 1.0). It does not own application sessions, coding tasks or
repository workflows.

- `EnrInferenceEngine.java` — stable inference contract + wire/domain records (ENR 1.0).
- `EnrRuntime.java` — default runtime; delegates model discovery/lifecycle to
  `OllamaService`/`OllamaManager`, native generation to `NativeEvoProvider`, llama.cpp to
  `LlamaService`; GPU status reuses `eu.kalafatic.evolution.gpu` reflectively when present.
- `EnrHttpServer.java` — HTTP/SSE surface under `/enr/v1` (family port 28780).
- `EnrLlmProvider.java` — bridges ENR into the Darwin `ILlmProvider`/`LlmRouter` abstraction.
- `EnrActivator.java` — OSGi wiring: service registration, router bridge, HTTP server.
- `EnrMain.java` — standalone (non-RCP) entry point.

The wire contract is documented in `docs/protocol/ENR-1.0.md`.
