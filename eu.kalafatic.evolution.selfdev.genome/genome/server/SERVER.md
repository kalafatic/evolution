# Evolution Server & Communication

## Overview
The Evolution Server provides a REST-based interface to the EVO platform, allowing external tools, web-based UIs, and autonomous supervisors to interact with the kernel.

## Architecture
- **Server Core**: Built on `NanoHTTPD` for lightweight, embeddable performance.
- **Authentication**: Cookie-based and Token-based (JWT) security via `AuthService`.
- **Session Persistence**: Backed by an SQLite database (`sessions` table), tracking workflow types and metadata.

## Key API Endpoints

### Session Management
- `POST /api/login`: Authenticates user and returns session ID.
- `GET /api/session/status`: Returns the current state of the kernel and active workflow.

### Orchestration
- `POST /api/orchestrate`: Submits a new goal to the `IterationManager`.
- `GET /api/tasks`: Retrieves logs and results of executing tasks.

### Feedback Loop
- `POST /api/provideInput`: Resumes a session waiting for user clarification.
- `POST /api/provideApproval`: Approves or rejects a proposed variant.

## Supervisor Integration
A separate "Process Watchdog" (Supervisor) communicates with the server via file-system polling or REST to ensure platform health and coordinate self-restarts during Self-Dev Mode.

## Future: WebSocket Migration
To eliminate the ~2s polling latency, the platform is migrating toward a WebSocket-based control plane for real-time telemetry and command propagation.

## Related
- [ARCHITECTURE.md](../architecture/ARCHITECTURE.md)
- [ROADMAP.md](../roadmap/ROADMAP.md)
