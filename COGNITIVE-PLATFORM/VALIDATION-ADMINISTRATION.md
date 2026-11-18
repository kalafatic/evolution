# VALIDATION ADMINISTRATION
Central validation policy — configuration and operation

## 1. Model

- `ValidationPolicyService` (servers bundle) owns the policy: a catalog of
  `ValidationRule`s grouped by `ValidationCategory`, each with an effective
  `ValidationMode` (DISABLED / WARN / STRICT), enablement flag, severity,
  last result, last check timestamp, warning and error counters.
- Default installation: **Strict Application Validation = OFF** (global rule
  `global.strict` mode DISABLED; all domain rules default DISABLED).
- Configuration persists in SQLite (`validation_policy_config`), every change
  is written to `validation_policy_history` with the actor and reason, and
  applies to new operations immediately (per-request policy reads; no change
  mid-operation).

## 2. Categories and rules (implemented catalog)

| Category | Rules |
|---|---|
| USER_PROFILE | displayName, locale, workingHints |
| USER_PREFERENCES | confidence, expiration, contradiction |
| APPLICATION_METADATA | description, kind, owner |
| SESSION | projectRef, ownerRef, parentRef |
| CONVERSATION | toolRecords, messageRoles, compaction |
| MEMORY | contentRequired, classification, relationships, confidenceRange, provenance |
| PROJECT_KNOWLEDGE | componentSummaries, decisionStatus, buildRefs |
| CONTEXT | categoriesComplete, budgetFits, sourcesAnnotated |
| KNOWLEDGE_RELATIONSHIPS | relatedTargets, supersessionChain |
| DATA_COMPLETENESS | requiredFields, metadataQuality |
| SECURITY | authentication, authorization, applicationIdentity, userDataAccess, appIsolation — ALWAYS STRICT, can never be disabled |

## 3. Enforcement semantics

- `evaluate(ruleId, check)`: DISABLED → returns ok (no check executed);
  WARN → check executed, failures recorded (warnings counter); STRICT →
  failures flagged blocking.
- `enforce(ruleId, check)`: throws `ValidationException(ruleId, message)`
  when a STRICT rule fails.
- The context pipeline runs `CONTEXT.categoriesComplete`, `CONTEXT.budgetFits`
  and `CONTEXT.sourcesAnnotated` through the policy (default: skipped).
- Security checks are outside the policy and always enforced.

## 4. Administration UI (Cognitive Services → Validation Administration)

Available in the EVO RCP "Cognitive Services" page:

- Global Strict Validation switch (Enable STRICT / Disable).
- Category overview with every rule: mode, status, last result, warning/error
  counters.
- Per-rule mode configuration (DISABLED/WARN/STRICT dropdown); security-fixed
  rules show "cannot be disabled".
- Run Validation Checks (executes registered rule implementations).
- Restore Default Configuration (with history entry).
- Configuration change history table (timestamp, rule, old → new mode, actor).
- Clear indication that security controls can never be disabled.

Backing: native `ValidationPolicyService` through the page bridge — no
database logic in UI code; only administrators should access the page (the
page operates with the admin principal; further UI-level role gating is a
deferred hardening item).

## 5. REST administration API (existing AdminApiController extensions)

- `GET  /api/admin/validation` — categories + rules + global switch
- `POST /api/admin/validation/global` — {strict: bool}
- `POST /api/admin/validation/rules/{ruleId}` — {mode} or {enabled}
- `GET  /api/admin/validation/history`
- `POST /api/admin/validation/reset`
- `POST /api/admin/validation/check-all`

## 6. Future activation

Enabling STRICT globally, per category or per rule requires no source changes:
it is a persisted configuration change made by an authorized administrator.
