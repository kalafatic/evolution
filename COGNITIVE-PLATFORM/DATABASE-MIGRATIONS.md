# DATABASE MIGRATIONS
Cognitive Services persistence — actual schema and migration strategy

## 1. Database

- Single SQLite database: `data/evolution.db` (existing `DatabaseManager`,
  sqlite-jdbc 3.45.2.0, WAL mode, busy_timeout 30000).
- No second database, no new persistence framework.
- Large text payloads live in JSONL/JSON files under the workspace
  (`shared/memory/`, `shared/conversations/`, `shared/sessions/`, `shared/contexts/`).

## 2. Migration mechanism

All cognitive tables are created additively in `DatabaseManager.initializeDatabase()`
with `CREATE TABLE IF NOT EXISTS` + `CREATE INDEX IF NOT EXISTS`. Migrations are:

- **Idempotent**: rerunning initialization never fails or duplicates.
- **Non-destructive**: no DROP, no destructive ALTER on existing tables.
- **Backward compatible**: existing tables (`users`, `sessions`, `roles`,
  `permissions`, `audit_log`, TEIP tables) are untouched.

Backup: `/home/petr/git/evolution-121126/backups/2026-11-17-cognitive-baseline/`
(`evolution.db` + tests `evolution.db`, integrity-checked at creation).

Rollback: the new tables can be dropped individually without affecting
existing functionality; no data from existing tables is migrated or moved.

## 3. Tables (all CREATE TABLE IF NOT EXISTS)

| Table | Purpose | Key columns |
|---|---|---|
| applications | ApplicationIdentity registry | id, application_id (unique), name, kind, owner_user_id, enabled |
| application_tokens | Issued access grants | id, application_id, user_id, token_hash (SHA-256), scopes_json, revoked, expires_at, last_used_at |
| validation_policy_config | Per-rule mode/enable state + counters | rule_id, category, mode, enabled, warnings, errors, last_result, last_check_at |
| validation_policy_history | Configuration change audit | timestamp, rule_id, category, old_mode, new_mode, changed_by, change_reason |
| memory_index | Memory search mirror | entry_id (PK), content, scope, type, project_ref, user_ref, application_ref, confidence, validity, updated_at |
| memory_fts | FTS5 external-content index over memory_index | content |
| cognitive_sessions | Durable session registry | id (PK, UUID), legacy_alias, kind, owner_user_id, application_id, project_ref, status, started_at, last_activity_at, ended_at, parent_session_id, metadata |
| session_relationships | Session lineage | from_session_id, to_session_id, kind, created_at |
| session_snapshots | Snapshot manifest index | id, session_id, kind, schema_version, ref (manifest file), created_at, created_by |
| conversations | Conversation headers | id, session_id, title, created_at, file_ref |
| conversation_messages | Searchable transcript index | id, conversation_id, sequence, role, agent_type, tool_name, content, tokens, timestamp, metadata |
| conversation_messages_fts | FTS5 external-content index | content |
| user_profiles | Profile data | user_id, display_name, locale, timezone, hints_json |
| user_preferences | Typed preferences | user_id, scope, project_ref, key, value_json, source (EXPLICIT/INFERRED/TEMPORARY), confidence, status, expires_at |
| project_knowledge | Project header | project_key (unique, path hash), project_name, root_path, last_scanned_at |
| project_components | Components | project_knowledge_id, component_path, component_type, summary, tags_json, criticality |
| architectural_decisions | ADRs | project_knowledge_id, decision_id, title, context, decision, rationale, status, superseded_by, decided_at |
| known_issues | Known issues | project_knowledge_id, title, description, severity, status, workaround |
| implementation_history | Change history | project_knowledge_id, summary, change_summary, commit_ref, session_id |
| build_results / test_results | Build/test archive | project_knowledge_id, success, pass_rate, summary, refs, timestamp |
| repository_snapshots | Snapshot refs | project_knowledge_id, snapshot_ref, root_path, file_count |
| context_snapshots | Context audit refs | context_id, schema_version, package_ref, requested_by |

## 4. File layout (workspace-relative, existing conventions)

```
${WORKSPACE}/shared/
  memory/user_memory.json             extended entries (backward compatible)
  memory/versions/<entryId>.jsonl     append-only version history
  conversations/<conversationId>.jsonl  append-only transcripts
  sessions/<sessionId>/snapshot_<id>.json  layered manifests
```

## 5. Verification performed (2026-11-17)

- Backup created and integrity-checked (`PRAGMA integrity_check` = ok).
- Test JVM against the tests-bundle `data/evolution.db`: all new tables created
  idempotently; 39 cognitive tests green (see TEST-RESULTS.md).
- Production `data/evolution.db` integrity re-checked after test runs: ok.

## 6. Rollback limitations

- SQLite `DROP TABLE` on the new tables fully rolls back the schema; JSONL/JSON
  files under `shared/` are additive and can be removed per directory.
- No data was migrated from legacy stores (loads are backward compatible);
  therefore no data-loss rollback scenario exists.
