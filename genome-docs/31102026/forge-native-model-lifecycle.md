# Native .evo Model Container Lifecycle & Serialization

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/forge-native-model-lifecycle.md`

---

## 1. Native Model Protocol v2 (`.evo`)

Native `.evo` model artifacts are serialized using `EvoGpuModelSerializer` and `EvoModelArtifact`:
- **Magic Header**: `EVO_NAT2` (8 bytes).
- **Metadata Section**: Length-prefixed JSON headers for model architecture, tokenizer parameters, and tensor manifest.
- **Payload Section**: GZIP-compressed float tensor weight arrays.
- **Integrity**: Evaluated with SHA-256 content hashes, parameter bounds, and inference smoke testing via `ReferenceEvoInferenceEngine`.
