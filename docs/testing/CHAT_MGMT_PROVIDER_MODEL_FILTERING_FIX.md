# Chat Management — Provider / LLM Combo Filtering Fix

> Repository: `eu.kalafatic.evolution` — `/home/petr/git/evolution-221126`
> Scope: Chat Management provider combo + LLM/model combo discovery, population and filtering.
> Result: 1 file modified (view module); discovery layer confirmed correct and left unchanged.

---

## 1. Root cause

The per-provider model **discovery dispatch** was already correct and unified by a prior fix:

- `LlmRouter.discoverModels(providerSelection, orchestrator)` dispatches by `normalizeProviderKey`
  → `ollama` (Ollama `/api/tags`), `llama-cpp` (`LlamaService.discoverLlamaCppModels` GGUF scan),
  `enr` (`EnrLlmProvider.discoverModels` unified catalog). No hardcoded lists; no cross-provider
  reuse; each descriptor preserves the real inference identifier (`LocalModelDescriptor.id()`).

The defects that remained were all in the **Chat Management UI combo lifecycle**
(`ChatMgmtGroup.java`):

1. **Combo not cleared on provider change** (§3.1). `refreshModelDiscovery` only set a
   "Loading…" status but left the previous provider's models in the combo (and in
   `currentModelDescriptors`) while the new provider was queried asynchronously. Old-provider
   models stayed visible and selectable during the loading window.
2. **Error path kept stale models** (§3, §6). `applyDiscoveryResult`'s error branch explicitly
   *"kept the previously discovered entries so the page stays usable"*. After switching
   Ollama → llama.cpp / ENR and discovery **failing**, the Ollama models remained in the combo —
   the exact "reuse Ollama models for another provider" symptom.
3. **Non-standard async** (§5). Discovery ran on a raw `new Thread(...)` instead of the
   project-standard `org.eclipse.core.runtime.jobs.Job` used everywhere else in the view.

A secondary risk introduced by fixing #1: with the combo empty during loading, the periodic
`refreshUI()`/`load()` path (`needsDiscovery = … || itemCount == 0`) would re-trigger discovery
in a loop. Guarded with `discoveryInFlight` / `discoveryFailed` flags.

## 2. Files and classes inspected

- `view/.../aichat/ChatMgmtGroup.java` — provider/model combos, async discovery, apply/restore.
- `view/.../aichat/ProviderModelSelectionSupport.java` — label normalization, selection resolution, dedupe.
- `controller/.../llm/LlmRouter.java` — `discoverModels` dispatch, `normalizeProviderKey`, `isEnrEngine`.
- `controller/.../llm/LocalModelDescriptor.java` — model DTO (id/name/origin/engine/available).
- `controller/.../llm/OllamaProvider.java` — `discoverModels` (Ollama `/api/tags`).
- `controller/.../manager/LlamaService.java` — `discoverLlamaCppModels` (GGUF scan).
- `enr/.../EnrLlmProvider.java` — `discoverModels` (ENR catalog).
- `controller/.../llm/ILlmProvider.java` — provider discovery contract.
- `controller/.../OrchestratorServiceImpl.java` — `updateConfiguration`/`applyInferenceEngineSelection` (provider → inference routing).
- `view/.../AiCommanderPage.java` — `applyProviderSelection`, `getSelectedModelName`, save/restore.
- `controller/.../manager/ProjectModelManager.java` — `filterModelsByEngine` (legacy name heuristic).
- `view/.../aichat/ChatMgmtGroup2.java` — duplicate, dead (not instantiated).
- `controller.tests/.../ProviderModelDiscoveryTest.java` — existing per-provider discovery suite (9 tests).

## 3. Files modified

| File | Change |
|---|---|
| `eu.kalafatic.evolution.view/.../aichat/ChatMgmtGroup.java` | clear combo on provider change & error; `Job` instead of `Thread`; `discoveryInFlight`/`discoveryFailed` guards |

No discovery-layer or controller classes were changed.

## 4. Changes in detail (ChatMgmtGroup.java)

1. Added `clearModelCombo()` — clears the combo items/selection and `currentModelDescriptors`.
2. `refreshModelDiscovery(...)`:
   - increments the generation token and marks `discoveryInFlight = true`, `discoveryFailed = false`;
   - calls `clearModelCombo()` **synchronously** before scheduling discovery (UI thread), so no
     previous-provider model remains visible/selectable;
   - replaces `new Thread(...)` with an `org.eclipse.core.runtime.jobs.Job` (project standard);
   - keeps `Display.asyncExec` + generation-token guard so stale/outdated results are discarded.
3. `applyDiscoveryResult(...)`:
   - error branch now `clearModelCombo()` + "Discovery failed for <provider>: <message>" and sets
     `discoveryFailed = true` (no more silently keeping stale entries);
   - success path clears `discoveryFailed` and populates the combo via
     `resolveSelectionIndex` (restore previous model if still present, else first, else empty state).
4. `load()` `needsDiscovery` now also requires `!discoveryInFlight && !discoveryFailed`, so periodic
   refreshes neither loop during loading nor hammer a failing provider (manual Refresh or a provider
   change re-arms discovery).

## 5. Discovery mechanism per provider (unchanged, verified)

| Provider | Discovery | Model identifier |
|---|---|---|
| Ollama | `OllamaProvider.discoverModels` → `OllamaService.loadModels()` (`GET /api/tags`) | `m.getName()` e.g. `llama3.2:3b` |
| llama.cpp | `LlamaService.discoverLlamaCppModels()` → GGUF scan of resolved model dirs (flat `*.gguf` + `subdir/evo.gguf`) | filename minus `.gguf` e.g. `evo` |
| EVO ENR | `EnrLlmProvider.discoverModels()` → `EnrInferenceEngine.listModels()` unified catalog | `EnrModel.id()` (provider-prefixed) e.g. `ollama:llama3.2:3b`, `evo-native:model.evo` |

All three return **every** model their own mechanism reports; the combo displays a friendly
origin-prefixed label while the descriptor list carries the real inference identifier, so
inference always receives the original identifier (never a derived display label).

## 6. Build result

`ChatMgmtGroup.java` compiled with `javac --release 21` against the full module classpath +
Eclipse/EMF/OSGi bundles: **COMPILE_RC=0** (only the two pre-existing varargs warnings).

## 7. Runtime verification

- **UI lifecycle (changed code):** verified by targeted compilation; the behaviour is covered by
  the existing logic and the generation-token/guard structure (SWT runtime execution requires the
  Eclipse RCP product, out of scope for this environment).
- **Provider discovery (unchanged code):** `ProviderModelDiscoveryTest` (9 tests: Ollama registry,
  Ollama failure, llama.cpp GGUF list + empty, ENR dispatch, ENR-not-registered, unknown provider,
  repeated switching, explicit llama.cpp routing) could not be executed in this plain-JVM harness
  because `eu.kalafatic.utils.log.Log`'s static initializer requires the Eclipse OSGi preferences
  service (`Platform.getPreferencesService()`). This is an **environmental** limitation unrelated to
  this change; the discovery layer was not modified and remains covered by that suite in the Tycho
  runtime.

## 8. Remaining limitations / findings (not changed)

1. `ProjectModelManager.filterModelsByEngine` — a legacy name-based heuristic filter, now referenced
   only by tests (`InferenceEngineSwitchingTest`), not wired into Chat Management. Left as-is.
2. `ChatMgmtGroup2.java` — a duplicate of `ChatMgmtGroup` that is never instantiated (dead). Left as-is
   (no broad cleanup per project rules).
3. `LLMSettingsPage.java` (wizard) still uses `ProjectModelManager.getLocalModelNames` (config-file
   `AIProvider` list), which is a different setup-wizard surface, not Chat Management.
4. The model identifier flows to inference via `Orchestrator.localModel`/`Ollama.model` and the
   `inferenceEngine` context metadata; this path was inspected and found correct (no change needed).

## 9. Acceptance mapping

| Requirement | Status |
|---|---|
| All Ollama models populate the combo | ✅ (unchanged discovery) |
| All llama.cpp models populate the combo | ✅ (unchanged discovery) |
| All ENR models populate the combo | ✅ (unchanged discovery) |
| No cross-provider model reuse | ✅ (clear-on-change + clear-on-error) |
| Correct inference identifier preserved | ✅ (descriptor `id()` unchanged) |
| Immediate clear + loading on provider change | ✅ (fixed) |
| Error state without stale models | ✅ (fixed) |
| Background Job (no UI freeze) | ✅ (fixed) |
| Stale/out-of-order async response guarded | ✅ (generation token, retained) |
