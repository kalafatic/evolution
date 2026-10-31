# Forge Output Integration with Darwin Inference Engine

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/forge-darwin-integration.md`

---

## 1. Native Model Discovery & Inference Routing

- **Model Registration**: Trained `.evo` model files located in `models/` or exported to local Ollama repositories are discovered dynamically by `ModelCapabilityDetector` and `OllamaProvider`.
- **Reference Inference Engine**: `ReferenceEvoInferenceEngine` executes local native autoregressive generation with top-k (40), top-p (0.9), and repeat penalty (1.1) sampling constraints.
- **Darwin Execution Mode**: Darwin instances configured with `PlatformType.DARWIN_MODE` or `AiMode.LOCAL` invoke trained native models through `AiService.sendLlmRequest(...)`.
