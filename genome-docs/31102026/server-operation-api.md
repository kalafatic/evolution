# Server Asynchronous Operation API Specifications

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/server-operation-api.md`

---

## 1. Asynchronous Execution Endpoints

### 1.1 Submit Operation
- **Endpoint**: `POST /task`
- **Request Body**:
```json
{
  "prompt": "Optimize data parsing logic",
  "sessionId": "session-uuid",
  "model": "local"
}
```
- **Response**: Immediate `HTTP 200/202` with operation status:
```json
{
  "id": "session-uuid",
  "status": "RUNNING"
}
```

### 1.2 Poll Operation Progress & Conversation
- **Endpoint**: `GET /server/conversation/:sessionId`
- **Response**: Array of incremental conversation and progress events including thinking traces, branch proposals, and final response text.

### 1.3 Provide User Decision / Approval
- **Endpoint**: `POST /task` with control command:
```json
{
  "prompt": "select v-1730000000",
  "sessionId": "session-uuid"
}
```
`OrchestratorServiceImpl` detects `WAITING_FOR_USER_DECISION` and immediately routes the command to `ADarwinEngine.handleUserDecision()`.
