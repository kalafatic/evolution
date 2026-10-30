# EVO UI State Synchronization & Observer Lifecycle

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-ui-state-synchronization.md`

---

## 1. Observer Attachment & Snapshot Synchronization

When a user attaches to an active process (e.g. opening `IterationPage` or `ForgePage`):
1. **Initial Snapshot**: The UI fetches an authoritative state snapshot from `SessionManager`, `DarwinEngineRegistry`, or `ForgeSessionManager`.
2. **Event Subscription**: The UI registers as a listener on `RuntimeEventBus`.
3. **Timestamp Guards**: `AEvoPage` maintains `sessionLastTimestamp` per session ID to discard out-of-order or stale snapshot updates (`isStateStale`).

---

## 2. Listener Cleanup & Page Disposal Invariants

Disposing or navigating away from a workbench page removes its event subscriptions (`removeRuntimeEventListener`) without terminating or interrupting background process execution.
