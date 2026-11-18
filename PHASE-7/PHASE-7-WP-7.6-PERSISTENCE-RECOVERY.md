# PHASE-7-WP-7.6-PERSISTENCE-RECOVERY.md

Phase 7 — WP-7.6 Session, Persistence and Recovery
Date: 2026-11-17
Status: COMPLETE

## 1. State ownership (authoritative, unchanged from Phases 1–6)

| State | Owner | Storage |
|---|---|---|
| EVO Darwin sessions | EVO `SessionManager` | in-memory + EMF configuration; harness selection in `data/connected-applications.json` |
| TRYST tasks/sessions/reviews | TRYST | SQLite `~/.tryst/tryst.db` (V1–V20, WAL) |
| DStudio executions/reviews/harness & engine config | DStudio | SQLite `~/.dstudio/dstudio.db` (V2) |
| DCode workspaces/sessions/messages/events/reviews | DCode | SQLite `~/.dcode/dcode.db` (V1, WAL) + `dcode-config.json` + AES-GCM `secrets.enc` |
| ENR inference runtime state | ENR | in-memory (models, sessions, KV caches); stateless per request by contract |
| Model configuration | ENR | `enr.yaml` / `RuntimeConfig` |

No competing stores were introduced anywhere (WP-7.1 audit).

## 2. Verified behaviors

| Test | Result | Evidence |
|---|---|---|
| Normal shutdown | PASS | all products stop services cleanly (lifecycle tests green: DCode `DefaultDCodeLifecycle`, TRYST `ApplicationLifecycleCoordinator` disposal, DStudio `headlessLifecycleStartsServerAndStopsCleanly`) |
| Unexpected termination | PASS | DCode `SessionRecoveryService`: RUNNING/WAITING_FOR_APPROVAL sessions marked FAILED on restart with diagnostic (test `recoveryMarksInterruptedSessionsFailed` green); never blindly re-run |
| Restart | PASS | DCode `sessionsAndMessagesSurviveRestart` + `migrationIsIdempotentAndVersioned` green; TRYST forward-only migrations V1–V20; DStudio V2 |
| Interrupted execution | PASS | TRYST `TaskRecoveryService` checkpoints per stage + `RECOVERY_REQUIRED` handling (suite green) |
| Runtime failure | **PASS (live)** | ENR killed while DCode serving: DCode stayed UP; a new session failed gracefully (state FAILED, no hang); after ENR restart the next session executed normally (ENR metrics incremented) — ENR restart does not corrupt DCode sessions |
| Database failure | PASS | SQLite WAL + busy timeout + foreign keys in DCode/TRYST; migration errors abort transactions (forward-only, versioned) |
| Service disconnection | PASS | TRYST task with unreachable DCode completed without corruption (empty review, task lifecycle intact); adapters report status failures |
| Recovery | PASS | see above; TRYST headless re-launch loaded the existing DB: previous tasks visible through `/api/tasks` (live observation during Phase 7) |

## 3. Cross-component guarantees (verified by inspection + live runs)

- Restarting ENR does not corrupt DCode sessions or application task state: ENR holds
  no application state; DCode/TRYST persist their own (live-verified §2).
- Restarting DCode does not corrupt application-owned task state: TRYST tasks live in
  `tryst.db`; the DCode adapter treats a failed DCode session as an execution outcome
  within the TRYST lifecycle (live-observed empty review, `WAITING_FOR_REVIEW` intact).
- Conversation history: DCode `messages` table (session-scoped), TRYST
  `conversation_messages`, DStudio `execution_events` — all per-application.
- Repository state: working trees under each product's workspace root (DCode
  `~/.dcode/workspaces`, TRYST `~/.tryst/workspace/sandboxes/{owner}/{task}`, DStudio
  project dirs). ENR never touches repositories.
- Configuration: persisted per product; DCode secrets AES-GCM encrypted at rest
  (test `secretsEncryptedAtRest` green); TRYST never persists the DCode bearer token
  (V20 migration + adapter `persist()`).
- Model configuration: ENR `enr.yaml`; TRYST/DStudio keep discovery metadata only.

## 4. Defects

None new. Pre-existing red classes in EVO do not affect persistence ownership
(see WP-7.10).
