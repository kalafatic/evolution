# EVO — Unified RCP / Web UX Standard — Delivery Report

Date: 2026-11-21

## 1. Executive summary

EVO already implemented the Family UX Standard v1.0 (code-built main menu with
Evolution/File/Edit/Server/Explorer/Tools/Window/Help, Tools → Open Terminal / Open
Web Interface with real port resolution, Help menu with all six standard entries,
F1 context help via the served `help.html`, a global RCP context-menu manager, a
web dashboard with a right-click contextual-action menu + Help button, and a
table-driven headless CLI). The gap against the mandatory CLI vocabulary was
`/models`, `/providers`, `/agents` — these are now implemented against live
capability data instead of being intentionally absent.

## 2. Existing infrastructure reused

- `HeadlessCli` single command table (dispatch + generated help) — extended, not
  replaced.
- `AiProviders` / `ProviderConfig` (configured LLM providers) — real provider data
  for `/providers` (API keys are never printed).
- `ConnectedApplicationRegistry` / `ConnectedApplicationDescriptor` — real coding
  harness/agent data for `/providers` (harness section) and `/agents`.
- `GET /server/status` `ollama` section + `OllamaManager`/`OllamaService.loadModels()`
  — real model data for `/models` (server-reported first, in-process fallback).
- Contextual Action Contract v1.0 (RCP + Web) — untouched.

## 3. Components modified

| File | Change |
|---|---|
| `eu.kalafatic.evolution.controller/src/.../application/HeadlessCli.java` | Added `models`, `providers`, `agents` commands with handlers and topic help; new imports for the live data sources. |
| `eu.kalafatic.evolution.controller/src/.../orchestration/help.html` | CLI table: added `/models /providers /agents` rows; replaced the "intentionally not provided" note with the real data-source description. |
| `eu.kalafatic.evolution.controller/META-INF/MANIFEST.MF` | Exported `eu.kalafatic.evolution.controller.application` with `x-friends:="eu.kalafatic.evolution.controller.tests"` (additive, friends-only export for testability). |
| `eu.kalafatic.evolution.controller.tests/src/.../HeadlessCliTest.java` | **New** CLI contract test (5 tests). |

## 4. New actions

- CLI `/models` — local Ollama models (name, size) from `GET /server/status` with an
  in-process `OllamaService` fallback, plus provider default models.
- CLI `/providers` — configured LLM providers (name, url, local, default model —
  never API keys) + connected coding-harness providers with enabled/status/selected.
- CLI `/agents` — connected coding agents (name, type, enabled, status,
  capabilities, selected) from `ConnectedApplicationRegistry`.

## 5. Context menu coverage (RCP)

Already provided by `EvoGlobalContextMenuManager` (global SWT `MenuDetect` filter)
plus the declarative `popup:org.eclipse.ui.popup.any` contribution and
`OrchestrationEditorActionProvider`:

- Files/folders (Tree/Table selections): Copy Path, Open Terminal in the resolved
  directory; standard Cut/Copy/Paste/Select All per control.
- Forge sessions/processes: Finish, Telemetry, View Details, Stop; contextual
  "Forge Session Actions" submenu from the contextual registry.
- AI assist submenu (Explain/Refactor/Summarize/…), Quick Navigation, Refresh.
- Orchestration/project/task entries: Open, Orchestrate, Edit Task, Rate Task,
  Remove, Refresh, New Evo Project/Task, Maven Build, Git Commit, Setup Ollama/LLM.

No changes were required here; file Create/Rename/Delete operations are not EVO
navigator capabilities and are intentionally not exposed.

## 6. Help coverage

- Help menu → Help Contents, Getting Started, Context Help, Keyboard Shortcuts, CLI
  Commands, Diagnostics, About (web `help.html#anchor` via `HelpLauncher`).
- F1 (no modifiers) → context help anchor per active part (display filter).
- Web dashboard has a Help button; the CLI section of `help.html` now documents the
  full vocabulary including the new commands.

## 7. CLI help coverage

- `/help` and `/help <topic>` generated from the single `COMMANDS` table. Vocabulary:
  `help, status, web, terminal, logs, sessions, models, providers, agents, config,
  stop, start, restart, exit`.
- Unknown commands → prefix + Levenshtein suggestions ("Did you mean").
- `evo>` interactive REPL (`HeadlessApplication.runRepl`) uses the same table.

## 8. Tests executed

- New `HeadlessCliTest` (JUnit 4, in `eu.kalafatic.evolution.controller.tests`):
  **5/5 OK** — executed standalone with
  `org.junit.runner.JUnitCore` against the Tycho-compiled classes
  (mandatory vocabulary, topic help, capability-command output, suggestions, exit).
- Full reactor build `./mvnw -B -P '!all-platforms' -P linux clean verify
  -DskipTests` → **BUILD SUCCESS** (all bundles compile and package, including the
  controller bundle and the tests bundle).
- The full unfiltered test suite could not be used for verification: it hangs in a
  pre-existing live HuggingFace dataset-acquisition loop
  (`Salesforce/wikitext`, `ROWS_API`, same offset retried indefinitely) in the
  Forge data tests — a network/environment issue unrelated to this change.

## 9. GUI behavior verified

- RCP GUI: not verified interactively (no X server on this host). Compilation and
  test coverage only.
- Headless CLI: exercised through `HeadlessCliTest` (dispatch level); the `evo>`
  REPL itself was not run in this session.

## 10. Remaining limitations

- The main menu is defined twice (programmatic `ApplicationActionBarAdvisor` and a
  parallel `menu:org.eclipse.ui.main.menu` contribution in `view/plugin.xml`) —
  pre-existing duplication, left untouched to avoid behavior changes.
- The embedded RCP chat web UI (`view/chat.html`) has no right-click context menu
  (the portal `dashboard.html` does).
- Terminal-emulator probing is duplicated between `EvoDesktopLauncher` (view
  bundle) and `HeadlessCli` (controller bundle); bundling constraints make a shared
  helper invasive, so the duplication was left.
- `org.eclipse.help.ui` remains a legacy (unused) manifest dependency.
