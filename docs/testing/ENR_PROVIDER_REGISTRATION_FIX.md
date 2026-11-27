# Fix: AI Commander Forge — ENR Provider Registration

> Repository: `eu.kalafatic.evolution` — `/home/petr/git/evolution-221126`
> Symptom: selecting "EVO Native ENR" in Chat Management fails with
> `ENR provider is not registered (ENR bundle not present)`.

---

## 1. Actual root cause

The ENR provider is wired into Darwin's `LlmRouter` **only** by the ENR bundle's OSGi activator:

- `eu.kalafatic.evolution.enr` → `MANIFEST.MF` declares `Bundle-Activator: eu.kalafatic.evolution.enr.EnrActivator`.
- `EnrActivator.start()` calls `LlmRouter.getInstance().setEnrProvider(new EnrLlmProvider(EnrRuntime.instance()))`.

The `eu.kalafatic.evolution.enr` bundle **is included** in the product (`eu.kalafatic.evolution.feature/feature.xml`
lists it as a `<plugin>`), but it was **never configured to auto-start**:

- the product `<configurations>` (both `evo.product` and `evolution.product`) did not list it;
- the PDE launch config listed it as `@default:default` (not started);
- no production bundle `Require-Bundle`s it (only `controller.tests` does).

Because the bundle is a normal (non-lazy) activator bundle, its activator runs **only when the bundle is
STARTED**. Since nothing started it, `EnrActivator.start()` never ran, `LlmRouter.enrProvider` stayed
`null`, and `LlmRouter.discoverModels("EVO Native ENR", …)` threw the misleading
"ENR bundle not present" error. The bundle was present — it was simply **RESOLVED but not STARTED**.

The error was a service-lifecycle/registration problem, **not** an Ollama discovery problem. ENR's
`EnrRuntime.listModels()` already delegates to the configured Ollama registry (`OllamaService.loadModels()`)
plus native `.evo` / llama.cpp `.gguf` artifacts — no new discovery mechanism was needed.

---

## 2. Fix

Make the ENR bundle start at application startup so its activator registers the provider, and improve
diagnostics so the actual state is reported instead of "bundle not present".

### 2.1 Auto-start the ENR bundle (core fix)

Added `<plugin id="eu.kalafatic.evolution.enr" autoStart="true" startLevel="3" />` to `<configurations>` in:

- `eu.kalafatic.evolution.repository/evo.product`
- `eu.kalafatic.evolution.repository/evolution.product`
- `eu.kalafatic.evolution.view/evo.product`
- `eu.kalafatic.evolution.view/evolution.product`

and changed the PDE launch bundle entries from "not started" to auto-start:

- `.launches/EVO_RCP_Application.launch` → `eu.kalafatic.evolution.enr@default:true`
- `.launches/EVO_RCP_Server_Mode.launch` → `eu.kalafatic.evolution.enr@default:true`

### 2.2 Diagnostics (Phase 4)

`LlmRouter.discoverModels` now logs the lookup and reports the actual ENR bundle state when the provider
is absent (replacing the misleading "ENR bundle not present"):

- `[AI-CMD][DISCOVERY] ENR service lookup started …`
- `[AI-CMD][DISCOVERY] ENR service registered; delegating model discovery to ENR.`
- on failure: `ENR provider is not registered: the ENR bundle (eu.kalafatic.evolution.enr) is RESOLVED
  (installed but NOT started); its EnrActivator has not wired the provider into LlmRouter (verify
  autoStart=true …)` — via a new `describeEnrBundle()` helper that reads `Platform.getBundle(...).getState()`.

`EnrActivator.start()` now:

- wraps `EnrRuntime.instance()` and service registration in try/catch (the bundle still starts and logs a
  clear message if runtime init fails);
- logs `[ENR] ENR provider registered in LlmRouter (evo.enr.router.enabled=…)` on successful wiring.

### 2.3 Model discovery (Phase 3)

Unchanged: ENR discovery already returns the same Ollama models via the configured Ollama endpoint and
preserves the stable provider-prefixed identifiers (`ollama:<name>`, `evo-native:<file>`, `llama-cpp:<file>`).
No hardcoded model names were introduced.

---

## 3. Files changed

| File | Change |
|---|---|
| `eu.kalafatic.evolution.repository/evo.product` | ENR `autoStart=true, startLevel=3` |
| `eu.kalafatic.evolution.repository/evolution.product` | ENR `autoStart=true, startLevel=3` |
| `eu.kalafatic.evolution.view/evo.product` | ENR `autoStart=true, startLevel=3` |
| `eu.kalafatic.evolution.view/evolution.product` | ENR `autoStart=true, startLevel=3` |
| `.launches/EVO_RCP_Application.launch` | ENR `@default:true` |
| `.launches/EVO_RCP_Server_Mode.launch` | ENR `@default:true` |
| `…/controller/orchestration/llm/LlmRouter.java` | diagnostic logging + `describeEnrBundle()` + accurate error |
| `…/enr/EnrActivator.java` | robust startup + "registered" log |

---

## 4. OSGi registration / lookup mechanism

- **Registration**: `EnrActivator.start()` → `LlmRouter.getInstance().setEnrProvider(new EnrLlmProvider(runtime))`
  (the existing Phase-6 static-slot mechanism; not replaced, no new service registry).
- **Lookup**: `ChatMgmtGroup` → `LlmRouter.discoverModels("EVO Native ENR", orchestrator)` →
  `normalizeProviderKey` → `"enr"` → `enrProvider.discoverModels(...)` → `EnrLlmProvider.discoverModels` →
  `EnrRuntime.listModels()` → Ollama registry + native/llama.cpp artifacts.

---

## 5. Verification

- **Compilation**: `LlmRouter.java` and `EnrActivator.java` compile cleanly (`javac --release 21`, RC=0)
  against the module classpath + Eclipse/OSGi bundles. `org.osgi.framework.Bundle` was already used by the
  controller bundle (`EvolutionServer.java`), so the new import is consistent.
- **Existing tests**: `ProviderModelDiscoveryTest.testEnrDiscoveryWithoutRegisteredProviderFailsExplicitly`
  still passes (the new exception message still `contains("ENR")`).
- **Full Tycho build**: not executable here — it fails at `eu.kalafatic.utils` with a **pre-existing**
  `Unknown OSGi execution environment: 'JavaSE-25'` error (the repository was migrated to `JavaSE-25`
  BREE/`executionEnvironment` while the 2025-12 target platform does not provide it). This is unrelated to
  this change and predates it.
- **Runtime**: not re-launched (the running app instance is the user's; the correction requires a restart
  with the updated product/launch config). Expected outcome after restart: `[ENR] ENR provider registered
  in LlmRouter` at startup, then `[AI-CMD][DISCOVERY] ENR service registered … Models discovered: N`.

---

## 6. Remaining notes / limitations

1. The `JavaSE-25` execution-environment inconsistency (pre-existing, unrelated) still breaks the full
   Tycho build; it was not addressed in this change.
2. `EVO_Headless_Server.launch` still lists ENR without auto-start; left unchanged because the headless
   server does not expose the AI Commander Chat Management UI where this bug manifests.
3. The `EnrActivator` uses the established static `LlmRouter` slot (Phase-6 mechanism); no Declarative
   Services component was introduced (the task permits "the existing project registration mechanism").
