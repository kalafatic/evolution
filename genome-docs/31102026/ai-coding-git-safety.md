# AI Coding Git Safety & Working Tree Protection

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/ai-coding-git-safety.md`

---

## 1. Git Safety Invariants

1. **Worktree Isolation**: Code generation occurs in temporary isolated worktrees (`evo-variant-<id>`) or temporary directory copies, preventing unreviewed changes from touching the active user working tree.
2. **Explicit Human Review**: Changes are applied to the working tree only after explicit acceptance via `PeerReviewPage`.
3. **Explicit Commit**: Accepted changes are staged and committed only upon user invocation (`Commit` action) with customizable commit message.
4. **Push Prohibition**: The platform never automatically pushes commits to remote repositories.
