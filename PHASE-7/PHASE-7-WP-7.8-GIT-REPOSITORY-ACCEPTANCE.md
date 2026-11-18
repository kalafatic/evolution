# PHASE-7-WP-7.8-GIT-REPOSITORY-ACCEPTANCE.md

Phase 7 — WP-7.8 Git and Repository Isolation
Date: 2026-11-17
Status: COMPLETE (suite evidence + live TRYST/DCode runs)

## 1. Repository operations inventory

| Operation | DCode | TRYST | DStudio | Evidence |
|---|---|---|---|---|
| Clone | `JGitEngine.clone` | `GitService.cloneOrFetch` + mirror pool | via DCode harness (delegated) | suite green; live: TRYST validated a `file://` repo and cloned it into its sandbox |
| Pull | `JGitEngine.pull` (fetch+ff) | `GitService` | delegated | suite |
| Branch discovery | `JGitEngine.branches` | `GitService` | delegated | suite |
| Checkout | `checkoutBranch` / `checkoutRemoteBranch` | `GitService` | delegated | suite |
| Commit | `JGitEngine.commit` (explicit identity) | authorization-gated `GitService.commit` (working-tree diff verified against approved fingerprint; duplicate commits prevented) | delegated (DCode never commits) | suite |
| Diff | `JGitEngine.diff` (unified, untracked excluded) | review diff endpoint | `ChangeManager` change sets | suite + live review endpoints |
| Staging | via working-tree flows | via review/approval | via reviews | suite |

## 2. Ownership and isolation (verified)

- **DCode performs coding operations through its harness interfaces**: `RepositoryOperations`
  policy facade (`READ_ONLY` never modified; mutating ops throw
  `RepositoryAccessException`), `ToolEngine.resolveContained` rejects absolute paths,
  `..` traversal and symlink escapes; FHC 1.0 §3: DCode never commits in delegated mode
  (sessions end `WAITING_FOR_APPROVAL`).
- **Application-specific repository ownership intact**:
  - TRYST: per-task sandbox `~/.tryst/workspace/sandboxes/{ownerId}/{taskId}` with
    lexical+canonical containment (`PermissionManager.validatePathWithinWorkspace`),
    process sandbox (cwd containment, env sanitization, process-group kill), mirror
    pool under `<workspace>/mirrors`; cleanup refuses ACTIVE/INITIALIZING/WAITING_FOR_REVIEW/
    REWORKING tasks.
  - DStudio: project directories under `~/.dstudio`; reviews/approvals own the commit
    decision.
  - EVO: `GitManager` inside the Darwin session workspace; target resolution explicit
    (`ChatSession.targetPath` → context project root → error).
  - DCode: `~/.dcode/workspaces/{id}` with `RepositoryWorkspaceManager` provisioning
    (existing `.git` reused, empty dir cloned, non-empty non-git dir rejected).
- **No application can accidentally modify another application's active workspace**:
  separate roots (`~/.dcode`, `~/.tryst`, `~/.dstudio`, EVO data dir); ENR never
  touches repositories; live Phase 7 runs confirmed TRYST tasks operated only inside
  its sandbox (DCode sessions created by TRYST pointed at the TRYST workspace path).
- **Concurrent tasks**: TRYST worker pool with per-task sandboxes; DCode fixed
  4-thread executor with per-session workspaces; multi-repo ops documented as
  non-atomic (deliberate).
- **Interrupted operations / cleanup**: DCode recovery marks interrupted sessions
  FAILED; TRYST cleanup refuses active work; no live workspace corruption observed.

## 3. Live evidence collected in Phase 7

- TRYST headless: `POST /api/tasks/intake/validate-repo` for a local repo →
  `{"valid":true,"type":"LOCAL","branch":"master"}`; task submitted, cloned into the
  sandbox, executed through the harness path, reached `WAITING_FOR_REVIEW` with a
  review record (diff endpoint verified, fingerprint SHA-256).
- DCode live server: session run against a real JGit-backed workspace; change
  collection/review flow exercised via HTTP API.
- No cross-workspace writes were observed in any live run.

## 4. Defects

None new. (DCode headless product launch defect D-9 does not affect repository
operations, which are suite-covered.)
