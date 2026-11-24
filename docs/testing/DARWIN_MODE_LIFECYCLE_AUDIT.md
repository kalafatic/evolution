# EVO Darwin — Dynamic Platform Mode Lifecycle Audit and Correction

> Repository: `eu.kalafatic.evolution` (EVO platform) — `/home/petr/git/evolution-221126`
> Scope: platform-mode selection, intent classification, iteration transitions, execution routing.
> Result: 3 targeted corrections applied + 1 new regression suite; 0 regressions in existing suites.

---

## 1. Executive summary

Darwin supports **10 execution modes** (`PlatformType`), each backed by a distinct engine
subclass. Mode selection is intended to be **dynamic per iteration within a single persistent
session** (a `SessionContext` re-used across turns retains one `TaskContext` and one
`IterationManager`, and `KernelFacade.handle` re-evaluates the mode on every turn).

The audit found that dynamic mode selection was effectively **broken after the first request**
by three interacting defects:

1. **D1 — intent classification permanently disabled.** `PromptIntentAnalyzer.analyze` treated
   *any* non-null `PlatformMode` as an authoritative override. Because `PlatformMode` is set on
   the first request and never cleared, every later non-greeting request skipped LLM intent
   classification and was forced to `TASK` → `ASSISTED_CODING`.
2. **D2 — auto-selected modes became sticky.** `ModeRecognizer.isForcedTechnicalMode` treated the
   *derived* `PlatformMode` cache as an explicit override, so an auto-detected mode (e.g.
   `ASSISTED_CODING`) would lock the session and prevent returning to `SIMPLE_CHAT`.
3. **D3 — stale mode cache.** `KernelFacade.createEngine` swapped the engine on a mode change but
   did not update `context.platformMode`, so downstream consumers (`FinalResponseAssembler`,
   `BehaviorPolicyRegistry`, …) kept observing the previous turn's mode.

All three were corrected with minimal, targeted changes. A new regression suite
(`DarwinDynamicModeLifecycleTest`, 4 tests) verifies the corrected behaviour; the existing
`DarwinModesAndRoutingRegressionTest` (7 tests) remains green apart from one pre-existing,
environment-only failure (missing Eclipse UI test harness in the plain-JVM runner), which fails
identically before and after the change.

---

## 2. The 10 modes — actual semantics (from the implementation)

| # | Mode (`PlatformType`) | Engine | Purpose | Expected input | Expected output | Selectable | Duration | Return to other modes |
|---|---|---|---|---|---|---|---|---|
| 1 | `SIMPLE_CHAT` | `ChatEngine` | Conversational chat | greeting / question | LLM text (direct `generateChatResponse`) | auto (greeting / CHAT intent) | single request | yes (default fallback) |
| 2 | `ASSISTED_CODING` | `CodingEngine` | Assisted code generation | coding request | code changes + summary | auto (TASK intent) + `mode: assisted` | single task (multi-iteration) | yes |
| 3 | `DARWIN_MODE` | `DarwinEngine` | Full evolutionary coding loop (branches/variants/selection) | coding/architecture task | implemented changes + final response | **explicit only** (`darwinMode` flag, `mode: darwin`) | task | yes |
| 4 | `SELF_DEV_MODE` | `SelfDevelopmentEngine` | Modify EVO's own code (self-development) | self-improvement request | changes to allowed paths | **explicit** (`selfIterativeMode`, `mode: self-dev`) | workflow | yes |
| 5 | `INTENT_RECONSTRUCTION` | `IntentReconstructionEngine` | Reverse-engineer intent from existing code | "discover/reconstruct intent" | intent analysis | auto (reasoning keywords) + `AiMode.INTENT` | task | yes |
| 6 | `HYBRID_MANUAL_EXPORT` (MEDIATED) | `MediatedEngine` | Mediate/analyze an external target and generate changes | "analyze target"/"mediate" | mediated analysis + proposals | `AiMode.MEDIATED`, `mode: mediated`, auto (CONTROL intent) | task | yes |
| 7 | `FORGE` | `LLMDarwinEngine` | Train a native EVO model (dataset → training → GGUF export) | forge request | trained model | **explicit only** (`AiMode.FORGE`, `mode: forge`) | long workflow | n/a (not a chat instance) |
| 8 | `DREAM_MODE` | `DreamEngine` | Synthesize/consolidate experiences into a narrative | `AiMode.DREAM` / `mode: dream` | dream synthesis (direct LLM) | **explicit only** | single request | yes |
| 9 | `DEVELOPER` | `DeveloperEngine` | High-autonomy coding | specific programming request | code changes | `AiMode.DEVELOPER`, `mode: developer`, auto (specific-programming heuristic) | task | yes |
| 10 | `MUTATION` | `MutationEngine` | Evolutionary code mutation | session `mutation*` / `MutationSessionManager` | mutated code | **explicit** (session-based) | task | yes |

**Key distinctions established:**

- **Auto-selectable** (re-evaluated every turn): `SIMPLE_CHAT`, `ASSISTED_CODING`,
  `INTENT_RECONSTRUCTION`, `HYBRID_MANUAL_EXPORT` (via `CONTROL` intent). These are what the
  `PromptIntentAnalyzer` → `ModeRecognizer.determineType(IntentResult, …)` path can produce.
- **Explicit-only** (sticky until the override is cleared): `DARWIN_MODE` (darwin flag),
  `SELF_DEV_MODE` (self-iterative flag), `FORGE`/`DREAM`/`DEVELOPER`/`MEDIATED`/`INTENT` (via
  `AiMode`), `MUTATION` (session id). These are high-autonomy or heavyweight modes that are not
  auto-triggered by a plain chat/coding intent.

> The illustrative example table in the mission maps "Analyze the architecture" → `DARWIN_MODE` and
> "Implement changes" → `DEVELOPER`. In the **actual implementation** those two modes are
> explicit-only: "Analyze the architecture" auto-routes to `INTENT_RECONSTRUCTION` (reasoning
> keyword) or `HYBRID_MANUAL_EXPORT` (cognitive `ARCHITECTURE`/`MEDIATED` capability), and
> `DEVELOPER` is activated only by `AiMode.DEVELOPER`, the `mode: developer` directive, or the
> specific-programming-request heuristic. This is documented as an intended-semantics finding
> (§6.4), not changed.

---

## 3. Complete production execution path — trace points

```
OrchestratorServiceImpl.handle / submit               // public entry point
  ├─ SessionManager.getOrCreateSession(sessionId)      // per-session SessionContext (reused)
  └─ KernelFacade.handle(taskRequest, context)
       ├─ [SESSION RESTORE] session = (SessionContext) context.metadata OR getOrCreateSession
       ├─ kernel = session.getIterationManager()        // previous iteration's kernel (persisted)
       ├─ if kernel == null:
       │    KernelFactory.create(prompt, context, session)          // ── INITIAL MODE ESTABLISHMENT
       │      ├─ ModeRecognizer.isForcedTechnicalMode(context)?      // explicit override?
       │      │     └─ determineType(context)                        //    explicit mode
       │      │     └─ PromptIntentAnalyzer.analyze(prompt)          //    ── INTENT CLASSIFICATION
       │      │           └─ determineType(intentResult, context)    //    CHAT/TASK/CONTROL/UNKNOWN
       │      └─ DarwinEngineFactory.createEngine(platformType, …)   //    ── ENGINE SELECTION
       │            └─ ADarwinEngine ctor → DarwinEngineRegistry.register
       ├─ else:                                                     // ── CURRENT MODE DETERMINATION (per turn)
       │    re-analyze intent → platformType
       │    if engine.platformType != platformType:
       │         KernelFacade.createEngine(...)                      //    ── MODE TRANSITION AUTHORIZED
       │           └─ kernel.setDarwinEngine(newEngine)              //       + platformMode sync (D3)
       └─ kernel.handle(taskRequest)
            └─ IterationManager.handle → darwinEngine.orchestrateEvolution
                  ├─ handleIntentRouting → PromptIntentAnalyzer.analyze  // chat short-circuit vs task
                  ├─ [platformMode cache] ModeRouter.route (turn 1 only)
                  └─ evolve() → runDarwinIteration() → AiService → LlmRouter → provider
```

Exact locations:

| Step | Location |
|---|---|
| Initial session mode established | `KernelFactory.create` (line 38–44); platformMode cache set by `ADarwinEngine.orchestrateEvolution` (line 398–400) |
| Current iteration mode determined | `KernelFacade.handle` (line 39–49) and `executeTask` (line 159–171) — re-evaluated every turn |
| Intent classification takes place | `PromptIntentAnalyzer.analyze` (line 83–181); callers: `KernelFactory` (42), `KernelFacade` (44, 165), `ADarwinEngine.handleIntentRouting` (3410) |
| Mode transitions authorized | `KernelFacade.createEngine` (line 185–204) — swaps engine when `engine.platformType != platformType` |
| Engine instances selected | `DarwinEngineFactory.createEngine(PlatformType, …)` (line 13–43) |
| Execution dispatched | `IterationManager.handle` (line 393–404) → `darwinEngine.orchestrateEvolution` |
| Previous iteration context restored | `KernelFacade.handle` (line 26–31: session + kernel lookup); `SessionContext.getTaskContext`; `IterationManager` ctor (checkpoint restore, line 331–338) |

---

## 4. Defects found and corrected

### 4.1 D1 — intent classification permanently disabled (corrected)

**File:** `PromptIntentAnalyzer.java`, `analyze` (line ~94).

```java
// before
if (context != null && (ModeRecognizer.isForcedTechnicalMode(context) || context.getPlatformMode() != null)) {
// after
if (context != null && ModeRecognizer.isForcedTechnicalMode(context)) {
```

`context.platformMode` is a **derived routing cache**, set on the first request by
`orchestrateEvolution` and kept thereafter. The `|| context.getPlatformMode() != null` clause made
every subsequent non-greeting request return `TASK` without ever invoking the LLM, which
`determineType(IntentResult, …)` then mapped to `ASSISTED_CODING`. Greetings still worked only
because `isSimpleGreeting` is checked before the early exit.

### 4.2 D2 — auto-selected modes became sticky (corrected)

**File:** `ModeRecognizer.java`, `isForcedTechnicalMode` + `isDreamMode`/`isForgeMode`/
`isDeveloperMode`/`isSelfDevMode`/`isMediatedMode`/`isIntentMode`/`isDarwinMode`/`isCoding`.

Removed the `context.getPlatformMode().getType() == X` clauses and the `isCoding(context)` term
from `isForcedTechnicalMode`. A "forced technical mode" is now defined strictly by **explicit
inputs**: `AiMode` (DREAM/FORGE/DEVELOPER/MEDIATED/INTENT), `orchestrator.isDarwinMode()`,
`PromptInstructions.isSelfIterativeMode()`, the `mutation*` session id, and the `WORKFLOW_SELF_DEV` /
`WORKFLOW_EXPORT_ONLY` behaviour traits. This restores per-iteration re-evaluation and lets a
session return from coding to chat (a greeting is no longer swallowed by a sticky coding mode).

### 4.3 D3 — stale platform-mode cache (corrected)

**Files:** `ModeRouter.java` (added `public static createMode(PlatformType)`, single source of
mode construction incl. autonomy/allowed-paths); `KernelFacade.createEngine` (now calls
`context.setPlatformMode(ModeRouter.createMode(platformType))` when swapping the engine).

This keeps the cached mode in sync with the engine actually selected, so consumers such as
`FinalResponseAssembler` and `BehaviorPolicyRegistry` observe the current mode instead of a stale
one.

---

## 5. Files modified

| File | Change |
|---|---|
| `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/agents/PromptIntentAnalyzer.java` | D1 — early exit only on explicit forced mode |
| `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/util/ModeRecognizer.java` | D2 — forced-mode detection from explicit inputs only |
| `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/ModeRouter.java` | D3 — added `createMode(PlatformType)`; made mode factories `static` |
| `eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/KernelFacade.java` | D3 — sync `platformMode` on engine swap |
| `eu.kalafatic.evolution.controller.tests/src/eu/kalafatic/evolution/controller/tests/DarwinDynamicModeLifecycleTest.java` | NEW regression suite (4 tests) |

---

## 6. Findings not changed (documented for awareness)

1. **Two sources of truth for mode.** Engine selection (`KernelFacade`/`KernelFactory`) uses
   `PromptIntentAnalyzer` + `ModeRecognizer.determineType` (LLM intent), while the platform-mode
   cache was historically refreshed by `ModeRouter.route` (cognitive pipeline). The cognitive
   pipeline can distinguish `EVOLUTION`/`ARCHITECTURE`/`DEVELOPER` capabilities that the intent
   classifier cannot. Fully unifying these is an architectural change and out of scope.
2. **`DARWIN_MODE` and `DEVELOPER` are explicit-only** (see §2). Auto-routing "analyze architecture"
   yields `INTENT_RECONSTRUCTION` (reasoning keyword) rather than `DARWIN_MODE` — a semantic
   ambiguity left unchanged.
3. **`mode: <x>` prompt directives are inconsistent.** `ModeRouter.route` honours them (setting the
   platform-mode cache), but engine selection (`determineType`) does not, so e.g. `mode: darwin`
   does not select `DarwinEngine`. Pre-existing; out of scope.
4. **`orchestrateEvolution` refreshes the platform-mode cache only once.** With D3 the cache is now
   corrected on engine swaps; the once-only `ModeRouter.route` call remains harmless.

---

## 7. Build and test results

- **Compilation:** the 4 modified controller files compile cleanly (`javac` release 21 against the
  existing module classpath + Eclipse/EMF/OSGi bundles). `COMPILE_RC=0` (only pre-existing
  deprecation/unchecked notes).
- **New regression suite** `DarwinDynamicModeLifecycleTest`: **4/4 PASS**
  (`testNonForcedPlatformModeStillRunsIntentClassification`, `testAutoSelectedModesAreNotForced`,
  `testGreetingReturnsToChatAfterCodingMode`, `testCreateModeBuildsCorrectMode`).
- **Existing** `DarwinModesAndRoutingRegressionTest`: **6/7 PASS**; the 1 failure
  (`testMalformedIntentResponseResolvesToUnknownOrSafeChat`) is a `ClassNotFoundException:
  org.eclipse.ui.testing.TestableObject` in the plain-JVM runner (no Eclipse UI test harness) and
  reproduces **identically** against the unmodified classes — i.e. pre-existing and environmental,
  not a regression.

> Note: tests were run in a plain-JVM harness against the compiled module classes (the authoritative
> Tycho/OSGi test run requires the full Eclipse test runtime, which is out of scope here). The
> production code paths exercised are identical.

---

## 8. Remaining limitations

1. Verification used the compiled classes + a minimal classpath; the full Tycho `mvn verify`
   (OSGi test runtime) was not executed in this environment.
2. The "two sources of truth" and explicit-only `DARWIN_MODE`/`DEVELOPER` semantics remain as-is
   (documented, not changed).
3. End-to-end inference was not re-validated (local Ollama was unresponsive during this session),
   so the corrected routing was verified at the unit/classification level rather than through a live
   model round-trip.

---

## 9. Manual verification instructions

1. Build the controller module (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -o -pl
   eu.kalafatic.evolution.controller -am compile -DskipTests`).
2. Run `DarwinDynamicModeLifecycleTest` (4 tests) and `DarwinModesAndRoutingRegressionTest` via the
   Tycho surefire harness (`./mvnw -o -pl eu.kalafatic.evolution.controller.tests verify`).
3. Manually, in a persistent session: send `Hi!` (expect chat), `Create a Java class X` (expect
   coding), `Thanks!` (expect chat again), `Reconstruct the intent of this code` (expect
   intent-reconstruction) — each request must be re-classified via the intent analyzer, and the
   cached platform mode must follow the selected engine.
