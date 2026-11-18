# CONTEXT RECONSTRUCTION DESIGN

The engine that produces a ContextPackage before inference. It reuses the existing context producers (`ContextBuilder`, `ContextCurator`, `ContextResolver`, `AttachmentInjector`, `MemoryContextProvider`, `ConversationState`) and adds the missing stages: assessment, conflict handling, unified budget, provenance.

## 1. Pipeline (ContextReconstructionService)

```
1  REQUEST CLASSIFICATION   → RequestClassifier (reuses ModeRecognizer/PromptIntentAnalyzer intent output; falls back to keyword classification)
2  INTENT ANALYSIS          → existing intent stack (CognitiveAnalysisPipeline, HybridAtomicIntentClassifier, ContextAssistant) — cached result honored
3  USER CONTEXT             → UserContextService (identity + preferences + app access); scope-filtered
4  SESSION RESOLUTION       → SessionContinuityService (existing session or create/relate); continuation state from last snapshot
5  PROJECT IDENTIFICATION   → explicit projectRef → else session.project → else TaskContext.projectRoot → else working-dir heuristic (reuse IterationMemoryService.findEffectiveRoot logic)
6  EPISODE RETRIEVAL        → ConversationHistoryService recent messages + compaction summary if budget-tight
7  MEMORY RETRIEVAL         → MemoryService.retrieveRelevant(MemoryQuery) filtered by scope (USER/PROJECT/SESSION/TASK) + validity=ACTIVE/VALIDATED
8  DECISION RETRIEVAL       → ProjectKnowledgeService.searchDecisions (status ACCEPTED + supersession chain; SUPERSEDED included only when explaining)
9  ISSUE/PROCEDURE RETRIEVAL→ known issues (OPEN/MITIGATED) + procedural entries ranked by strategy reliability
10 PROJECT STATE VERIFY     → ProjectKnowledgeService.getState (last build/test, open issues, last scan)
11 REPOSITORY INTELLIGENCE  → ProjectKnowledgeService components + (optionally) fresh ContextCurator selection for code context
12 CONFLICT DETECTION       → MemoryRelationship CONTRADICTS + preference contradictions + decision supersession
13 FRESHNESS ASSESSMENT     → per-item: lastVerifiedAt/updatedAt vs timeWindow + project build/test recency
14 RELEVANCE RANKING        → scorer: textual match × type weight × confidence × freshness × scope proximity
15 DEDUPLICATION             → hash-normalized content + SUPERSEDES relationship pruning
16 BUDGET OPTIMIZATION      → TokenEstimationService; per-category quotas; trim lowest-relevance first; compact history if needed
17 PACKAGE ASSEMBLY         → ContextPackage + ContextAssessment (ContextQualityService)
```

## 2. Retrieval combination

| Method | Used for | Implementation |
|---|---|---|
| Exact search | sessionId/project/decisionId lookup | SQLite PK/index lookups |
| Full-text search | memory/conversation/decisions | SQLite FTS5 (`conversation_messages`, `memory_index`, `architectural_decisions`) |
| Semantic search | Phase 2+ (memory) | embeddings via ENR `/enr/v1/embeddings` (new additive endpoint); store vectors in `memory_index` — **NOT required for Phase 1** |
| Relationship traversal | supersession, contradictions, session lineage | `session_relationships`, `MemoryEntry.relationships[]` |
| Temporal relevance | recency | timestamps + exponential decay weight (reuse SemanticWorkspace 0.95-decay idea as scoring input) |
| Project filtering | scope isolation | projectRef equality + scope enum checks |
| Confidence | ranking | MemoryEntry.confidence, ArchitecturalDecision status |
| Freshness | staleness marking | lastVerifiedAt vs staleness thresholds per type |

## 3. Provenance classes (must be distinguishable in output)

- **CONFIRMED_FACT** — type FACT + source USER/TOOL/BUILD/TEST + confidence ≥ 0.8 + recently verified.
- **HISTORICAL_FACT** — same, but older than freshness window → lower rank, flagged in freshness.
- **INFERRED_PREFERENCE** — type PREFERENCE + source INFERENCE/CONSOLIDATION → never overrides explicit; rendered as "observed behavior" text.
- **UNVERIFIED_ASSUMPTION** — confidence < threshold or source=DREAM_UNVERIFIED (existing `DreamEngine` metadata pattern) → rendered with caveat prefix, excluded from "facts" section.
- **SUPERSEDED_DECISION** — status SUPERSEDED → included only with its successor and a `supersededBy` pointer.

## 4. Conflict and staleness handling

- Contradicting memories (relationship CONTRADICTS): both surfaced in `warnings[]`; the one with higher confidence+newer lastVerifiedAt is ranked first; if equal → `missingInfo` asks for user resolution; never silently drops.
- Explicit vs inferred preference conflict → explicit wins; conflict noted in `warnings` (see PERSONALIZATION-AND-LEARNING.md).
- Stale entries (beyond per-type TTL without re-verification): downranked, marked `stale: true`, NOT auto-deleted. TTL defaults: FACT 90 d, PROJECT_CONTEXT 30 d, PROCEDURE 180 d, PREFERENCE 365 d — configurable via EMF `ServerSettings` addition or config file (keep in existing config architecture).
- Decisions: only ACCEPTED enter the active context; SUPERSEDED appear only via chain traversal when the query mentions the old decision.

## 5. Budget optimization

- Single unit: tokens via `TokenEstimationService` (BPE when trained vocab available, else char/4 — replaces `ContextCurator.estimateTokens` heuristic at that call site).
- Default quotas (of `budgetTokens`): system/architecture constraints 10%, user context 10%, session/history 25%, project state 10%, memories 20%, decisions/issues 10%, code/attachments 15% — all tunable.
- Trim order: lowest relevance score first; then oldest; then lowest confidence. History compaction: `ConversationHistoryService.compact()` produces a bounded summary (LLM when available via existing `AiService`, else extractive head-lines) — reused for DCode-style 40-message windows on demand.
- Final `tokenEstimate` is measured, not guessed; over-budget result is never returned silently — it carries a warning and the trimmed list.

## 6. Data flow diagram

```text
ContextRequest ──► Classify ──► Intent ──► Session ──► Project
                                                  │
        ┌───────────────┬─────────────┬───────────┴──────┬─────────────┐
        ▼               ▼             ▼                  ▼             ▼
    UserContext   Conversation    MemoryService    ProjectKnowledge  Repository
    (profile,     HistoryService  (episodes,      (decisions,       Intelligence
     prefs)       (recent+summary) facts, procs)   issues, state)   (components,
                                                                     curated code)
        └───────────────┴─────────────┴──────────┬───────┴─────────────┘
                                                 ▼
                                     Conflict + Freshness + Rank + Dedup
                                                 ▼
                                      Budget optimization (tokens)
                                                 ▼
                                   ContextPackage assembly
                                                 ▼
                                   ContextQualityService.assess
                                                 ▼
                            ContextPackage + ContextAssessment
```

## 7. Integration with Darwin (native path)

- Additive: `ContextBuilder.build()` (used by Darwin modes) gains an optional call to `CognitiveContextService.getContext(...)` producing the new ContextPackage section (memoryContext extended, decisions/issues/procedures appended). Behavior preserved when the cognitive store is empty (same output as today).
- The mediation path (`ContextCurator` + `PromptSynthesizer`) remains the code-context authority; the reconstruction engine calls it as the "repository intelligence" source adapter.
- No change to `IterationManager` authority: context reconstruction is read-only w.r.t. iteration state.

## 8. Performance targets (to be measured, not assumed)

| Operation | Target | Measurement |
|---|---|---|
| context_get (warm, empty history) | < 200 ms p95 | JUnit microbench + latency log in service |
| context_get with 10k-message history | < 1 s p95 (FTS + windowing) | synthetic history fixture |
| memory_search FTS | < 50 ms p95 | fixture with 10k entries |
| consolidation job (background) | < 30 s per conversation | async executor, no user wait |
| MCP tool call overhead (dispatch+authz) | < 5 ms | dispatcher test |

No benchmark numbers are claimed until tests run.
