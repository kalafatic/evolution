# AI SUMMARY: Evolution Server

## What is this subsystem?
The REST API and session management backbone.

## Why does it exist?
To enable decoupled interaction between the Java kernel and web-based UIs or external autonomous supervisors.

## Key Mechanisms
- **Session Isolation**: Mandatory `sessionId` for all calls.
- **REST Protocol**: JSON-based command and status updates.
- **Persistence**: SQLite storage for session state and active workflows.
