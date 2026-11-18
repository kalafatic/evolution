# EXECUTIVE SUMMARY

**Objective**: make EVO Platform the shared cognitive infrastructure provider (identity, sessions, conversation history, memory, project knowledge, context reconstruction, personalization) for the EVO ecosystem and external MCP-compatible applications — extending the existing architecture, never replacing it.

**Verdict**: **ACCEPT WITH LIMITATIONS** — fully implementable inside the existing EVO Platform with additive changes and high reuse.

## What the audit found (source-verified)

- EVO already owns nearly every building block: SQLite-backed users/roles/permissions/audit (`eu.kalafatic.evolution.servers`), an in-process session architecture (`SessionManager`/`SessionContext`/`IterationManager` with kernel checkpoints), a persistent memory store (`MemoryService` → `user_memory.json`), context pipelines (`ContextBuilder`, `ContextCurator`, `AttachmentInjector`), an MCP server (JSON-RPC 2.0, protocol 2024-11-05, port 28280, real tools via `EvoMcpToolProvider`), and repository intelligence (scanners + genome pipeline).
- But the pieces are fragmented: three memory stores with no events or consolidation, no durable conversation persistence for headless modes, no shareable identity, no MCP authentication, no cognitive MCP tools, and no consumption path in TRYST/DStudio/DCode.
- DCode has the ingestion seams but no external-context contract; ENR is stateless but missing token-usage/context-window metadata; TRYST and DStudio have local identity/persistence and no MCP clients.

## The blueprint (19 documents)

1. **Domain model**: SQLite registry tables + extended `MemoryEntry` (versioning, validity, relationships, provenance) — EMF stays the configuration model (ADR-001).
2. **Services** (one new package in controller + one extension in servers): CognitiveContextService facade over SessionContinuity, ConversationHistory, Memory (existing, extended), ProjectKnowledge, UserContext, Personalization, Consolidation, KnowledgeExtraction, ContextReconstruction, ContextQuality, TokenEstimation.
3. **MCP API**: ~20 tools (`context_get`, `memory_*`, `user_*`, `session_*`, `decision_*`, `project_*`) + `evo://` resources on the existing 28280 server, with bearer authentication and scope enforcement. Params never trusted for identity.
4. **Context reconstruction**: 16-stage pipeline (classify → intent → gather → conflict/freshness → rank/dedup → token budget → assess) combining exact/FTS/relationship/temporal retrieval; embeddings deferred to a stateless ENR endpoint in Phase 2; provenance classes (confirmed/historical/inferred/unverified/superseded) always distinguishable.
5. **Continuity**: session registry + layered snapshot manifests referencing the existing kernel checkpoints; Darwin restore path reused verbatim.
6. **Personalization**: explicit > inferred always; inferred preferences rendered as observations; learning lands in auditable, versioned memory — no uncontrolled model self-training.
7. **Security**: app tokens (hashed), scope model (USER/APPLICATION/PROJECT/SESSION/TASK/ECOSYSTEM), service-layer isolation, full audit, retract-never-delete.
8. **Persistence**: SQLite (existing DatabaseManager, FTS5) + JSONL files — no new database (ADR-003).
9. **Family integration**: TRYST/DStudio/DCode consume via MCP wire-only clients with correlation columns and graceful degradation.

## Roadmap (MVP first)

WP1 domain model/persistence → WP2 native services → WP3 reconstruction engine → WP4 authenticated MCP → (parallel) WP7 ENR metadata + DCode ingest → WP5 continuity hardening → WP6 consolidation/learning → WP8 TRYST/DStudio → WP9 security → WP10 performance/validation.

**MVP deliverable**: an external client obtains a budgeted, assessed ContextPackage and stores memory over authenticated MCP on 28280 — everything else layers on without architectural change.

## Limitations accepted

Semantic search deferred to Phase 2; session restore covers conversation/context/kernel layers only; DStudio per-user identity deferred; performance targets are goals pending measurement; SQLite scaling validated before any migration.
