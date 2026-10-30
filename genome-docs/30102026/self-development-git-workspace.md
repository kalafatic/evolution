# Self-Development Git & Workspace Isolation Guarantees

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/self-development-git-workspace.md`

---

## 1. Repository Handling & Workspace Separation

- **Canonical Repository vs. Workspace**: The canonical Git repository configuration (`EVO_GIT_REPOSITORY`) is strictly separated from temporary run directories.
- **Source Copy**: `CopySourceTask` creates an isolated working copy in `<workspace>/self-dev-run/` or `target/workspace_copy/`.
- **Exclusion Invariants**: Temporary build outputs (`target/`, `bin/`) and run directories (`run_*`) are excluded from contaminating the canonical Git repository.

---

## 2. Directory Structure Lifecycle

```
workspace/self-dev-run/
├── source/          # Isolated Git repository working tree copy
├── build/           # Tycho/Maven target build output
├── export/          # Exported RCP product artifact (.zip / .jar)
├── runtime/         # Deployed external runtime environment
└── logs/            # Execution logs and diagnostic output
```

- Cleanup of run directories occurs deterministically upon run completion or failure, without removing active resources belonging to other parallel runs.
