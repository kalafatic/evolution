# Workspace Directory Semantics and Target Mapping

**Document Identifier**: `docs/architecture/filesystem.md`
**Date**: September 9, 2026

---

## 1. Directory Structure Mapping

To eliminate directory ambiguity across platform modules, current workspace directory paths are mapped to standardized semantic roles:

| Workspace Path | Semantic Purpose | Owner Subsystem | Status |
| :--- | :--- | :--- | :--- |
| `models/`, `forge-output/` | Native `.evo` models, exported `.gguf`, and model snapshots. | Model / Forge Engine | Canonical model output directory. |
| `data/`, `.evodata` | Raw training datasets and serialized `.evodata` archives. | Dataset Preparation | Canonical training data directory. |
| `self-dev-run/` | Self-Dev supervisor logs, task state, and build artifacts. | Self-Dev Supervisor | Workspace runtime control plane. |
| `.evo/mutation-workspaces/` | Isolated Git repository clones for interactive coding agents. | Mutation Engine | Workspaces isolated from host checkout. |
| `iterations/` | Darwin evolution iteration snapshots and trajectory logs. | Darwin Engine | Darwin history repository. |
| `docs/` | System architecture, protocols, specifications, and milestones. | Platform Documentation | Platform documentation authority. |

---

## 2. Canonical Target Filesystem Layout

```text
<evo-home>/
├── models/             # Native .evo and exported .gguf models
├── datasets/           # Raw files and .evodata dataset artifacts
├── forge/              # Checkpoints, training run logs, evaluation metrics
├── darwin/             # Iteration trajectories and variant histories
├── selfdev/            # Supervisor workspaces, builds, and logs
├── git/                # Model versioning refs and lineage
├── logs/               # Structured application log files
└── cache/              # Temporary download and processing caches
```
