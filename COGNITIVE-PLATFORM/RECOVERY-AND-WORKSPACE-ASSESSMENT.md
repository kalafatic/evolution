# RECOVERY AND WORKSPACE ASSESSMENT
Cognitive Services Implementation — Phase 0 Gate Report

Date: 2026-11-17
Author: automated recovery assessment

---

## 1. Server health

| Check | Result | Status |
|---|---|---|
| OS | Ubuntu 24.04.5 LTS, kernel 7.0.0-43-generic | OK |
| Uptime | 34 min at assessment start (rebooted ~12:15–12:19) | Rebooted recently (expected after crash) |
| Load | 2.75 / 2.28 / 1.55 (64 vCPU) | OK |
| CPU | 64 cores, AMD EPYC 7302 | OK |
| Memory | 121 GiB total, 108 GiB available, swap 0 used | OK |
| GPU | NVIDIA RTX 5070 Ti, 16 GiB, 258 MiB used, available | OK |
| Disk / | 1.8 TiB, 746 GiB used (43%), 995 GiB free | OK |
| Inodes | 1.5% used on / | OK |
| Network | enp100s0f0 UP 192.168.0.130/24, default route OK, ping 8.8.8.8 OK | OK |
| Tools | java (8/17/21/25), maven 3.8.7, git, node, python3, sqlite3, docker | OK |
| Java 21 | /usr/lib/jvm/java-21-openjdk-amd64 present (required for EVO) | OK |

**Crash indicators found:**
- `journalctl -b -1` shows normal services; current boot shows only corrected MCE hardware errors (CE, "no action required", EPYC MCA) and benign GDM/gnome-keyring warnings.
- `/var/crash/`: `_usr_bin_python3.12.1000.crash` (SIGSEGV, 2026-11-16 20:44, `python3 -` — external python process, not EVO), `libnvidia-gl-595.0.crash` (2026-11-12).
- No OOM kills, no filesystem errors, no EVO-related crash artifacts.

**Server health classification: HEALTHY.**

## 2. Port status (project conventions vs actual listeners)

| Port | Convention | Actual listener at assessment |
|---|---|---|
| 28080 | EVO HTTP primary | Occupied by external process (open-webui, root, python ai_env) — EVO not running |
| 28180 | Portal/secondary | free |
| 28280 | EVO MCP | free (EVO not running) |
| 28380 | DEVELOP | free |
| 28480/28580 | Supervisor | free |
| 28680 | Auxiliary | free |
| 28780 | ENR | free |

Note: EVO itself is not running (no EVO java processes). Port 28080 is currently held by open-webui (unrelated external app started 12:19). This is a runtime conflict to be aware of when EVO is started later; not modified here.

## 3. OpenCode recovery state (observed only, never modified)

- `~/.local/share/opencode/`: active `opencode.db` (402 MB, with `-wal` 4.1 MB / `-shm`) is in use by the running session.
- Recovery artifacts preserved: `opencode.db.corrupted` (400 MB, 2026-11-16 12:30), `recovery-backup/` containing `opencode.db`, `opencode-corrupted-original.db`, `opencode-working.db`, `snapshot/` (db + wal + shm).
- External recovery areas: `~/opencode-recovery-20261116/opencode-corrupted.db`, `~/opencode-phase3-clean/`.
- **No recovery artifact was deleted, moved, or modified.** OpenCode recovery is complete (active DB healthy, current session runs on it).
- Classification: **HEALTHY (recovery completed, artifacts preserved).**

## 4. Repository status

Git inspection was performed with read-only commands only (status/log/branch/fsck). No writes, no resets, no clean.

| Repository | Branch | fsck | Conflicts | Deleted tracked files | Uncommitted changes |
|---|---|---|---|---|---|
| evolution-121126 (EVO) | master | clean | 0 | 0 | 105 files (substantial Phase 1/6/7 work, see §6) |
| DCoder-121126 | master | clean | 0 | 0 | 20 untracked (project scaffolding + reports) |
| tryst-121126 | master | clean | 0 | 0 | 69 modified (TEIP/Phase-6/7 work) |
| DStudio-121126 | master | clean | 0 | 0 | 9 untracked |
| evo-native-runtime-161126 (ENR native) | master | clean | 0 | 0 | 17 untracked |

Notes:
- EVO HEAD: cbbfa121 (merge PR #1796, developer-mode integration). All Phase 6/7 + Cognitive Phase 1 work is uncommitted on top of it.
- No merge conflicts, no interrupted operations (no MERGE_HEAD, no rebase dirs), no stashes.
- All repositories accessible and readable.

Classification: **HEALTHY WITH WARNINGS** (large uncommitted work surface in EVO/TRYST that must be preserved; nothing corrupted).

## 5. Database integrity

| Database | Integrity check | user_version | WAL/SHM present | Status |
|---|---|---|---|---|
| EVO data/evolution.db | ok | 0 | shm 32 KB + wal 0 B (leftover from crash) | HEALTHY |
| EVO controller.tests/data/evolution.db | ok | 0 | — | HEALTHY |
| TRYST ~/.tryst/tryst.db | ok | 0 | — | HEALTHY |
| DStudio ~/.dstudio/dstudio.db | ok | 0 | — | HEALTHY |
| DCode ~/.dcode/dcode.db | ok | 0 | — | HEALTHY |
| OpenCode opencode.db | (active; recovery completed) | — | live WAL | HEALTHY (do not touch) |

- No corrupted SQLite databases found. All `PRAGMA integrity_check` return `ok`.
- `data/evolution.db` has an empty WAL and an SHM file — consistent with an interrupted EVO process; the database itself is intact.
- Backups to create before any migration work: see §8.

## 6. Existing uncommitted work (valuable, preserved)

The crash interrupted an active Cognitive Services implementation session. The following uncommitted work exists in EVO and is treated as the baseline to continue from:

**servers bundle (Phase 1 — Foundation & Security, largely implemented):**
- `service/IdentityService.java` (280 lines, complete: app registration, token issuance/validation/revocation, SHA-256 hashing, auditing)
- `service/CognitiveScopes.java`, `service/ValidatedPrincipal.java`
- `model/ApplicationIdentity.java`, `model/ApplicationAccess.java`
- `repository/ApplicationRepository.java`, `repository/ApplicationAccessRepository.java`
- `security/TokenHasher.java`
- `mcp/server/McpAuthenticator.java` + `JsonRpcDispatcher` modifications (bearer auth hook, tool scope requirements)
- `validation/` package: `ValidationPolicyService` (511 lines, rule catalog incl. SECURITY-fixed rules, global strict switch), `ValidationRule`, `ValidationCategory`, `ValidationMode`, `ValidationResult`, `ValidationException`
- `database/DatabaseManager.java`: 21 additive cognitive tables (applications, application_tokens, validation_policy_config/history, memory_index, cognitive_sessions, session_relationships, session_snapshots, conversations, conversation_messages, user_profiles, user_preferences, project_*, context_snapshots)
- `controller/AdminApiController.java`: `/api/admin/applications*` and `/api/admin/validation*` endpoints
- `mcp/demo/*` modifications

**controller bundle (WP1 memory model foundations):**
- `memory/MemoryEntry.java`: extended (userId, applicationId, projectRef, sessionRef, taskRef, validity, informationStatus, version, lastVerifiedAt, relationships, provenanceChain; backward-compatible JSON)
- New: `MemoryInformationStatus`, `MemoryRelationship`, `MemoryValidity`, `MemoryVersion`; `MemoryType`/`MemorySource` extended
- `harness/` package (Phase 6 coding harness adapters — separate work stream)
- Phase 6/7 TEIP, capability, tool, LlmRouter, KernelFactory changes

**Build evidence:** servers jar `eu.kalafatic.evolution.servers-2.6.5-SNAPSHOT.jar` built 2026-11-17 07:47 with all Phase 1 classes compiled (IdentityService, ValidationPolicyService, McpAuthenticator .class files present in target/classes). Controller/enr/view jars also built.

## 7. What is NOT yet implemented (gap from spec)

- controller `cognitive/` package: none of the Phase 2–4 services exist yet (ConversationHistoryService, SessionContinuityService, ProjectKnowledgeService, UserContextService, PersonalizationService, ContextReconstructionService, ContextQualityService, TokenEstimationService, MemoryConsolidationService, KnowledgeExtractionService, CognitiveContextService facade).
- `MemoryService` not yet wired to versioning/FTS mirror (MemoryEntry model done, service unchanged).
- No `CognitiveMcpToolProvider`; `McpDemoServerManager` (28280) does not wire authenticator or cognitive tools yet.
- No `ContextPackage` extension (cognitive DTOs).
- View bundle: no Cognitive Services administration pages (AdminPage is 141 lines, pre-existing).
- ENR: `EnrModel` lacks contextLength; `EnrChatResponse` lacks token usage fields.
- DCode: no `/sessions/{id}/context` ingest endpoint; metadata_json round-trip fix not verified.
- TRYST/DStudio: no EvoMcpClient.
- No tests for the new cognitive components.

## 8. Backups and protection measures

**Planned (per spec §2.4):**
- Backup of `data/evolution.db` + `controller.tests/data/evolution.db` to `/home/petr/git/evolution-121126/backups/` before first migration run; verify readability by reopening with sqlite3.
- Migration history: `user_version` = 0 on all DBs; new tables are additive `CREATE TABLE IF NOT EXISTS` (idempotent).
- Isolated test databases only for destructive/migration tests (temp dirs), never the development DB.
- OpenCode recovery artifacts: never touched.

**Do-not list (active):** no git reset/clean/commit; no DB destructive repair; no deletion of recovery files; no WAL/SHM removal; no changes to open-webui/JBoss/Tomcat or any unrelated system service.

## 9. Risks identified

1. **Uncommitted Phase 1 work** — 105 modified/untracked files in EVO; a crash during continuation could lose it (mitigation: it stays in the working tree; nothing we do commits or discards it).
2. **Port 28080 held by open-webui** — EVO primary HTTP cannot bind while it runs; runtime verification of EVO servers must account for this (28280 MCP is free).
3. **No verified build after 07:47** — target/ jars are stale relative to possible last-session edits after 07:47 (docs were written 09:24–09:38; opencode DB corrupted 12:30 Nov 16, recovered). Baseline build required.
4. **Corrected MCE errors** — EPYC corrected errors are benign but logged continuously.
5. **SQLite user_version = 0** — no formal migration versioning exists yet; additive IF NOT EXISTS migrations mitigate.
6. **admin/admin + password123 defaults** — pre-existing hardening risk (documented in COGNITIVE-PLATFORM audit).

## 10. Required recovery actions

1. Re-run a baseline Maven/Tycho build of servers + controller bundles to confirm the uncommitted Phase 1 code compiles after the crash. — IN PROGRESS (Phase 0 step)
2. Create verified DB backups before any migration work. — PENDING (before Phase 2 migration test)
3. Keep the running open-webui untouched; do not rely on port 28080 for EVO runtime tests (use 28280 MCP). — ACTIVE

## 11. Implementation readiness

**READY — WITH WARNINGS.**
- Recovery gate satisfied: server stable, repositories accessible and conflict-free, all databases readable, existing uncommitted work preserved, OpenCode recovery complete.
- Baseline build verification is the only remaining gate action; it does not block reading/analysis work.

## 12. Repository/database classification summary

| Asset | Classification |
|---|---|
| Server (petr-rome2-epyc) | HEALTHY |
| evolution-121126 | HEALTHY WITH WARNINGS (105 uncommitted files — intended work) |
| DCoder-121126 | HEALTHY |
| tryst-121126 | HEALTHY WITH WARNINGS (69 uncommitted files — intended work) |
| DStudio-121126 | HEALTHY |
| evo-native-runtime-161126 | HEALTHY |
| data/evolution.db | HEALTHY |
| controller.tests/data/evolution.db | HEALTHY |
| tryst.db / dstudio.db / dcode.db | HEALTHY |
| OpenCode storage | HEALTHY (recovery completed; artifacts preserved) |

## 13. Final recovery status (end of implementation)

- Baseline build verified after the crash: servers, controller, enr, view and
  the full code reactor build clean (Java 21 / Tycho).
- All databases re-checked after test runs: integrity ok.
- Backups remain at `backups/2026-11-17-cognitive-baseline/` (verified
  readable at creation).
- No recovery artifact was modified or deleted during implementation;
  OpenCode recovery state untouched.
- The only environmental limitation found: the offline p2 cache lacks
  Windows-specific artifacts, so the win32 product materialization step of
  `eu.kalafatic.evolution.repository` fails offline (unrelated to the crash
  and to cognitive changes).

## 14. Final phase update (integration + runtime verification)

- Workspace baseline re-recorded before new changes
  (FINAL-WORKSPACE-BASELINE.md): all five repos, same branches/HEADs, no
  conflicts, previously preserved uncommitted work intact.
- Product materialization performed ONLINE (p2 repositories reachable):
  linux, win32 and headless products built successfully; the offline win32
  limitation from §13 is resolved via the online build.
- EVO started through its existing RCP product launcher (DISPLAY :1);
  startup log verified: cognitive tools/resources wired on MCP 28280,
  secondary 28180 up, supervisor launched; the 28080 external-process
  conflict is detected and handled by the existing PortAvailabilityChecker.
- Live verification totals: MCP runtime matrix 41/41, cross-application
  E2E 20/20, restart persistence checks, live TRYST/DStudio client suites.
- EVO left RUNNING (product instance, PID in `ps aux | grep equinox
  launcher`) with the verification fixtures (test applications/tokens
  seeded in data/evolution.db) for user inspection.
- New DB backup note: an additional backup of data/evolution.db is
  recommended after this session's runtime verification writes; the
  original baseline backups remain untouched.
