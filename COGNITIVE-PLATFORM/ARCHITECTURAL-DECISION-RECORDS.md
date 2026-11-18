# ARCHITECTURAL DECISION RECORDS

## ADR-001 — Cognitive data outside the EMF model
**Status**: ACCEPTED.
**Context**: EMF `Orchestrator` (XMI) already holds ChatSession/ChatMessage and config. Adding high-volume cognitive entities (memory, transcripts, build results) would bloat the editor resource and couple cognitive writes to editor save cycles (verified: headless sessions never persisted today).
**Decision**: EMF remains configuration/editor model. Cognitive entities live in SQLite (existing `DatabaseManager`) + JSONL files under workspace paths; ContextPackage is a plain DTO with a schema version.
**Consequences**: no EMF regeneration; headless persistence possible; UI adapters read the same services.

## ADR-002 — One memory authority (MemoryService), not three
**Context**: Three memory stores exist (MemoryService JSON; IterationMemoryService per-project JSON; SemanticWorkspace in-memory) plus EMF sharedMemory string.
**Decision**: Extend `MemoryService` as the platform authority (additive fields, events, FTS mirror, versions). IterationMemoryService/SemanticWorkspace remain session-scoped special-purpose stores that *feed* MemoryService via consolidation — never compete with it.
**Consequences**: single query surface; existing readers unchanged; consolidation pipeline is the only new writer path.

## ADR-003 — SQLite is sufficient; no new database
**Context**: Platform already runs SQLite (users/roles/audit/TEIP) with WAL. Cognitive workload is local-first, few concurrent apps, modest volume.
**Decision**: Reuse `data/evolution.db` with additive tables + FTS5; large text in JSONL files. Re-evaluate only if multi-node or >10 GB index requirements appear.
**Consequences**: zero new dependencies; single-writer limits documented in risks.

## ADR-004 — MCP = external contract; native services = implementation
**Context**: Existing MCP has three wirings; the authoritative tools live on the 28080 UI dispatcher; 28280 is a demo server.
**Decision**: Cognitive tools (`CognitiveMcpToolProvider`) are thin adapters over the native services; they are registered on the 28280 `McpDemoServerManager` path with authenticator+authorization wired; 28080 UI path also gets them (shares provider). Internal EVO code never calls MCP for itself.
**Consequences**: one implementation, two interfaces; demo docs resources preserved.

## ADR-005 — Bearer tokens; params never carry identity
**Context**: Current MCP trusts an unverified `x-evo-user` header (defaults "admin").
**Decision**: Cognitive MCP requires Bearer (user session token or application token). `userId`/`applicationId`/`projectId` in tool params are hints validated against the token principal. Scopes enforced at service layer.
**Consequences**: spoofing impossible; admin-by-default demo path closed for cognitive tools.

## ADR-006 — Session continuity = registry + manifests, not full-state serialization
**Context**: Kernel checkpoints exist (`checkpoint_<sessionId>.json`); full SessionContext serialization would duplicate bus/registry state and drift.
**Decision**: `cognitive_sessions` registry + layered snapshot manifests (conversation/context/kernel refs). Kernel restore reuses the existing `IterationManager` restart-continuity path verbatim.
**Consequences**: no new checkpoint format; restore limited to the three layers (documented).

## ADR-007 — Context quality is measured, budget is token-based
**Context**: Budget units are inconsistent today (chars in AttachmentInjector, bytes/4 in ContextCurator, 32k fixed in curation).
**Decision**: `TokenEstimationService` (BPE when vocab exists, char/4 fallback) is the single estimator; ContextPackage always carries measured `tokenEstimate` and `ContextAssessment`.
**Consequences**: one call-site swap in ContextCurator; behavior preserved when store empty.

## ADR-008 — Embeddings deferred to ENR (Phase 2), FTS first
**Context**: No embedding/vector infrastructure exists anywhere in the family. Phase 1 needs working retrieval immediately.
**Decision**: Exact lookup + SQLite FTS5 + relationship traversal + temporal ranking for Phase 1; semantic search arrives as additive ENR `/enr/v1/embeddings` capability in Phase 2, with vectors stored in `memory_index`.
**Consequences**: retrieval quality is keyword-limited in Phase 1 (documented risk); architecture does not change later.

## ADR-009 — ENR stays stateless; metadata-only extension
**Context**: ENR 1.0 spec §8: stateless per request. DCode reads contextLength/usage fields ENR never sends (latent interop gap).
**Decision**: Add per-response usage fields and per-model contextLength (+known flag) — pure metadata, no storage. Embeddings endpoint is per-request stateless too.
**Consequences**: DCode budget chain becomes real; ENR ownership model unchanged.

## ADR-010 — Explicit beats inferred, always
**Context**: Preference precedence must be deterministic and safe.
**Decision**: EXPLICIT > PROJECT-scoped > INFERRED; inferred writes rejected when explicit exists; inferred rendered as "observed behavior" section in context; corrections downgrade confidence.
**Consequences**: no silent preference overrides (enforced by tests).

## ADR-011 — Retract, never silently delete
**Context**: Memory correctness + auditability requirements.
**Decision**: `memory_retract` sets validity=RETRACTED + version record; hard delete admin-only. Derived entries flagged for re-verification.
**Consequences**: full audit lineage; retention sweeps only archive old episodes into summaries.

## ADR-012 — No new MCP server, no new port
**Context**: Constraint 2.3: extend existing MCP on 28280.
**Decision**: All cognitive tools/resources ride the existing server/dispatcher/registries; `PortRegistry.EVO_MCP` unchanged.
**Consequences**: none for external consumers; internal cleanup of the dead servers-bundle `startMcpServer` paths is optional and out of scope.

## ADR-013 — TRYST/DStudio consume via MCP wire only
**Context**: Constraint 2.5: no EVO-internal class dependencies in sibling apps.
**Decision**: `EvoMcpClient` modules in TRYST/DStudio; JSON-RPC over HTTP with bearer; correlation columns in their local DBs; graceful degradation when EVO absent.
**Consequences**: sibling repos stay independently buildable; DStudio per-user identity deferred (single app token in Phase 1).
