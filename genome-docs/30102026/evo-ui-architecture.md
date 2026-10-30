# EVO UI & Observability Architecture

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-ui-architecture.md`

---

## 1. Overview & One Execution, Multiple Views Principle

In EVO RCP, a running process (Darwin execution, Forge training job, Self-Development loop, or Server operation) has exactly **one authoritative backend lifecycle**. UI views (`MultiPageEditor`, `AiCommanderPage`, `ForgePage`, `DevelopmentPage`, `IterationPage`, `TaskStackPage`, `ArchitecturePage`) act strictly as observers or command issuers.

---

## 2. MultiPageEditor Tab Topology

```
MultiPageEditor
├── Forge Models (ForgePage)
├── Development (DevelopmentPage)
├── Iterations (IterationPage)
├── Tasks (TaskStackPage)
├── Server (ServerPage)
├── MCP Settings (McpSettingsPage)
├── Browser (BrowserPage)
├── Tests (TestsPage)
├── Context & Memory (ContextPage)
├── Approvals (ApprovalPage)
├── Peer Review (PeerReviewPage)
├── Graph (GraphPage)
├── Tools (ToolsPage)
├── Properties (PropertiesPage)
├── Settings (SettingsPage)
├── Administration (AdminPage)
├── Compare (ComparePage)
└── Editor (NestedTextEditor)
```

---

## 3. Observational Tab Switching

Switching tabs inside `MultiPageEditor.pageChange(int newPageIndex)` invokes `refreshUI()` / `scheduleRefresh()` on the active page without restarting background processes or clearing session context.
