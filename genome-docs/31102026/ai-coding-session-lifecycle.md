# AI Coding Session Lifecycle & State Machine

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/ai-coding-session-lifecycle.md`

---

## 1. Coding Session Lifecycle

The coding session progresses through deterministic, verifiable states:
```
CREATED
  ↓
REPOSITORY_ANALYSIS (TargetScanner, ContextCurator, TargetRealityModel)
  ↓
CONTEXT_PREPARATION (PromptSynthesizer, SemanticEnvelope)
  ↓
CODE_GENERATION (DarwinVariantSpawner, isolated Git worktree generation)
  ↓
VALIDATION (RealityLevel LIGHT/MEDIUM reality gates, Maven compile/test)
  ↓
REVIEW_REQUIRED (Populates EMF ReviewSession & ChangeSet)
  ↓
ACCEPTED / REJECTED (PeerReviewPage line-level inspection & decision)
  ↓
COMMIT_PREPARATION (Working tree diff staging & commit message synthesis)
  ↓
COMMITTED (Explicit Git commit via GitTool without automatic push)
```
