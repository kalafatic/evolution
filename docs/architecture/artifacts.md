# Canonical Artifact Inventory and Ownership

**Document Identifier**: `docs/architecture/artifacts.md`
**Date**: September 9, 2026

---

## 1. Canonical Artifact Inventory

| Artifact Type | File Extension / Container | Producer / Owner | Semantics & Format | Immutability |
| :--- | :--- | :--- | :--- | :--- |
| **Training Dataset** | `.evodata` (ZIP Archive) | `eu.kalafatic.evolution.forge.data` | Contains `metadata.json`, `data.jsonl`, `val_data.jsonl`, `report.txt`. Standardized dataset protocol v1/v2. | Immutable once `READY`. |
| **Native Neural Model** | `.evo` (Binary Container) | `eu.kalafatic.evolution.forge.model` | Dual v1 (ZIP/Dir) or v2 (`EVO_NAT2` Little-Endian binary) format storing manifest, architecture, vocabulary, and float32 weights. | Immutable. Continuation creates child `.evo`. |
| **Exported GGUF Model** | `.gguf` (GGUF v3 Binary) | `eu.kalafatic.evolution.forge.agent.api` | Transposed column-major GGML float tensor payloads validated via `GGUFValidator`. | Immutable binary artifact for Ollama / `llama.cpp`. |
| **Self-Dev Supervisor Run** | `self-dev-run/` Directory | `eu.kalafatic.evolution.supervisor` | Contains build logs, `state.json`, and deployed product target binaries. | Mutable during run execution; frozen on completion. |
| **Git EVO Revision** | Git Repository Commit/Tag | `eu.kalafatic.evolution.controller.git` | Model state and lineage metadata registered under `.evo/` Git refs. | Immutable commit history. |

---

## 2. Model Lineage & Provenance Chain

Every `.evo` child model records explicit provenance metadata linking back to its origin:

```text
chat-v1.evo
   │
   ├── Parent Hash: sha256:...
   ├── Forge Job ID: F-20260909-01
   ├── Dataset Artifacts: [dataset-en-v1.evodata, codebase-v2.evodata]
   └── Evaluation Score: 0.892
         │
         ▼
chat-v2.evo
```

---

## 3. Artifact Validation Invariant (I9)

No artifact is marked `READY` or made available to registries without passing mandatory preflight and post-execution validation checks:
- `.evodata` validated for schema, token accounting, and non-zero record count.
- `.evo` validated via `EvoModelValidator` (tensor bounds, architecture invariant checks).
- `.gguf` validated via `GGUFValidator` independent byte-stream parser before Ollama registration.
