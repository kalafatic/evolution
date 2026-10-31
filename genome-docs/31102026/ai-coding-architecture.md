# AI Coding Workflow Architecture & Subsystem Integration

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/ai-coding-architecture.md`

---

## 1. Overview & Operational Principles

The AI-assisted coding workflow in EVO integrates the Darwin Engine (`ADarwinEngine`, `CodingEngine`), EMF domain models (`ReviewSession`, `ChangeSet`, `FileChange`, `DiffHunk`), `PeerReviewService`, and Git integration (`GitTool`, `EclipseGitEvoTool`).

The workflow guarantees human-in-the-loop gatekeeping:
```
Repository → Coding Session → Inference → AI-generated ChangeSet → Peer Review → Explicit Acceptance → Explicit Commit
```
AI code modifications never commit or push automatically.
