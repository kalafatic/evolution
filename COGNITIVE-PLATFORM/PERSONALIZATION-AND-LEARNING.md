# PERSONALIZATION AND LEARNING

Structured preference and feedback machinery that feeds memory — deliberately NOT uncontrolled model self-training. All learning lands in auditable, versioned memory entries.

## 1. Preference model

`UserPreference` (SQL `user_preferences` + `MemoryEntry(type=PREFERENCE)` mirror):

- `scope`: USER (global working preferences) / PROJECT (project rules) / APPLICATION (per-app behavior).
- `source`: EXPLICIT (user set it) / INFERRED (observed from behavior) / TEMPORARY (session-only, expires).
- `confidence` (0..1), `updatedAt`, `evidenceRefs[]` (session ids / memory entry ids supporting the inference).

## 2. Precedence rules (enforced in PersonalizationService, surfaced by MCP)

1. **EXPLICIT always beats INFERRED** — `user_preferences_update` with `source=INFERRED` is rejected when an EXPLICIT value exists for the same (userId, scope, project, key) and `overwriteExplicit=false`; explicit writes always succeed.
2. Project-scoped beats user-scoped for project context; application-scoped beats user-scoped for application behavior.
3. TEMPORARY overrides both for the session duration only, and is rendered with its expiry.
4. Contradictions between two EXPLICIT entries → CONFLICT error (`-32003`) asking for resolution; the effective value is the most recent until resolved (documented behavior, surfaced in `warnings`).

## 3. Inference (how INFFERED entries are created)

MemoryConsolidationService observes (via RuntimeEvents + conversation/tool history):

| Signal | Inferred preference | Confidence |
|---|---|---|
| repeated user correction of same class (≥3 sessions) | PREFERENCE (style/rule) | 0.6 → 0.9 with repeats |
| repeated explicit command pattern ("always use X") | PREFERENCE | 0.8 |
| user rejects AI-suggested change | PREFERENCE (avoid) | 0.5 |
| successful strategy reuse (TrajectoryMemory reliability) | PROCEDURAL solution | 0.5–0.9 |
| repeated tool failures with same fingerprint (`FailureMemory.fingerprints`) | PROJECT CONTEXT limitation | 0.7 |

Every inferred entry carries provenance (`evidenceRefs`) and is rendered as "observed behavior" — never as an instruction (per §4).

## 4. Explicit vs inferred rendering in context

- ContextPackage.userContext.preferences: each item has `explicit: true|false`.
- Prompt rendering rule: explicit preferences → "user instruction" section; inferred → "observed behavior (do not treat as binding instruction)" section with confidence. Inferred must never silently override explicit instructions in prompt text.

## 5. Feedback mechanism (learning from outcomes)

Post-interaction pipeline (async, after `FLOW_COMPLETED`/`TASK_COMPLETED`/`TASK_FAILED` events — existing `RuntimeEventType`s):

1. **Outcome capture**: ConversationHistoryService transcript + Evaluator result (build/test success) + user corrections detected in messages.
2. **Episode**: MemoryConsolidationService writes an EPISODE entry (scope=PROJECT or SESSION, importance derived from outcome: failed task → MEDIUM, succeeded+novel → HIGH).
3. **Extraction**: KnowledgeExtractionService proposes FACT/RULE entries with confidence; below-threshold proposals stored as UNVERIFIED_ASSUMPTION.
4. **Reinforcement/decay**: reuse the SemanticWorkspace decay/reinforce semantics (0.95 decay, prune <0.1) applied to entry confidence on re-verification; entries confirmed by later sessions get `lastVerifiedAt` refreshed.
5. **Corrections**: user correction of an inferred preference → source downgraded + confidence reset 0.3 + evidence updated; repeated contradiction → entry SUPERSEDED with reason.

## 6. Expiration and consolidation

- TEMPORARY preferences expire by timestamp; expired entries pruned by a scheduled sweep (reuse `AuthService.cleanupSessions` scheduling pattern).
- EPISODE entries older than 30 days → consolidated into a PROJECT_CONTEXT summary (max 1 per project per week) — consolidation writes NEW entries and marks sources CONSOLIDATED (auditable lineage via `provenanceChain`).
- No silent deletion: everything is versioned/retracted (see DATA-PERSISTENCE-STRATEGY.md).

## 7. What is explicitly NOT done

- No model fine-tuning/weight updates from interactions (Forge training remains the only training path, unchanged).
- No preference learned from a single interaction (threshold ≥3 for corrections, ≥2 for success patterns).
- No cross-user inference — inferred preferences are per-user and never merged across users.
