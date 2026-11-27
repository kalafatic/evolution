# Repair: Forge Models Session Selection & Training Monitor Visualization

> Repository: `eu.kalafatic.evolution` — `/home/petr/git/evolution-221126`

---

## 1. Root cause — session selection

The Forge Models page (`ForgePage`) renders `forge.html` in an SWT Browser and pushes the "active
session" into it via `window.initializeForge` / `window.updateActiveSession`. The active session was
resolved with a **conflicting priority**:

```java
String activeTrainingSession = ForgeSessionManager.getInstance().getActiveTrainingSessionId();
if (activeTrainingSession != null && !empty) {
    activeAiSession = activeTrainingSession;          // Forge UUID namespace
} else if (editor.getAiChatPage() != null) {
    activeAiSession = aiPage.getCurrentSessionName(); // AI Commander ChatSession name
}
```

`getActiveTrainingSessionId()` returns a **Forge** session key (a `ForgeSession.sessionId` UUID, or a
trainer/telemetry key), while `getCurrentSessionName()` returns the **AI Commander** `ChatSession.id`
name. These are two different namespaces. Prioritising the Forge key means the page could select a
foreign/stale Forge session instead of the authoritative AI Commander session the user is actually
working in.

Two further JS defects compounded it:

- `forge.html` `loadSessions()` created a **synthetic** entry for an unmatched AI Commander session with
  a hardcoded `status: 'TRAINING'` that was only ever *upgraded* (never downgraded), so an idle AI
  Commander session permanently displayed as "TRAINING".
- `forge.html` `updateActiveSession()` used `activeSessionId === 'Default'` as a proxy for "not yet
  selected". This mis-handles a session literally named `Default` and fails to follow AI Commander
  session changes once the user has manually selected a Forge session.

## 2. Root cause — missing Training Monitor graph

`viz-training.js` `renderTrainingViz` is correct: it draws the loss SVG only when
`metrics.history.length > 1` and otherwise shows an informative empty/placeholder state. The "graph not
rendered" is therefore **not** a chart bug — it is a consequence of the session bug: with a wrong/empty
`activeSessionId`, `refreshViz()` fetches `/forge/session/{id}/training/metrics` for the wrong session,
returns empty history, and the chart shows the placeholder instead of real data. **Both problems share
the same underlying session-resolution cause.**

---

## 3. Files modified

| File | Change |
|---|---|
| `eu.kalafatic.evolution.view/.../editors/pages/ForgePage.java` | `refreshUI()` — AI Commander session is now the authoritative primary source; Forge training session is only a fallback |
| `eu.kalafatic.evolution.controller/.../orchestration/forge.html` | `loadSessions()` synthetic session starts `IDLE`/`CREATED`; `updateActiveSession()` follows the AI Commander session only on actual change |

### 3.1 `ForgePage.refreshUI()` (session synchronization)

Resolves the active session as: **AI Commander `getCurrentSessionName()`** → active-training session →
self-dev session → `"Default"`. This makes the AI Commander session the authoritative context, while
keeping the Forge training session as a secondary fallback when no AI Commander session is available.

### 3.2 `forge.html` — `updateActiveSession()`

```js
const aiChanged = !!(activeAi && activeAi !== activeAiSession);
...
if (aiChanged) activeSessionId = activeAiSession;
loadSessions();
```

The Forge page now follows the AI Commander session **only when it changes**, preserving the user's
manual selection during periodic refreshes, and correctly handles a session literally named `Default`.

### 3.3 `forge.html` — synthetic session status

An unmatched AI Commander session is now created as `{ status: 'IDLE', lifecycleState: 'CREATED' }`;
the existing live-metrics probe still upgrades it to `TRAINING` when real training telemetry exists.

---

## 4. Data flow (session → Training Monitor)

```
AiCommanderPage.getCurrentSessionName()         (ChatSession.id — authoritative)
  -> ForgePage.refreshUI()  -> window.initializeForge / updateActiveSession
  -> forge.html activeSessionId
  -> GET /forge/session/{id}/training/metrics   (ForgeSessionManager.getTelemetry(id))
  -> renderTrainingViz(metrics, events)         (viz-training.js)
  -> loss/val-loss SVG (renders when history.length > 1; else empty state)
```

`ForgeSessionManager.getTelemetry(id)` resolves by `findSessionById` then `findSession` (by name) and
falls back to the raw id as the telemetry key, so the AI Commander name (`Default`) correctly keys the
training telemetry registered by `LLMDarwinEngine` via `registerActiveTrainer(context.getSessionId(), …)`.

---

## 5. Verification

- **Compilation**: `ForgePage.java` compiles cleanly (`javac --release 21`, RC=0) against the module
  classpath + Eclipse/OSGi bundles.
- **JS**: edits are small object-literal / function-body changes, verified by inspection (Node 18 is
  present but the inline JS is not extracted for `--check`; the edits are syntactically trivial).
- **Existing tests**: `ForgeSessionManagerIncrementalTest`, `ForgeTrainingIntegrityAndSyncTest`,
  `ForgeTelemetryLrHistoryTest` cover the (unchanged) `ForgeSessionManager` telemetry/registration
  paths, including `registerActiveTrainer`/`getTelemetry`/`getActiveTrainingSessionId`.
- **Full Tycho build**: not executable — pre-existing `Unknown OSGi execution environment:
  'JavaSE-25'` (unrelated to this change).

---

## 6. Remaining limitations

1. Runtime verification in the actual EVO RCP application was not performed (headless environment); the
   end-to-end workflow (open AI Commander → open Forge → verify auto-selection → Training Monitor chart)
   requires the running RCP product.
2. The full Tycho build remains blocked by the pre-existing `JavaSE-25` execution-environment mismatch.
3. `forge2.html` / `forge3.html` (unused legacy copies) were intentionally left unchanged.
