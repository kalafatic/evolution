# EVO Server Platform Architecture & Subsystem Analysis

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/server-platform-architecture.md`

---

## 1. Executive Overview

`EvolutionServer` (built on `NanoHTTPD`) acts as the central asynchronous capability server for EVO. It integrates authentication, RBAC authorization, SQLite database persistence, session tracking, REST endpoints, and MCP tool execution.

---

## 2. Server Request Processing Architecture

```
CLIENT REQUEST
  ↓
EVOLUTION SERVER (NanoHTTPD)
  ↓
CORS & HEADER VALIDATION
  ↓
AUTHENTICATION CONTROLLER (AuthController / AuthService)
  ↓
RBAC AUTHORIZATION SERVICE (AuthorizationService)
  ├── Explicit DENY Evaluation
  └── Role-based Permission Grants
  ↓
CONTROLLER ROUTING
  ├── /api/auth/*       → AuthController
  ├── /api/admin/*      → AdminApiController
  ├── /task             → OrchestratorServiceImpl
  ├── /mcp              → McpServer & JsonRpcDispatcher
  └── Static Resources  → StaticResourceController
  ↓
PERSISTENCE & AUDIT (DatabaseManager, SessionRepository, AuditLogRepository)
```
