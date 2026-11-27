# Forge Training Monitor – AI Commander Session Propagation Fix

Date: 2026-11-27 · Repository: `/home/petr/git/evolution-221126` (authoritative production repo, **Java 25**)

---

## 1. Root Cause

The Forge Models / Training Monitor page bound its telemetry to the **wrong session identifier**. The chain was:

- The Forge page resolved its "current session" from `AiCommanderPage.getCurrentSessionName()`, which returns the AI Commander **`ChatSession` id** (e.g. `"Default"` or `"yyyyMMdd-HHmm_task"`).
- Training telemetry is recorded by `ForgeSessionManager` under the **`ForgeSession` UUID** (a `xxxxxxxx-xxxx-…` UUID in a different namespace).
- The Training Monitor's REST call `/forge/session/{id}/training/metrics` therefore looked up telemetry under the ChatSession id, which has no telemetry → the monitor rendered the generic empty state ("No Active Training Telemetry").

**Authoritative source of the selected Forge session:** `ForgeSessionManager` (already the single source of truth used by the AI Commander's `ForgeStatusGroup.resolveForgeSessionId()`). The Forge page was not using it.

## 2. Corrections

| File | Change | Reason |
|---|---|---|
| `controller/.../orchestration/ForgeSessionManager.java` | Added public `getCurrentSessionId()` — resolves the canonical Forge session UUID (active training → first non-IDLE telemetry session → default session) | Single authoritative resolution for the Training Monitor to bind to |
| same | `getActiveTrainingSessionId()` now only returns trainers/telemetry whose session still **exists** (`findSessionById != null`) | Prevent stale telemetry from a prior/deleted session leaking into resolution |
| `view/.../pages/aichat/ForgeStatusGroup.java` | `resolveForgeSessionId()` now delegates to `ForgeSessionManager.getCurrentSessionId()` | Eliminates duplicated selection logic |
| `view/.../pages/ForgePage.java` | `refreshUI()` resolves the Forge session via `getCurrentSessionId()` (falling back to the ChatSession id only when no Forge session exists) | Binds telemetry to the correct UUID |
| same | Added a 1 s periodic session-sync timer that re-resolves and propagates via the existing `updateActiveSession` JS bridge (no-op when unchanged) | Propagates AI Commander session changes without duplicate manual selection; self-terminates on browser disposal |
| `controller/.../forge-viz/viz-training.js` | Session-aware empty state + status title (`Live Training Monitoring` / `Training Completed – Historical Metrics` / `Training Failed – …` / `No Training Telemetry Yet` / `No Forge Session Selected`) | Distinguishes "no session" from "session with no history" and shows actual status |
| `controller.tests/META-INF/MANIFEST.MF` | Added `eu.kalafatic.evolution.view.editors.pages.aichat` to `Import-Package` | Fixed a pre-existing test-module compilation error that blocked running the test suite |
| `controller.tests/.../ForgeSessionResolutionTest.java` | Added 3 tests for `getCurrentSessionId()` | Regression coverage (default / non-IDLE / active-training priority) |

No new session manager, registry, or charting framework was introduced. The existing EMF/OSGi/SWT architecture and the existing `TrainingTelemetry` + REST (`/forge/session/{id}/training/metrics` + `/training/events`) telemetry path are reused.

## 3. How the Fix Works

1. **AI Commander owns the selection** — its `ForgeStatusGroup` (and now `ForgeSessionManager.getCurrentSessionId()`) identify the current Forge session UUID.
2. **Forge Models Page receives it** — `ForgePage.refreshUI()` calls `getCurrentSessionId()` and passes the UUID to `window.initializeForge`/`updateActiveSession`.
3. **Training Monitor binds** — the HTML/JS resolves `activeSessionId` to that UUID and fetches `/forge/session/{uuid}/training/metrics`, which returns `TrainingTelemetry.toJsonObject()` (status, currentLoss, history, logStream).
4. **Historical data** — `ForgeSessionManager` persists/restores per-session telemetry (`persistSessionState`/`restorePersistedHistory`); a completed/failed session has non-IDLE status so `getCurrentSessionId()` selects it and its history loads even when training is no longer active.
5. **Live updates** — the existing 1 s auto-refresh (`refreshViz`) re-fetches metrics/events while `status` is RUNNING.
6. **Session switch** — the new ForgePage timer re-resolves every second and propagates a change through `updateActiveSession`, which re-selects and re-fetches the new session's telemetry (old graph cleared, no cross-session mixing).

## 4. Evidence

- **Owning component:** `ForgeSessionManager.getCurrentSessionId()` (canonical `ForgeSession` UUID).
- **Telemetry provider:** `ForgeSessionManager.TrainingTelemetry` → `toJsonObject()` (includes `history[]` of `MetricSample` loss entries) served via `EvolutionServer.handleGetTrainingMetrics`.
- **Persisted data:** `forge-state` directory; `restorePersistedHistory` rehydrates `history`/`logStream` on restart.
- **Graph data source:** `viz-training.js` renders `data.history[].loss`/`valLoss` (actual training values, not demo data).

## 5. Tests

```
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./mvnw -Plinux \
  -pl eu.kalafatic.evolution.controller.tests verify \
  -Dtest=ForgeSessionResolutionTest,ForgeSessionManagerIncrementalTest,ForgeSessionLifecycleRegressionTest,ForgeSettingsDialogLiveMonitorTest,ForgePageResourceIntegrityTest,ForgeDeviceSelectionTest
→ Tests run: 43, Failures: 0, Errors: 0, Skipped: 0   (BUILD SUCCESS)
```

New regression tests (in `ForgeSessionResolutionTest`):
- `testGetCurrentSessionIdReturnsDefaultWhenNoTraining` — PASS
- `testGetCurrentSessionIdPrefersSessionWithNonIdleTelemetry` — PASS (historical graph without active training)
- `testGetCurrentSessionIdPrefersActiveTrainingSession` — PASS (active training takes priority)

Builds (all SUCCESS):
- `./mvnw -o -Plinux -pl eu.kalafatic.evolution.controller,eu.kalafatic.evolution.view -am install -DskipTests`
- `./mvnw -o -Plinux -pl eu.kalafatic.evolution.controller -am install -DskipTests`

## 6. Final Verdict

**Session propagation and telemetry binding repaired.** The Forge Training Monitor now binds to the authoritative `ForgeSession` UUID (not the AI Commander chat-thread id), loads historical telemetry for completed/failed runs, follows live training, propagates session changes within ~1 s, and shows session-aware UI states. Verified by 43 passing tests and successful builds.

**Remaining / not verified at runtime:** the actual SWT browser rendering was not exercised headlessly (the HTML/JS render path is covered by `ForgePageResourceIntegrityTest` for resource integrity, but live visual confirmation in the RCP product is not part of the automated suite).
