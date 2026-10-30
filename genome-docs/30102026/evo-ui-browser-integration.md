# SWT Browser Lifecycle & JavaScript Bridge Safety

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-ui-browser-integration.md`

---

## 1. Thread Safety & Disposed Widget Protection

- **UI Thread Dispatch**: All SWT widget updates originating from background thread events or async callbacks execute safely via `Display.asyncExec(...)` or `Display.getDefault().asyncExec(...)`.
- **Disposed Widget Guard**: Every UI update checks `!control.isDisposed()` prior to widget or browser method invocation.

---

## 2. BrowserFunction Function Registration Guards

To prevent Edge IPC timeout exceptions when reloads occur, `BrowserFunction` instances across `ArchitecturePage`, `ChatGroup`, `ForgePage`, and `SafeBrowserWrapper` enforce registration guards using `AtomicBoolean` or `bridgesInstalled` flags.
