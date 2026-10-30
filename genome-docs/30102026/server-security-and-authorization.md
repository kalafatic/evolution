# EVO Server Platform Security & Authorization Architecture

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/server-security-and-authorization.md`

---

## 1. Authentication & RBAC Model

- **Database Persistence**: SQLite tables (`users`, `sessions`, `roles`, `permissions`, `resource_access`, `audit_log`) initialized by `DatabaseManager`.
- **BCrypt Password Hashing**: `BCryptUtils` provides secure salted password verification.
- **Authorization Service**: `AuthorizationService` enforces permissions across principal types (`USER` and `ROLE`).
- **Explicit DENY Policies**: In `ResourceAccessPolicy`, explicit `DENY` policies take absolute priority over role-based `ALLOW` grants.
- **Audit Logging**: `AuditLogRepository` records sensitive administrative and access actions (`USER_CREATED`, `SESSION_TERMINATED`, `POLICY_CREATED`).
