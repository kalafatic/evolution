# CROSS-APPLICATION E2E REPORT
Mandatory scenario (spec 13), executed live against the running EVO on 28280.

## Setup

- Two distinct application identities (TRYST, DStudio) with separate
  scoped tokens, one shared user, one shared project
  (`/tmp/e2e-shared-project`).
- Driver speaks the exact JSON-RPC wire protocol used by the TRYST/DStudio
  EvoMcpClient implementations (and both real client classes were
  additionally verified live in their own repositories' test suites).

## Scenario execution and results (20/20 PASS)

### Step 1-2: TRYST authenticates, starts a task, requests context
- session_create(TASK, correlationId) → cognitive session id. PASS
- context_get (budget 8000) → ContextPackage. PASS

### Step 3: TRYST executes work and persists through EVO
- decision_record (architectural decision). PASS
- memory_store EPISODE (task outcome, PROJECT scope). PASS
- memory_store WORKING note bound to the TRYST session (SESSION scope). PASS
- user_preferences_update (explicit user preference). PASS
- context_record (context snapshot). PASS

### Step 4-5: TRYST terminates; DStudio starts a new workflow
- DStudio session_create. PASS
- DStudio context_get reconstructs:
  - the TRYST architectural decision (shared project knowledge) PASS
  - the TRYST episode outcome PASS
  - the explicit user preference PASS
  - **not** the TRYST private session note PASS
  - **not** the TRYST conversation history PASS

### Step 6-7: DStudio continues the work and persists new knowledge
- DStudio memory_store (new finding, PROJECT scope). PASS
- TRYST memory_search finds DStudio's finding (authorized client sees
  shared knowledge). PASS

### Step 8: Isolation negatives
- DStudio cannot read the TRYST session (-32002). PASS
- DStudio cannot read the TRYST conversation (-32002). PASS
- TRYST cannot read the DStudio session (-32002). PASS

### Step 9: User-level isolation
- context_get with a spoofed userId does not gain private data. PASS

## Proven capabilities

Shared identity handling, application authentication, authorization,
shared knowledge, context reconstruction, persistent history,
cross-application continuity, data isolation, and actual use of the
reconstructed context by both applications' wire clients.

## Defects found and fixed through this scenario

1. Session ownership checked user only → cross-app session reads were
   possible for the same user (fixed: app+user binding; regression-tested).
2. SESSION-scoped entries leaked to session-less requests (fixed:
   sessionRef must match the requesting session; regression-tested).
3. memory_store bound entries to the ephemeral MCP request id instead of a
   cognitive session (fixed: explicit validated sessionId argument).
4. Decision search with NULL context dropped rows (fixed: COALESCE +
   token OR-semantics; regression-tested).
