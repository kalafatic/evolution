# CONTEXT RECONSTRUCTION VERIFICATION
CognitiveContextService facade, verified live and by tests.

## 1. Verified capabilities

- User profile retrieval (live `user_get` returns profile).
- Explicit preferences (live update/get round-trip; explicit beats inferred —
  unit test).
- Inferred preferences (rendered as observed behavior; never override
  explicit — unit + bridge tests).
- Session history (live conversation_history round-trip).
- Working and long-term memory selection by intent relevance (unit).
- Project knowledge: components, decisions (incl. supersession chains),
  known issues, build/test state (unit + live project_context_get).
- Historical information marked (informationStatus; stale flag with
  per-type TTLs — unit).
- Freshness and confidence preserved in ContextItems and sources (live
  ContextPackage inspection).
- Provenance: sources array with sourceType/sourceId/contribution (live).
- Conflict detection: CONTRADICTS surfaced in warnings (unit + Darwin).
- Deduplication: normalized-content dedup + bridge dedup vs existing
  Darwin package (unit).
- Token estimation: measured tokenEstimate (live; always present).
- Budget trim-to-fit: package trimmed to the requested budget; overflow
  reported explicitly (unit + live within-budget checks).

## 2. Security and isolation (all verified)

- User A cannot retrieve User B's private memory (unit; live spoofed-user
  check).
- Application A cannot access Application B's private history (live E2E
  negatives, -32002).
- Session IDs cannot be spoofed (session-scope fix + live checks).
- Client-supplied identity cannot override authenticated identity (live:
  spoofed userId ignored; token principal used).
- Shared knowledge returned only per access policy (live: shared project
  knowledge visible, private session content invisible).

## 3. Context quality

- ContextPackage assessment (quality, completeness, conflicts, staleItems,
  budgetUtilization) attached to every reconstruction (unit + live).
- The Context Inspector UI calls the exact same facade
  (`CognitiveContextService.getContext`) as MCP — no separate diagnostic
  calculation; the returned package JSON is displayed verbatim.

## 4. Test evidence

- ContextReconstructionTest: 7/7 PASS
- DarwinCognitiveIntegrationTest: 8/8 PASS
- Live MCP matrix context section + cross-application E2E reconstruction
  steps: PASS (20/20 scenario checks).
