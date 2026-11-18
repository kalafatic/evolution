# DATA PERSISTENCE STRATEGY

Where every byte of cognitive data lives, using only the existing persistence stack: SQLite (`sqlite-jdbc` via servers `DatabaseManager`), JSON/JSONL files, EMF XMI (unchanged). **No new database technology.**

## 1. Decision record: SQLite vs EMF vs JSON

- **EMF XMI** stays the *configuration/editor* model (`orchestrator.xml`). Extending it for high-volume cognitive data would (a) make every conversation write trigger editor dirty-state churn, (b) create a single-giant-XMI performance problem, (c) break the existing save/load contracts. → Cognitive data is NOT added to the EMF model in Phase 1.
- **SQLite** (`data/evolution.db`, existing `DatabaseManager`, additive migrations) hosts: identity/app tokens, session registry, conversation message index (FTS5), memory index, preferences, project knowledge tables, snapshots index, audit (existing).
- **JSON/JSONL files** (existing `ResourceManager`/workspace paths) host large text payloads: conversation transcripts, memory version history, context snapshots, kernel checkpoints (existing location unchanged).
- Rationale: SQLite gives transactional search/relationships; files keep unbounded text out of the DB (matches existing `IterationMemoryService` pattern); no ORM, no schema framework — plain JDBC repositories like the existing `UserRepository`.

## 2. Table map (new; all additive migrations in `DatabaseManager` style)

| Table | Purpose | Key columns |
|---|---|---|
| `applications` | ApplicationIdentity | id, name, kind, owner_user_id, enabled, created_at |
| `application_tokens` | issued access | id, application_id, user_id, token_hash, scopes_json, revoked, expires_at |
| `cognitive_sessions` | session registry | id, legacy_alias, kind, owner_user_id, application_id, project_ref, status, started_at, last_activity_at, ended_at, parent_session_id, metadata_json |
| `session_relationships` | lineage | id, from_session_id, to_session_id, kind |
| `session_snapshots` | snapshot manifests | id, session_id, kind, schema_version, ref, created_at, created_by |
| `conversation_messages` (+FTS5 `conversation_messages_fts`) | searchable transcript | id, conversation_id, sequence, role, agent_type, tool_name, content, tokens, timestamp |
| `conversations` | conversation header | id, session_id, title, created_at, file_ref |
| `memory_index` (+FTS5) | memory search mirror | entry_id, content, scope, type, project_ref, confidence, updated_at |
| `user_profiles` | profile | user_id, display_name, locale, timezone, hints_json |
| `user_preferences` | preferences | user_id, scope, project_ref, key, value_json, source, confidence, updated_at |
| `project_knowledge` | project header | id, project_key, name, root_path, last_scanned_at |
| `project_components` | components | id, project_knowledge_id, path, type, summary, tags_json, criticality |
| `architectural_decisions` (+FTS5) | ADRs | id, project_knowledge_id, decision_id, title, decision, status, superseded_by |
| `known_issues` | issues | id, project_knowledge_id, title, severity, status, workaround |
| `implementation_history` | change history | id, project_knowledge_id, summary, commit_ref, session_id |
| `build_results` / `test_results` | build/test history | id, project_knowledge_id, success, pass_rate, summary, refs |
| `repository_snapshots` | snapshots | id, project_knowledge_id, snapshot_ref, file_count, created_at |
| `context_snapshots` | context audit | id, context_id, schema_version, package_ref, requested_by |

## 3. File layout (under existing workspace root conventions)

```text
${WORKSPACE}/
  shared/
    memory/
      user_memory.json                 (EXISTING — extended fields, backward compatible)
      versions/<entryId>.jsonl         (MemoryVersion audit)
    conversations/
      <conversationId>.jsonl           (append-only transcript, incl. structured tool records)
    contexts/
      <contextId>.json                 (ContextSnapshot payload)
    sessions/
      <sessionId>/
        manifest.json                  (FULL snapshot manifest referencing checkpoints)
  (existing) orchestrator/memory/checkpoint_<sessionId>.json  — UNCHANGED location/format
```

Path resolution: through the existing `ResourceManager`/`ProjectModelManager.getWorkspacePath()` chain (same as `MemoryService.initStoragePath()`), NOT the process working directory.

## 4. Write paths

- Conversation: `ConversationOutputController` (existing buffer flush) → `ConversationHistoryService.append` (JSONL + SQL index in one transaction).
- Memory: `MemoryService.addEntry/updateEntry` (existing) → JSON save + `memory_index` upsert + optional version append. Format stays a JSON array (`array.toString(2)`) for compatibility; large entries (>8 KB content) live only in the version JSONL and index row with a pointer.
- Build/test: `Evaluator` gains one call → `ProjectKnowledgeService.recordBuild/recordTest` (existing EMF EvaluationResult unchanged).
- Genome import: `ProjectKnowledgeService.importGenome(root)` reads existing `genome/current/genome.json` + `functionality_inventory.json` (read-only import; no genome pipeline changes).

## 5. Migration strategy

- SQLite: additive `CREATE TABLE IF NOT EXISTS` + index creation in `DatabaseManager.initializeDatabase()` (existing pattern swallows `ALTER` errors — use explicit `CREATE ... IF NOT EXISTS` instead of ALTER for new tables; ALTER only for new columns on existing `users`).
- `user_memory.json`: on load, entries without new fields get defaults (`version=1`, `validity=VALID`, `relationships=[]`) — old files load unchanged.
- Backward compatibility invariants: existing consumers (`ContextBuilder`, `ContextualHelpService`, `NeuronContextService`, `DreamEngine`, `UserMemoryGroup` UI) compile and behave identically; existing tests (`UserMemorySubsystemTest`, `MemoryMenuIntegrationTest`, `ContextualHelpServiceTest`, `DreamExperienceSelectionAndContextTest`) stay green.
- Rollback: no destructive migrations; new tables can be dropped without affecting existing features.

## 6. Retention

| Data | Policy |
|---|---|
| conversation JSONL | keep (append-only); compaction only summarizes, never truncates the log |
| context snapshots | keep last 20 per session + 1 per completed task (configurable) |
| memory versions | keep all (small) |
| audit_log | existing (no change) |
| checkpoints | keep last 5 per session + latest (align with existing `IterationMemoryService`) |
| build/test results | keep last 500 per project |

Sweeps scheduled like the existing `cleanupSessions` (AuthService) pattern — no new scheduler infrastructure.

## 7. Why no additional database

SQLite already runs the platform's transactional data (users, roles, audit, TEIP persistence); it is single-writer but the EVO cognitive workload is local-first with modest concurrency (a handful of apps, MCP clients). Requirements that would force a change — multi-node writes, >10 GB indexes, horizontal replication — are absent today. This decision is recorded in ARCHITECTURAL-DECISION-RECORDS.md (ADR-004).
