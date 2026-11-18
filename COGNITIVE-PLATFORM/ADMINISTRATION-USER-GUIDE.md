# ADMINISTRATION USER GUIDE
Cognitive Services Administration in EVO RCP

## Opening the page

In the EVO editor, open the **"Cognitive Services"** tab (next to the existing
"Administration" tab).

## Sections

### Overview
Platform status badge, registered applications, active sessions, memory
records, conversation records, validation warnings/errors and the recent
audit trail.

### Applications & Access
- Register applications (id, name, kind: EVO/TRYST/DSTUDIO/DCODE/EXTERNAL_MCP,
  optional owner user).
- Issue tokens: select an application and user, define scopes (comma
  separated), click Issue Token. **The token is displayed exactly once** —
  copy it immediately; only its hash is stored.
- Token grants table: application, user, scopes, status, last used; Revoke
  revokes a grant immediately.
- Registering an application never grants automatic data access — every token
  is bound to a user and a scope list.

### Memory Administration
Search memory records by query, scope and type; inspect content, type,
validity, confidence; Retract marks records RETRACTED (auditable; never
hard-deleted).

### Conversations & Sessions
List registered cognitive sessions with kind, status, owner user,
application, project and start time.

### Context Inspector
Specify user, application, project, session, intent, token budget and
categories; click Reconstruct Context. The result shows the full
ContextPackage: which information was selected, its sources, confidence,
freshness, token estimate, conflicts, warnings and items excluded by the
budget. Essential for diagnosing context quality.

### Validation Administration
See VALIDATION-ADMINISTRATION.md. Global strict switch, per-rule modes,
run checks, restore defaults, change history. Security rules are fixed and
cannot be disabled.

### Audit & Diagnostics
Recent audit entries: time, actor, action, resource, outcome — covering MCP
tool calls, token operations, validation changes and security events.

## Notes

- All operations go through native services; long-running operations are
  handled asynchronously by the platform services.
- Only administrators should operate this page; unauthorized MCP callers are
  rejected at the dispatcher regardless of the page.
