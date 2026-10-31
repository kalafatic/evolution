# Model Quantization & Format Conversion Pipeline

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/forge-quantization.md`

---

## 1. Supported Quantization Schemes

- **Native `.evo` Quantization**: `EvoModelQuantizer` converts F32 model weight float arrays to `F16`, `INT8`, or `INT4` representations with per-tensor scale factors.
- **GGUF Export Quantization**: `OllamaExporter` exports GGUF quantization schemes (`F16`, `Q8_0`, `Q4_0`, `Q4_K_M`) validated by `GGUFValidator`.
- **Lineage Preservation**: The original baseline F32 model is strictly preserved; quantized models are registered as separate artifacts with distinct identifiers and scale metadata.
