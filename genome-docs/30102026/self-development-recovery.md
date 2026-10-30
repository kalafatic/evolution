# Self-Development Failure Classification, Retry & Recovery Policy

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/self-development-recovery.md`

---

## 1. Failure Classification & Bounded Retries

- **Transient Errors**: Network connection timeouts or brief HTTP 503 server busy responses are retried up to 3 times with exponential backoff (500ms initial delay).
- **Permanent Errors**: Security software execution blocks (Windows Defender 0x800700e5), corrupt JAR files, missing configuration files, or HTTP 401/403 auth failures fail fast without endless retry loops.

---

## 2. Supervisor Monitoring & Crash Recovery

`SelfDevSupervisor` runs independently on ports 8089/28080. If the external target EVO RCP process crashes or terminates unexpectedly:
1. `SelfDevSupervisor` detects process exit via PID monitoring.
2. Captures exit code and stderr/stdout logs into `SessionLogger`.
3. Marks current task state as `RUNNING_FAILED`.
4. Executes automated Git rollback (`GitManager.rollback()`) to restore source working tree to last known good commit.
