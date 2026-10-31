# AI Coding Changeset Management & Peer Review

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/ai-coding-changesets-and-review.md`

---

## 1. EMF ChangeSet Domain Model

Proposed changesets are modeled via EMF Ecore entities:
- **`ReviewSession`**: Encapsulates `id`, `repositoryUrl`, `branch`, `reviewStatus`, `ChangeSet`, and `Comment` list.
- **`ChangeSet`**: Contains `files` (`List<FileChange>`) and `baseCommit`.
- **`FileChange`**: Tracks `filePath`, `changeType` (`ADD`, `MODIFY`, `DELETE`), `originalContent`, `newContent`, and `hunks`.
- **`DiffHunk`**: Line-level unified diff hunks with line numbers.

---

## 2. Peer Review UI Layout

`PeerReviewPage.java` provides a 3-panel horizontal `SashForm` (25%, 50%, 25% weights):
1. **Left Panel (`FileTreeGroup`)**: Changed files tree showing add/modify/delete status badges.
2. **Center Panel (`DiffViewerGroup`)**: HTML-rendered unified diff viewer with line numbers and line-click comments.
3. **Right Panel (`CommentsGroup`)**: Review comments, decision buttons (Accept, Reject), and commit preparation.
