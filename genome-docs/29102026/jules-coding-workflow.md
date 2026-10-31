# Jules-like Coding Workflow Specification & Guide

**Date:** October 29, 2026
**System:** EVO Platform
**Document ID:** `jules-coding-workflow.md`

---

## 1. Overview & Core Interaction Model

The Jules-like coding workflow in EVO enables session-based, repository-aware AI software engineering driven by human review and explicit git commitments.

### Central Interaction Model

`Repository -> Coding Session -> Inference -> AI-generated Changes -> Human Review -> Explicit Commit`

---

## 2. Complete Workflow Lifecycle

1. **Repository Selection & Validation:**
   - User provides local or remote Git repository location and target branch via `GitServiceImpl` or UI.
2. **Coding Session Initialization:**
   - EMF `Orchestrator` and `SelfDevSession` initialize a session with task context.
3. **Repository Context Selection:**
   - Darwin inference engines (`ADarwinEngine`, `SelfDevDarwinEngine`, `RealityDiscoveryAgent`) query relevant packages, metadata, and source files.
4. **Inference Execution & Code Generation:**
   - Generated changes are structured into `ChangeSet`, `FileChange`, and `DiffHunk` models.
5. **Change Set Isolation:**
   - AI-generated changes are held in `ReviewSession` isolation and flagged as `PENDING`.
6. **Human Code Review:**
   - User inspects unified diffs and file trees in `PeerReviewPage` (`DiffViewerGroup`).
   - File-level acceptances set status to `ACCEPTED`; rejections set status to `REJECTED`.
7. **Explicit Commit Execution:**
   - User triggers an explicit commit action (`commitChanges`).
   - Only `ACCEPTED` files are staged and committed into the target branch.
   - Pushing to remote repositories is NEVER performed automatically.
8. **Session Persistence & Reopening:**
   - The entire conversation, review decision state, and commit details persist across restarts.
