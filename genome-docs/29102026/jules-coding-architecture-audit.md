# Jules-like Coding Workflow Architecture Audit & Gap Analysis

**Date:** October 29, 2026
**System:** EVO Platform (Eclipse RCP / OSGi / Java 21 / EMF / Darwin Inference)
**Document ID:** `jules-coding-architecture-audit.md`

---

## 1. Executive Summary

This architecture audit evaluates the existing Jules-like coding workflow within the EVO platform. The objective is to establish an end-to-end, session-based AI coding workflow that adheres to the central interaction model:

`Repository -> Coding Session -> Inference -> AI-generated Changes -> Human Review -> Explicit Commit`

---

## 2. Architecture Inventory & Component Tracing

### 2.1 Repository Management
- **Primary Classes:** `GitServiceImpl`, `GitManager`, `GitVersionControlProvider`, `GitTool`, `GitGroup`, `GitSettingsPage`.
- **Capabilities:**
  - Validates Git repositories, remote URLs, and local directory structures.
  - Supports branch creation, checkout, diff generation, changed-file listings, and commit operations.
  - Distinguishes repository URL, local working directory, Git branch, and working-tree state.

### 2.2 Coding Session & Lifecycle
- **Primary Classes:** `SelfDevSession`, `SelfDevContext`, `Orchestrator`, `SessionManager`, `AiCommanderPage`, `ChatGroup`.
- **Capabilities:**
  - Session lifecycle managed through EMF `Orchestrator` and `SelfDevSession` models.
  - Chat interface (`ChatGroup`) interacts with Darwin inference and session context.
  - Reopening sessions reconstructs conversation history and associated session state.

### 2.3 Darwin & Inference Integration
- **Primary Classes:** `ADarwinEngine`, `SelfDevDarwinEngine`, `RealityDiscoveryAgent`, `ReviewerAgent`.
- **Capabilities:**
  - Leverages Darwin engine pipeline for intent recognition, prompt synthesis, and code generation.
  - Extracts generated code into change models (`ChangeSet`, `FileChange`, `DiffHunk`).

### 2.4 Code Review & Diff Visualization
- **Primary Classes:** `PeerReviewPage`, `DiffViewerGroup`, `ApprovalPage`, `ReviewGroup`, `SummaryGroup`.
- **Capabilities:**
  - `PeerReviewPage` and `DiffViewerGroup` render file trees and unified diff viewers.
  - `ApprovalPage` provides overall approval/rejection gates.

---

## 3. Gap Analysis & Required Incremental Improvements

1. **Change Set Isolation & File-Level Decision Tracking:**
   - **Gap:** `FileChange` models currently lack explicit per-file review status flags (`PENDING`, `ACCEPTED`, `REJECTED`).
   - **Improvement:** Introduce file-level review status tracking on `FileChange` and `ChangeSet` to isolate unreviewed and rejected proposals from the repository working tree.

2. **Code Review Panel Interaction:**
   - **Gap:** UI currently supports bulk pass/fail approval rather than individual file-level accept/reject decisions.
   - **Improvement:** Add individual file accept/reject controls in `DiffViewerGroup` / `PeerReviewPage` and synchronize decision states across all views.

3. **Explicit User-Controlled Commit Workflow:**
   - **Gap:** Staging and commit operations were tied to automatic tasks or full approvals.
   - **Improvement:** Provide an explicit commit action with an editable commit message that stages ONLY accepted files, excludes rejected or unreviewed proposals, and guarantees NO automatic push.

4. **Server REST Endpoint Integration:**
   - **Gap:** Server REST API lacks explicit endpoints for retrieving change-sets, accepting/rejecting individual file changes, and executing explicit commits.
   - **Improvement:** Extend server controllers to expose `/api/coding/changeset`, `/api/coding/review`, and `/api/coding/commit` endpoints.

---

## 4. Architectural Boundaries & Invariants

- **Java 21 / Eclipse RCP / OSGi / EMF Architecture:** Retain existing framework without external frameworks or Python/Node.js backends.
- **Repository Authority:** The Git repository is the sole source of truth for committed code.
- **Human Authority:** The user explicitly reviews and accepts changes before any commit occurs.
- **No Auto-Push:** Pushing to remote repositories must never occur automatically.
