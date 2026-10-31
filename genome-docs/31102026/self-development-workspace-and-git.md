# Self-Development Workspace Isolation & Git Safety

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/self-development-workspace-and-git.md`

---

## 1. Workspace Isolation Guarantees

Every Self-Development execution operates within an isolated working directory structure:
```
workspace/self-dev-run/
├── source/      # Isolated Git clone / working directory copy
├── build/       # Tycho / Maven target compilation directory
├── export/      # Exported Eclipse RCP product layout
├── runtime/     # Deployed execution environment
└── logs/        # Run-isolated logs (session_<id>.log)
```

- **Canonical Repository Protection**: The canonical Git repository configuration (`EVO_GIT_REPOSITORY`) is strictly read-only relative to the self-development build process.
- **Automated Commit Prohibition**: AI-generated code changes are never committed or pushed to canonical repositories automatically; explicit user review and staging boundaries are strictly preserved.
