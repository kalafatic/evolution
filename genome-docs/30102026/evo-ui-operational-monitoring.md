# EVO UI Operational Monitoring Across Subsystems

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-ui-operational-monitoring.md`

---

## 1. Subsystem Monitoring Capabilities

| Subsystem | UI Page / Group | Operational Monitoring Capabilities |
| :--- | :--- | :--- |
| **Darwin Engine** | `IterationPage`, `AiCommanderPage` | Iteration progress, active variant status, evolution flow steps (PLAN -> CODE -> TEST -> SCORE -> SELECT -> MERGE), decision gate prompts, session-isolated logs. |
| **Forge Lab** | `ForgePage`, `viz-training.js` | Live training loss graph, model capacity graph, step/epoch metrics, tokens/sec, ETA, system memory/CPU utilization. |
| **Self-Development** | `DevelopmentPage`, `SupervisorGroup` | 18-stage task graph execution, build output logs, supervisor status badges, PID, API status, and deployment controls. |
| **Server Platform** | `ServerPage`, `AdminPage` | Active HTTP client sessions, RBAC resource policies, audit log entries, server health metrics. |
