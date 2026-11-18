# KNOWN LIMITATIONS (final)
Cognitive Services — first generation, post-verification state.

## Resolved in this phase (were limitations, now closed)

1. Darwin ContextBuilder hook — IMPLEMENTED (CognitiveContextBridge +
   ContextBuilder step 5c, 8 tests).
2. Port 28080 conflict — INVESTIGATED AND HANDLED: held by an unrelated
   external process (open-webui, PID 3763, root). Not killed, not
   reconfigured. EVO's existing PortAvailabilityChecker detects it and
   continues startup; MCP (28280), secondary (28180) and supervisor start
   normally. Deterministic ports preserved.
3. Live runtime verification — PERFORMED: EVO RCP product started (X
   display), MCP matrix 41/41, cross-app E2E 20/20, restart persistence.
4. Direct tests for PersonalizationService, ProjectKnowledgeService,
   MemoryConsolidationService, KnowledgeExtractionService — ADDED
   (CognitivePlatformServicesTest 9/9).
5. ENR/ENR-test and DStudio-test record-constructor breakage — FIXED
   (test-code updates).
6. Live TRYST/DStudio client verification — ADDED (4/4 and 3/3 against
   running EVO).

## Remaining limitations

### Functional
1. Search is FTS5/LIKE, not semantic (embeddings deferred to Phase 2 by
   design).
2. Consolidation/extraction are deterministic, not LLM-based.
3. Session restore covers conversation/context/kernel layers; UI state out
   of scope.
4. DStudio per-user identity deferred (application token model).
5. Retention sweeps (conversations, context snapshots) not implemented;
   stores are append-only with compaction summaries.
6. The three legacy memory stores are not auto-migrated (backward-compatible
   load; no data moved). ECOSYSTEM-scope public-knowledge tier defined but
   unpopulated.

### Verification
7. Admin UI click-through not automated (no xdotool/screenshot tooling);
   page services verified live via the same code paths.
8. No formal soak/connection-leak profiling run.
9. Live ENR server process not exercised (ENR bundle lazy activation in the
   product); ENR contract verified by wire-level tests.
10. Supervisor (28480) has no authentication — pre-existing, out of scope.
11. `admin/admin` + `password123` defaults remain — pre-existing hardening
    item; external apps use the token path.

### Environmental
12. DStudio's TeipOpenCodeEndToEndTest fails when the external OpenCode
    agent cannot perform a real edit (no working LLM in its sandbox) —
    environment-dependent, unrelated to cognitive changes.
13. Pre-existing red EVO tests (documented in PHASE-6/7 reports) unchanged.
14. The win32/linux product materialization requires network access to the
    p2 repositories (now cached locally; offline source compilation is
    unaffected).

## Deliberately not implemented (spec 20)

Graph database, complex knowledge graph, embedding search, autonomous LLM
memory consolidation, predictive personalization, autonomous preference
modification, multi-agent memory, new orchestration framework.
