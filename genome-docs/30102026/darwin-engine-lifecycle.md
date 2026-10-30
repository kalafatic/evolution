# Darwin Engine Lifecycle & State Transition Authority

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/darwin-engine-lifecycle.md`

---

## 1. Lifecycle State Machine

`ADarwinEngine` manages process instances across seven explicit states via `IterationManager`:

| State | Purpose | Transition Trigger |
| :--- | :--- | :--- |
| `INIT` | Resets state holders, clears stale tasks, initializes kernel. | Start of new request or goal reset. |
| `ANALYZING` | Scans repository, performs intent expansion and reality discovery. | Transition from `INIT`. |
| `EXECUTING` | Spawns variant candidates and executes parallel evaluation. | Discovery completion or manual resume. |
| `VERIFYING` | Evaluates static analysis, compilation, and reality gates. | Variant task execution completion. |
| `WAITING_FOR_USER_DECISION` | Pauses engine thread (`approvalLock.wait()`) for variant selection. | Proposal generation in manual mode. |
| `DONE` | Terminal success state. | Winner materialization & merge complete. |
| `FAILED` | Terminal error state. | Unrecoverable error or maximum retries exceeded. |

---

## 2. Decision Authority & Approval Mechanics

1. **Auto-Approve Mode**: Selects the candidate with the highest fitness score (`variants.stream().max()`).
2. **Manual Mode**: Emits `[DARWIN_BRANCHES]` event payload to UI, registers pending candidates in metadata, transitions state to `WAITING_FOR_USER_DECISION`, and blocks the execution thread on `approvalLock.wait()`.
3. **Resumption**: Client selection or auto-approve toggle invokes `resumeWithWinner` / `resumeAutoApprove`, releasing `approvalLock.notifyAll()` and resuming the evolution thread seamlessly.
