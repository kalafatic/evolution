# EVO — Update Guide

Git is controlled exclusively by the user (Eclipse EGit). UPM provides the safe orchestrated workflow below; it never discards local changes, never resets, never rebases, never switches branches silently.

## Safe update workflow (Update All)

1. **Inspect status** — branch, dirty files (modified/staged/untracked).
2. **Stop on local changes** — any uncommitted change aborts the update.
3. **Fetch** remote refs (`git fetch --all --prune`) — never touches the working tree.
4. **Divergence analysis** — outgoing vs incoming commits via `rev-list --left-right --count`.
5. **Stop if unpushed local commits** exist (no rebase/merge is performed automatically).
6. **Require explicit confirmation** before integrating incoming commits.
7. **Integrate with `git pull --ff-only`** only.
8. **Update dependencies** (`./mvnw -B -U -DskipTests clean verify`) when requested.
9. **Validate** (`./mvnw -B validate`) the integrated state.
10. Report the full step log.

## Individual operations

| Operation | Command | Safety |
|---|---|---|
| Check Updates | `git fetch --all --prune` + divergence report | read-only for the worktree |
| Fetch | `git fetch --all --prune` | safe |
| Pull | `git pull --ff-only` | fast-forward only |
| Status | `git status --porcelain`, `git branch --show-current`, `git remote -v` | read-only |
| Update Dependencies | `./mvnw -B -U -DskipTests clean verify` | never silently upgrades versions beyond declared ranges |

## Verified behavior (UPM, disposable copy)
- Update All **refused** with local modifications present (changes preserved).
- Update All **refused** without confirmation when 1 incoming commit existed.
- Update All with `confirm=true` integrated 1 commit via fast-forward.
- Update All **refused** when the local branch had unpushed commits.
- Check Updates reported 0/1 divergence correctly.
