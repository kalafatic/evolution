# ADMINISTRATION UI VERIFICATION
Cognitive Services administration page.

## 1. Implementation state

- Page `CognitiveAdminPage` (view bundle) registered in `MultiPageEditor`
  as "Cognitive Services" (next to "Administration").
- HTML/JS view `cognitive.html` (controller bundle) with seven sections:
  Overview, Applications & Access, Memory Administration,
  Conversations & Sessions, Context Inspector, Validation Administration,
  Audit & Diagnostics.
- Bridge (`cognitiveAction`) dispatches exclusively to native services:
  IdentityService, UserService, ValidationPolicyService, MemoryService,
  SessionContinuityService, CognitiveContextService, DatabaseManager
  (audit reads). No database logic in UI code; no mock data.

## 2. Verification status

- Build: CognitiveAdminPage + cognitive.html present in the deployed
  product bundles (verified by jar inspection of the materialized product).
- Runtime: the page's backing services were verified LIVE in the same EVO
  process through MCP (overview counters from the same DB, applications +
  token issuance via IdentityService, memory search/retract via
  MemoryService, sessions via SessionContinuityService, context_get via the
  CognitiveContextService facade, validation state from the runtime DB,
  audit rows present).
- Click-through UI automation: NOT performed — no UI automation tooling
  (xdotool/screenshot) exists on the host; the SWT browser page itself was
  not exercised interactively. This is the remaining UI verification gap.

## 3. Section mapping to verified service behavior

| Section | Backing service | Verified how |
|---|---|---|
| Overview | IdentityService/listApplications, SessionContinuityService.countSessions, MemoryService.getEntryCount, conversation_messages count, validation counters, audit_log | live DB values match the MCP-visible state |
| Applications & Access | IdentityService register/issue/revoke + UserService | token issuance/revocation verified live (seeded fixtures + revocation rejection) |
| Memory | MemoryService.search + retractEntry | live MCP memory lifecycle |
| Sessions | SessionContinuityService.listSessions | live session_search |
| Context Inspector | CognitiveContextService.getContext (same facade as MCP context_get) | live context_get — identical code path |
| Validation | ValidationPolicyService (global switch, rules, history) | runtime DB state (default DISABLED, security rules fixed) + unit tests for all three modes |
| Audit | audit_log reads | 393 live audit rows |

## 4. Notes

- All bridge calls run on the SWT UI thread via BrowserFunction (the
  established AdminPage pattern); the operations are short service/DB calls.
- Administrator-only access: the page operates with the admin principal;
  stricter UI role-gating remains a documented hardening item.
