# MEMORY PERSISTENCE VERIFICATION
Unified MemoryService persistence, verified live and by unit tests.

## 1. Lifecycle (live over MCP)

1. Create record — `memory_store` → entryId (UUID), version 1. PASS
2. Retrieve — `memory_search` finds content. PASS
3. Search — FTS + LIKE fallback verified. PASS
4. Update — version 2, MemoryVersion JSONL appended. PASS
5. Relationship — SUPERSEDES marks the superseded entry, relationship
   recorded (unit test). PASS
6. Conflict detection — CONTRADICTS relationships surfaced in context
   warnings (unit + Darwin tests). PASS
7. Stale marking — OUTDATED/verify transition refreshes freshness (unit). PASS
8. Retract — validity RETRACTED; excluded from search and context
   reconstruction; original record remains persisted (live + unit). PASS

## 2. JSONL + SQLite consistency

- Writes: JSON (`user_memory.json`) first (durable), then `memory_index`
  upsert + FTS rebuild (best effort; failures never break the store).
- Reads: search via SQLite FTS, LIKE fallback, then in-memory scan — three
  independent paths agree in tests.
- Version history: append-only `versions/<entryId>.jsonl`; corrupted lines
  skipped (self-healing per line).
- Restart: EVO restarted after storing records; `memory_search` returned the
  records after restart (live persistence check, 2 records found).
- Interrupted writes: fault tolerance covered by WAL + durable-first write
  ordering; destructive tests run only against isolated test DBs.

## 3. Backward compatibility

- Legacy JSON entries (without unified-model fields) load with safe defaults
  (version 1, VALID, empty relationships) — `CognitiveMemoryServiceTest.
  legacyJsonLoadsWithSafeDefaults` PASS.
- Existing readers (UserMemorySubsystemTest 4/4, MemoryMenuIntegrationTest
  1/1) unaffected.

## 4. Isolation

- USER-scope entry of user A excluded from user B context (unit).
- PROJECT scope: cross-project leakage excluded (unit + live E2E).
- SESSION scope: visible only to the owning session; a defect that leaked
  session-scoped entries to session-less requests was found by the live E2E
  and fixed (`ContextReconstructionService.inScope`), regression test added.

## 5. Test evidence

- CognitiveMemoryServiceTest: 9/9 PASS
- CognitiveRuntimeDefectRegressionTest: 4/4 PASS
- ContextReconstructionTest: 7/7 PASS
- CognitivePlatformServicesTest (consolidation): 9/9 PASS
- Live MCP matrix memory section + cross-app E2E memory steps: PASS
