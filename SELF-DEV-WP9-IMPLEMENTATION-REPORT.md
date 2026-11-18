# SELF-DEV WP9 IMPLEMENTATION REPORT — Git Lifecycle and Rollback Policy (Simulation Only)

Status: IMPLEMENTED + UNIT TESTED. ZERO real Git mutations performed.
Date: 2026-11-18.

## 1. Changes implemented

### 1.1 Git simulation boundary (GitManager)
- `GitManager` gained a simulation mode enabled BY DEFAULT (`evo.git.simulation=true`). While enabled, NO mutating Git command is executed; every mutating operation (init, config, add, commit, checkout, branch, merge, worktree add/remove/prune, push, reset/clean) is validated and recorded as `[SIMULATED]` via `getSimulatedOperations()`. Real mutation requires an explicit operator opt-out (`-Devo.git.simulation=false`).
- Read-only operations (branch, HEAD, `status --porcelain`, `ls-files`) remain real and are the only Git commands executed.

### 1.2 Iteration baseline (new GitBaseline)
- Read-only baseline capture at run start: branch, HEAD, full `status --porcelain` snapshot; persisted at `<run>/git-baseline.json`.
- `iterationChangedPaths(currentStatus)` attributes ONLY paths not present in the baseline — pre-existing user changes can never become part of an autonomous iteration (D-41/D-47).

### 1.3 Commit eligibility (D-44)
- `GitManager.commitWithEligibility(message, context)`: requires recorded BUILD_EVO + EXPORT_EVO success and non-skipped test evidence in the artifact manifest. Empty/dummy commits are blocked. The legacy `commit(...)` signature remains for non-self-dev callers (now simulated by default).

### 1.4 Push authorization gate (D-45)
- `GitManager.pushWithAuthorization(remote, branch, context)`: separate, authorization-gated operation. Default state is PUSH_PENDING (prepared commit, no push) unless `SelfDevContext.setGitPushAuthorized(true)` was explicitly set. Force-push is refused. Remote/branch must be explicit.

### 1.5 Destructive rollback replaced (D-40/D-41)
- `GitManager.rollback` now REFUSES destructive `reset --hard`/`clean -fd` without a captured baseline (simulated refusal by default; exception in real mode).
- New `restoreIterationBaseline(baseline, changedPaths, context)` performs iteration-scoped restoration: tracked paths via `checkout --`, untracked paths via targeted file deletion; traversal/absolute paths are refused (D-48). Pre-existing changes are untouched by construction.

### 1.6 Supervisor patch protection (D-42)
- `ProcessRunner.applyPatch`: the unconditional `git reset --hard`/`git clean -fd` pre-patch cleanup is REMOVED. A dirty working tree (read-only `status --porcelain`) now refuses patch application. Simulation property gates real execution.

### 1.7 No silent retargeting (D-47)
- `GitTool` working-directory retargeting is now logged explicitly instead of silent.

## 2. Tests

New test class: `SelfDevGitLifecycleSimulationTest` (9 tests, all passing):
- simulation mode default
- baseline capture read-only + persisted + reloaded
- commit eligibility gate blocks without evidence
- eligible commit is simulated and the repository is unchanged
- destructive rollback refused without baseline
- scoped restoration simulated; unsafe paths refused
- iteration change attribution excludes pre-existing changes
- push requires explicit authorization (PENDING → authorized simulated → force-push refused)
- all mutating operations simulated (6 recorded) with repository unchanged

Every test verifies HEAD and working-tree status (read-only) before and after — the repository state is asserted unchanged.

## 3. Verification

- Controller + supervisor bundles compile: BUILD SUCCESS.
- `SelfDevGitLifecycleSimulationTest` 9/9 PASS; `SelfDevPortIsolationTest` 3/3 PASS.
- Read-only verification after the suite: HEAD unchanged (`cbbfa121...`), working tree state unchanged.

## 4. Defect register updates

- D-40 FIXED (destructive reset/clean replaced by baseline-required scoped restoration + simulation default).
- D-41 FIXED (iteration baseline capture; change attribution excludes pre-existing changes).
- D-42 FIXED (supervisor patch application refuses dirty trees; no pre-patch destruction).
- D-44 FIXED (commit eligibility gate: build+export+test evidence required).
- D-45 FIXED (push authorization gate; PUSH_PENDING default; force-push refused).
- D-47 FIXED (no silent working-directory retargeting).
- D-48 FIXED (unsafe path refusal in restoration).
- D-46 PARTIALLY_FIXED (GitManager/GitTool remain the self-dev authority; the parallel integrations outside self-dev are documented but not consolidated — out of scope without caller migration).
- D-49 PARTIALLY_FIXED (ensureInitialCommit is simulated by default).
