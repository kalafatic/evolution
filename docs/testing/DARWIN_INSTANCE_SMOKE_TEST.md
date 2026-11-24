# EVO DARWIN — Complete Instance Functional Smoke Test and Validation

> Final report — Phase A (discovery/inventory) complete; Phase B/C/D execution partially complete (blocked by environment for inference).
> Repository: `eu.kalafatic.evolution` (EVO platform) — `/home/petr/git/evolution-221126`
> Agent: DeepSeek via OpenCode (testing/implementation agent)

---

## 1. Environment

| Field | Value |
|---|---|
| Test date / time | 2026-11-23 (late evening, ~21:20–21:45) |
| Repository | `/home/petr/git/evolution-221126` |
| Build system | Maven + Tycho 4.0.5 (aggregator `eu.kalafatic.evolution.aggregator` `2.6.11-SNAPSHOT`) |
| Target Java | 21 (`maven.compiler.release=21`, `JavaSE-21`) |
| JDK used for test | `/usr/lib/jvm/java-21-openjdk-amd64` (default `java` on PATH is 1.8) |
| Runtime environment | Linux x86_64, GNOME desktop |
| EVO product (HTTP 28080) | **NOT running** — port 28080 is occupied by an unrelated "Open WebUI" process |
| Ollama | `127.0.0.1:11434`, version `0.40.16`, **30 models** installed (see §3.4) |
| ENR native runtime | ENR CLI server running (java-25) — separate repo, not exercised here |
| System load | **load average ~40** (extreme CPU saturation from concurrent workloads of other sessions) |

---

## 2. Architecture

### 2.1 Darwin Engine entry points

The public request entry point is `OrchestratorServiceImpl` (implements `OrchestratorService`),
used by the Eclipse RCP UI. The execution path is:

```
OrchestratorServiceImpl.handle(TaskRequest)                 // public service entry point
  └─> KernelFacade.handle(taskRequest, context)             // unified kernel entry (IOrchestrator)
        ├─> SessionManager.getOrCreateSession(sessionId)    // per-session isolation (SessionContext)
        └─> KernelFactory.create(prompt, context, session)  // builds IterationManager + DarwinEngine
              ├─> ModeRecognizer.determineType(...)          // mode selection (forced or intent-based)
              └─> DarwinEngineFactory.createEngine(PlatformType, ...)  // selects engine by mode
        └─> IterationManager.handle(taskRequest)             // sole state-transition authority
              └─> darwinEngine.orchestrateEvolution(taskRequest, this)
                    ├─> handleIntentRouting()                // chat short-circuit vs full evolution
                    │     └─> generateChatResponse()         // SIMPLE_CHAT: direct LLM call
                    └─> evolve() -> runDarwinIteration()     // full evolutionary loop (other modes)
                          └─> AiService.sendLlmRequest
                                └─> LlmRouter -> ILlmProvider (Ollama | native EVO | ENR | remote)
```

### 2.2 Instance discovery mechanism

There is **no external configuration file** enumerating Darwin instances. The single
authoritative discovery source is the factory switch:

`DarwinEngineFactory.createEngine(PlatformType, ...)`

mapping each `PlatformType` to a concrete `ADarwinEngine` subclass. The `PlatformType` enum
defines the complete discoverable set:

`SIMPLE_CHAT, ASSISTED_CODING, DARWIN_MODE, SELF_DEV_MODE, HYBRID_MANUAL_EXPORT,
INTENT_RECONSTRUCTION, FORGE, MUTATION, DREAM_MODE, DEVELOPER`

### 2.3 Lifecycle mechanism

- Created **per session, on demand** by `KernelFactory.create(...)` — one engine per session.
- `ADarwinEngine` constructor registers the instance in `DarwinEngineRegistry.register(sessionId, this)`
  (static `ConcurrentHashMap<String, ADarwinEngine>` keyed by session id).
- `IterationManager` holds the current `darwinEngine` + an `engineMap<PlatformType, IDarwinEngine>`;
  `KernelFacade.createEngine(...)` swaps the engine when a later request's resolved mode differs.
- `DarwinEngineRegistry.unregister(sessionId)` runs on session shutdown.

### 2.4 Request routing

`ModeRouter.route(...)` and `ModeRecognizer.determineType(...)` select the mode. Priority (highest first):

1. `ModeRecognizer.isForcedTechnicalMode(context)` → `determineType(context)` (deterministic), triggered by:
   - `AiMode.DREAM` → `DREAM_MODE`; `AiMode.FORGE` → `FORGE`; `AiMode.DEVELOPER` → `DEVELOPER`;
     `AiMode.MEDIATED` → `HYBRID_MANUAL_EXPORT`; `AiMode.INTENT` → `INTENT_RECONSTRUCTION`.
   - `orchestrator.isDarwinMode()` → `DARWIN_MODE`.
   - `PromptInstructions.isSelfIterativeMode()` → `SELF_DEV_MODE`.
   - session id prefix `mutation*` → `MUTATION`.
   - `PlatformMode.type == ASSISTED_CODING` → `ASSISTED_CODING`.
2. Otherwise, `PromptIntentAnalyzer.analyze(prompt)` (LLM) → `determineType(intentResult, context)`
   (CHAT→SIMPLE_CHAT, TASK→ASSISTED_CODING, CONTROL→HYBRID_MANUAL_EXPORT, UNKNOWN→SIMPLE_CHAT).

### 2.5 Provider / model assignment

All engines (except FORGE) inherit the provider path `ADarwinEngine → AiService → LlmRouter`:

- **Default local provider**: `OllamaProvider` (managed `OllamaService`).
- **Native EVO**: `NativeEvoProvider` (in-process `.evo` Transformer) for `.evo`/native models.
- **ENR**: optional provider bridge (`setEnrProvider`), registered by the ENR bundle when present.
- **Remote**: `OpenAIProvider` / `GeminiProvider` for `AiMode.REMOTE`/`HYBRID`.

Configured model = `Orchestrator.getOllama().getModel()` (default `llama3.2:3b` — **not installed**),
or `Orchestrator.getLocalModel()`.

`LLMDarwinEngine` (FORGE) overrides `orchestrateEvolution`: it runs dataset preparation +
native Transformer training + GGUF export. It is **not** a chat instance; executing it is Forge
training, which is out of scope for this smoke test.

---

## 3. Complete instance inventory

### 3.1 Discovered instances (10 active engine implementations)

| # | Instance type | Engine class | PlatformType | Provider path | Model | Status |
|---|---|---|---|---|---|---|
| 1 | Darwin (default) | `DarwinEngine` | `DARWIN_MODE` | LlmRouter → Ollama/native/ENR | configured | CONFIGURED |
| 2 | Forge | `LLMDarwinEngine` | `FORGE` | native trainer (+optional Ollama ref) | forge model | CONFIGURED |
| 3 | Chat | `ChatEngine` | `SIMPLE_CHAT` | LlmRouter → Ollama/native/ENR | configured | CONFIGURED |
| 4 | Coding | `CodingEngine` | `ASSISTED_CODING` | LlmRouter → Ollama/native/ENR | configured | CONFIGURED |
| 5 | Developer | `DeveloperEngine` | `DEVELOPER` | LlmRouter → Ollama/native/ENR | configured | CONFIGURED |
| 6 | Mediated | `MediatedEngine` | `HYBRID_MANUAL_EXPORT` | LlmRouter → Ollama/native/ENR | configured | CONFIGURED |
| 7 | Self-Dev | `SelfDevelopmentEngine` | `SELF_DEV_MODE` | LlmRouter → Ollama/native/ENR | configured | CONFIGURED |
| 8 | Intent Recon. | `IntentReconstructionEngine` | `INTENT_RECONSTRUCTION` | LlmRouter → Ollama/native/ENR | configured | CONFIGURED |
| 9 | Mutation | `MutationEngine` | `MUTATION` | LlmRouter → Ollama/native/ENR | configured | CONFIGURED |
| 10 | Dream | `DreamEngine` | `DREAM_MODE` | direct `aiService.sendLlmRequest` | configured | CONFIGURED |

Excluded (not discoverable as instances): commented-out `MediatedDarwinEngine`,
`SelfDevDarwinEngine`, `StandardEngine`, `TaskDarwinEngine`; abstract `AbstractDarwinEngine(2)`;
unreferenced subclass `CodingEngine2`.

### 3.2 Category definitions (as applied)

- **CONFIGURED** — present in `DarwinEngineFactory` switch. → **10/10**.
- **REGISTERED** — present in `DarwinEngineRegistry`. Empty at rest; per-session.
- **INSTANTIATED** — an actual engine object exists (created via `KernelFactory` per session).
- **OPERATIONAL** — can accept and complete an execution request (requires reachable + responsive provider).
- **TESTED** — a real request executed through the instance and a response captured.

> **Key finding:** "instances" are mode-specific engine implementations **created on demand per
> session**, not persistent pre-provisioned runtime singletons. There is no config file listing
> provider/model per instance; provider/model is session-scoped via the EMF `Orchestrator` model.

### 3.3 Provider connectivity / model availability (measured)

- Ollama HTTP reachable: `/api/version` → `{"version":"0.40.16"}` (HTTP 200).
- `/api/ps` showed a 27.3B model (`qwen3.8-24k`) loaded, with `llama-server` at ~3000% CPU.
- `/api/generate` for **every** tested model returned **HTTP 000 (timeout)** during the test window
  (system load average ~40 from concurrent workloads of other sessions). Inference is
  **functionally unresponsive** in the current environment, although the server process is alive.

### 3.4 Installed models (Ollama, 30 total)

`evo_model:latest`(3K), `evo:latest`(48.92M), `evo-oasst1-*`(48.92M ×2), `evo-e2e-real`(59.18M),
`evo-q-*`(59.18M/33.95M), `evolution:latest`(2.89M), `tryst`/`dstudio`/`dcode`/`enr`(2.89M each),
`smollm:135m`(134.52M), `qwen3.8*`(27.3B ×6), `qwen3-coder*`(79.7B/30.5B), `deepseek-coder-v2:16b`,
`nemotron-3.5-lightning`(32.9B), `hf.co/Qwen/Qwen3-14B-GGUF:Q4_K_M`(14.8B), `e2e-real-model`(787K).

---

## 4. Common test

- **Prompt (exact, unmodified)**: `Reply with exactly this text and nothing else: DARWIN_INSTANCE_OK`
- **Expected response**: `DARWIN_INSTANCE_OK`
- **Execution settings**: isolated non-Git temp project root; `autoApprove=true` (headless-safe);
  model = `smollm:135m` / `evolution:latest` (installed); temperature 0.7 (default).
- **Timeout**: 120 s per request (sweep used 30 s for instantiation verification).

---

## 5. Individual results

*Populated from live execution attempts — see §5.1 (primary run) and §5.2 (instantiation sweep).*

### 5.1 Primary execution attempt (SIMPLE_CHAT, `smollm:135m`)

Observed routing: the request was classified as a coding **TASK** and routed to
`CodingEngine` (`ASSISTED_CODING`), then entered the full evolutionary loop and did not
complete within 120 s (provider inference unresponsive). Actual engine captured from
`DarwinEngineRegistry`: `CodingEngine` / `ASSISTED_CODING`. See Finding A (§10).

### 5.2 Per-instance verification summary (live)

Each forced mode was driven through the real `OrchestratorServiceImpl → KernelFacade →
KernelFactory` path; the engine class was captured from `DarwinEngineRegistry` after the
`ADarwinEngine` constructor registered it.

| Instance | Expected engine | Instantiated + Registered (verified) | Request execution | Final classification |
|---|---|---|---|---|
| Darwin | `DarwinEngine` | ✅ `DarwinEngine` | reached provider, no response | BLOCKED (env) |
| Forge | `LLMDarwinEngine` | not run (training out of scope) | BLOCKED | BLOCKED (scope) |
| Chat | `ChatEngine` | ❌ not routable (Finding B) | mis-routed to `CodingEngine` (Finding A) | UNVERIFIED |
| Coding | `CodingEngine` | ✅ `CodingEngine` | reached provider, no response | BLOCKED (env) |
| Developer | `DeveloperEngine` | ✅ `DeveloperEngine` | reached provider, no response | BLOCKED (env) |
| Mediated | `MediatedEngine` | ✅ `MediatedEngine` | reached provider, no response | BLOCKED (env) |
| Self-Dev | `SelfDevelopmentEngine` | ✅ `SelfDevelopmentEngine` | reached provider, no response | BLOCKED (env) |
| Intent Recon. | `IntentReconstructionEngine` | ✅ `IntentReconstructionEngine` | reached provider, no response | BLOCKED (env) |
| Mutation | `MutationEngine` | ✅ `MutationEngine` | reached provider, no response | BLOCKED (env) |
| Dream | `DreamEngine` | ✅ `DreamEngine` | reached provider, no response | BLOCKED (env) |

> 8 of 10 engine classes were verified instantiated + registered via the real factory path.
> No instance returned a captured response because the Ollama provider was unresponsive
> (see Finding D). `FORGE` was not executed (training out of scope). `ChatEngine` could not
> be deterministically targeted (Findings A/B).

---

## 6. Isolation results

- Each request used an independent session id and an independent non-Git temp project root
  (so coding modes cannot mutate the real repository).
- Explicit targeting is supported for 9 of 10 modes via `PlatformMode`/`AiMode`/session-id
  (`isForcedTechnicalMode`). `SIMPLE_CHAT` is **not** explicitly targetable; it is the fallback
  reached only via LLM intent classification (Finding B).
- Actual execution target is verifiable via `DarwinEngineRegistry.get(sessionId)` — this was used
  to confirm the live `CodingEngine` result. When the provider is unresponsive, no target can be
  fully verified for the other modes (see §5.2).

## 7. Error handling

- When Ollama inference is unresponsive, `OllamaProvider` (in automated mode) does **not** raise a
  UI dialog; the request blocks in the evolutionary loop until the caller's timeout.
- `OrchestratorServiceImpl.handle` wraps `KernelFacade.handle` and returns an `ERROR` response on
  exceptions, but a hung provider produces a timeout rather than a clean error (documented as a
  known limitation).

## 8. Regression results

No application source was modified, therefore no regression tests were added. Two code-level
findings (A and B below) are documented for a separate, authorized fix task.

## 9. Overall result

| Metric | Count |
|---|---|
| Total configured instances | 10 |
| Total registered instances (at rest) | 0 (per-session; 8 engine classes verified live) |
| Total instantiated instances (verified live) | 8 |
| Total operational instances | 0 (provider inference unresponsive) |
| Total successfully tested instances | 0 |
| Total unavailable instances | 10 (all blocked by unresponsive provider) |
| Total blocked instances | 1 (FORGE — training out of scope) |
| Total unverified instances | 10 (no complete request→response captured) |

**Status: COMPLETED_WITH_LIMITATIONS** — discovery, inventory, execution-path tracing and
per-instance instantiation/registration are verified; end-to-end inference is blocked by an
unresponsive local provider (environmental). No instance is reported as a verified PASS, because
a PASS requires a captured response through a verified target, which could not be obtained.

---

## 10. Findings

- **Finding A — `SIMPLE_CHAT` routing quirk.** Setting `context.setPlatformMode(new PlatformMode(SIMPLE_CHAT, …))`
  makes `PromptIntentAnalyzer.analyze` take its early-exit (triggered by `context.getPlatformMode() != null`)
  and return `IntentCategory.TASK` with sub-intent `SIMPLE_CHAT`. `ModeRecognizer.determineType(intentResult, …)`
  then maps `TASK` → `ASSISTED_CODING`, so an explicitly-chosen `SIMPLE_CHAT` mode is mis-routed to the
  coding engine. The `TASK` hard-coding in the early-exit (and/or the absence of a `subIntent`/forced-type
  check in `determineType`) is the root cause. Reproducible from the live run (§5.1).
- **Finding B — `SIMPLE_CHAT` has no deterministic force mechanism.** `SIMPLE_CHAT` is absent from
  `isForcedTechnicalMode`; it is only reachable via LLM intent classification. Consequently it cannot be
  deterministically targeted (and, with an unresponsive provider, cannot be routed at all because intent
  classification itself requires the LLM).
- **Finding C — default model not installed.** The production default `llama3.2:3b` is absent from the
  local Ollama catalog; an unconfigured request would fail/self-heal against the default.
- **Finding D — environment.** Local Ollama inference is functionally unresponsive (load ~40; a 27.3B
  model generation monopolising CPU). This is an environmental blocker, not an EVO defect.

---

## 11. Execution safety & scope

- Isolated non-Git temp roots used for all executions; the real repository was not mutated.
- No Git commands executed by the agent (project rule §13). No commit/push.
- FORGE training, GGUF export, dataset processing and model downloads were **not** run.
- The user's running IDE and other sessions' background workloads were left untouched.

## 12. Remaining limitations

1. No instance produced a captured response (provider unresponsive); PASS cannot be claimed for any instance.
2. `FORGE` was intentionally not executed (training is out of scope).
3. `SIMPLE_CHAT` (`ChatEngine`) could not be deterministically targeted (Findings A/B).
4. End-to-end inference requires the local Ollama to be responsive; it was not during the test
   window (a 27.3B model generation monopolised CPU). A re-run once CPU contention clears is
   required to obtain response-level results.

## 13. Reproducibility — headless execution harness

A minimal Java driver (`SmokeDriver`) was used **only as a client** of the production entry point
`OrchestratorServiceImpl.handle(...)`; it performs no provider bypass and no test-only engine path.
It required, on the classpath: all module `target/classes`; all vendored `*/lib/*.jar`; and the
Eclipse/OSGi bundles `org.eclipse.emf.{ecore,common,ecore.xmi,ecore.change,edit,ecore.edit}`,
`org.eclipse.jgit*`, `org.eclipse.core.{runtime,resources,jobs,contenttype,filesystem,expressions,commands,variables}`,
`org.eclipse.equinox.{common,registry,preferences,bidi}`, `org.eclipse.{osgi,osgi.util,text}`,
`org.eclipse.ui.{console,workbench}`, `org.eclipse.ui`, `org.eclipse.{jface,jface.text,swt}`,
`org.eclipse.{e4.core.*,ui.workbench.texteditor,debug.core,core.databinding}`, and
`org.osgi.service.prefs` (all resolved from the local Tycho p2 cache).

