# DARWIN COGNITIVE INTEGRATION
Darwin Engine ← CognitiveContextService native integration (verified)

## 1. What was integrated

- **`CognitiveContextBridge`** (new, `controller/cognitive/`): builds the
  platform context section for a Darwin task through the NATIVE
  `CognitiveContextService` facade. No HTTP, no MCP self-calls.
- **`ContextBuilder`** (extended, additive step 5c): after the existing
  memory-context retrieval, requests the cognitive section and appends it to
  `memoryContext`. Existing pipeline stages, ordering and consumers
  (`EvolutionOrchestrator` → `buildPrompt`) are unchanged.

## 2. Identity resolution

- Active user: orchestration-state metadata `userId`, else sysprop
  `evo.cognitive.user`, else `admin` (single-user EVO deployment default).
- Application: `evo` (internal). Session: `TaskContext.getSessionId()`.
- Project: `TaskContext.getProjectRoot()`.

## 3. Priority model (prompt ordering, highest first)

1. System and safety instructions (architecture rules, constraints)
2. Current explicit user instructions (task hints in constraints)
3. Current task requirements (goal/step)
4. Current session state (semantic workspace artifacts)
5. Verified current project state (cognitive project state)
6. Explicit user preferences
7. Relevant historical context (confirmed memories, decisions, issues)
8. Inferred preferences (rendered as "OBSERVED BEHAVIOR - not binding")
9. Lower-priority background knowledge (procedures, assumptions)

Historical/inferred information can never override current explicit
instructions; every section carries its provenance label. Conflicts are
surfaced in a CONTEXT WARNINGS section. Deduplication drops content already
present in the package.

## 4. Failure handling

The bridge never throws; ContextBuilder wraps the call in try/catch and
records a diagnostic (`ContextBuilder: Cognitive platform context
unavailable, continuing with local context`). An empty store produces no
section (identical behavior to before the integration).

## 5. Budget

Default 4000 tokens for the cognitive section (TokenEstimationService);
the bridge renders only content that fits (bounded summaries).

## 6. Verification (DarwinCognitiveIntegrationTest, 8/8 PASS)

| Test | Result |
|---|---|
| cognitiveContextIsRequestedAndMergedIntoPipeline | PASS — stored memory appears in the built ContextPackage |
| projectScopeIsUsed | PASS — other-project knowledge excluded |
| explicitInstructionsKeepPriorityOverCognitiveMemory | PASS — task section precedes cognitive memory in the prompt |
| conflictsAreSurfacedInPrompt | PASS — CONTEXT WARNINGS section present |
| duplicateInformationIsNotRepeated | PASS |
| emptyStorePreservesExistingBehavior | PASS — prompt structure unchanged |
| bridgeFailureDoesNotBreakContextBuild | PASS — null context → empty section; prompt remains well-formed |
| bridgeSectionRespectsTokenBudget | PASS |

ContextBuilderTest regression: 3/3 PASS.

## 7. Architecture invariants preserved

- IterationManager remains the sole iteration-transition authority.
- No new orchestrator, session manager, or prompt pipeline.
- ContextBuilder signature unchanged; all existing callers unaffected.
- No MCP/HTTP calls inside Darwin.
