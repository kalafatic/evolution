# EVO System Security & Authorization Boundaries

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/evo-system-security.md`

---

## 1. Cross-Component Security Boundaries

- **Database-Backed RBAC**: `AuthorizationService` enforces permissions across principal types (`USER`, `ROLE`).
- **Explicit DENY Priority**: Explicit `DENY` policies in `ResourceAccessPolicy` override role-based `ALLOW` grants.
- **Audit Trails**: All security-relevant actions (`USER_CREATED`, `SESSION_TERMINATED`, `POLICY_CREATED`) are logged into `AuditLogRepository`.
- **System Principal**: Background process executions execute under dedicated `SYSTEM` credentials.
