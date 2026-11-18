# RISKS AND OPEN QUESTIONS

## Risks

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Memory JSON file grows unbounded; naive JSON-array rewrite per save degrades | Medium | Medium | FTS index does reads; content >8 KB goes to version JSONL + pointer; sweep policy; format stays array for compat |
| R2 | TokenEstimationService accuracy without trained BPE vocab (char/4 fallback ~±25%) | High | Low-Med | Budgets get 15% headroom; measured `tokenEstimate` always reported; ENR usage counters correct budgets over time |
| R3 | Consolidation writes low-quality entries (LLM hallucination into memory) | Medium | High | confidence thresholds; provenance classes; UNVERIFIED rendering section; deterministic fallback; retract path |
| R4 | Prompt-injection via consolidated tool output entering later prompts as instruction-like content | Medium | High | source=CONSOLIDATION/TOOL rendered only in "observations" section; sensitive flag; validators |
| R5 | 28280 wiring changes break existing demo UI on 28080 | Low | Medium | separate registries per server already; cognitive provider additive; demo resources untouched |
| R6 | SQLite single-writer contention with concurrent MCP apps | Low | Medium | WAL + busy_timeout already set; write serialization; measured before scaling decisions |
| R7 | Session restore expectations exceed the three layers (UI/selection state lost) | High (expectation) | Low | Documented layer scope; FULL manifest is honest about what it references |
| R8 | Cross-repo contract drift (ContextPackage 1.0 changes) | Medium | High | schemaVersion in payload; golden JSON fixtures in all repos; MCP contract tests |
| R9 | Existing headless/remote paths never persist conversations — new store introduces double-write inconsistency with EMF ChatSession | Medium | Medium | ConversationHistoryService is the durable mirror; EMF remains UI-interactive model; reconciliation = store wins on read |
| R10 | DStudio has no user identity — Phase 1 app-token model limits per-user isolation | Certain (design) | Medium | Single-operator deployments fine; per-user model is a V3+ roadmap item (open question Q4) |
| R11 | Hard-coded `admin/admin` seed and `password123` default remain exploitable until changed | Certain | High | First-run change prompt + token-only path for external apps (WP9); document as hardening milestone |
| R12 | Deferred semantic search limits retrieval quality in Phase 1 | Certain (design) | Medium | FTS + relationships cover most project workflows; ENR embeddings unblock Phase 2 without architecture change |
| R13 | TEIP static token vs new app tokens — two token systems coexist | Certain (fact) | Low | TEIP unchanged (normative protocol); cognitive MCP uses the new tokens; documented separation |

## Open questions

| # | Question | Where to resolve | Default if unresolved |
|---|---|---|---|
| Q1 | Should conversation JSONL files move under `data/` (workspace-independent) instead of `${WORKSPACE}/shared/`? | WP1 review with ResourceManager path owners | Keep `${WORKSPACE}/shared/` (matches MemoryService) |
| Q2 | Should the 28080 UI dispatcher also enforce the new authenticator, or keep SWT loopback bypass? | WP4 | Keep bypass for non-cognitive tools only |
| Q3 | Which models are available for consolidation summaries (LlmRouter local/Ollama vs ENR)? | WP6 | Use existing `AiService`/LlmRouter; deterministic fallback always |
| Q4 | When does DStudio adopt per-user identity (users table, login)? | DStudio roadmap (out of this scope) | Phase 1 app token + operator hint |
| Q5 | Retention TTL defaults (FACT 90 d etc.) — who configures them? | WP1 config design | EMF ServerSettings addition or config file; defaults as documented |
| Q6 | Should `context_get` include raw code sections by default or only on request (categories)? | WP3 | Categories opt-in; code via existing ContextCurator when `repository` category requested |
| Q7 | Is FTS5 available in the bundled sqlite-jdbc version (3.45.2.0)? | WP1 verification (compile-time capability probe) | Verified: FTS5 included in sqlite-jdbc 3.45.2.0 artifacts; fallback to LIKE-based search if a platform build strips it |
| Q8 | Which existing MCP client (if any) should TRYST/DStudio share, or do both write their own? | WP8 | Small per-repo clients (wire-only, no shared repo dependency) |
| Q9 | Should kernel checkpoints be copied into session snapshot dirs for FULL snapshots? | WP5 | No — manifests reference existing checkpoint paths (ADR-006) |
| Q10 | Supervisor (28480, no auth) — out of scope for cognitive services? | Security review | Out of scope; documented as pre-existing risk in SECURITY-AND-AUTHORIZATION.md |

## UNKNOWNs (explicit)

- Actual performance numbers: none measured yet — targets in CONTEXT-RECONSTRUCTION-DESIGN.md §8 are goals, not results.
- Whether `IntrospectionEngine`/`ReplayEngine` (docs/COGNITIVE_TRACING_ARCHITECTURE.md) exist beyond documentation — verified absent in `orchestration/diagnostics/`; treated as not implemented.
- Exact BPE vocab availability at runtime for arbitrary models — depends on Forge artifacts present; fallback defined.
- GGUF/`.evo` context-length metadata reliability across all artifacts — flagged per-model (`contextLengthKnown`).
