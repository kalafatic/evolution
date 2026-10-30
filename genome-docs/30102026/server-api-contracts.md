# EVO Server REST & MCP API Contracts

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/server-api-contracts.md`

---

## 1. REST Endpoint Specifications

| Method | Endpoint | Auth Required | Purpose | Response Payload |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | No | User authentication & session token creation. | `{ "token": "uuid", "user": "admin" }` |
| `GET` | `/server/status` | No | Health check ping. | `{ "status": "OK" }` |
| `POST` | `/server/project/create` | Yes | Initialize active `EvoProject`. | `{ "status": "OK", "project": "SelfDevProject" }` |
| `POST` | `/task` | Yes | Asynchronous task / inference submission. | `{ "taskId": "t-123", "status": "ACCEPTED" }` |
| `GET` | `/server/conversation/:id` | Yes | Poll session logs and progress. | `{ "messages": [...] }` |
| `GET` | `/api/admin/overview` | Yes (`MANAGE`) | Administrative system overview metrics. | `{ "totalUsers": 1, "activeSessions": 1 }` |
| `POST` | `/mcp` | Yes | MCP JSON-RPC tool dispatch. | `{ "jsonrpc": "2.0", "result": {...} }` |
