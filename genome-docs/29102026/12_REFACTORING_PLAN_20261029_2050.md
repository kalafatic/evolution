# REFACTORING PLAN REPORT

**Document Type:** Universal Refactoring Plan & Verification Strategy
**Project:** EVO / Evolution
**Analysis Timestamp:** 2026-10-29T20:50:00Z
**Directory Location:** `genome-docs/29102026/12_REFACTORING_PLAN_20261029_2050.md`
**Git Branch:** `jules-5933475433298650312-6c1d7316`

---

## 1. REFACTORING PRIORITIES & JUSTIFICATION

Based on deep source analysis of key core processes, the refactoring and stabilization strategy focuses on preserving working architectural behavior while verifying critical safeguards across the four core processes:

### 1.1 Priority 1: Self-Dev Network Access & Pre-Launch Hardening
* **Target Files:** `NetworkAccessManager.java`, `WindowsNetworkAccessProvider.java`, `EvoRcpRuntime.java`
* **Objective:** Ensure pre-launch network access rule verification executes gracefully across non-admin and security-restricted environments without blocking application startup.
* **Scope:** Validate loopback fallback logic (`127.0.0.1`) and executable permission checks.

### 1.2 Priority 2: Forge Dataset Acquisition Guard & Validation
* **Target Files:** `ForgeOrchestratorImpl.java`, `HuggingFaceDatasetSource.java`, `EvoModelValidator.java`
* **Objective:** Ensure pre-training byte target guards (`isTargetReached()`) fail fast on data acquisition shortfalls and model validation detects NaN/Inf tensor values before export.
* **Scope:** Confirm multi-tier HTTP fallbacks and pre-export model sanity verification.

### 1.3 Priority 3: Genome Milestone Staging & Inventory Atomicity
* **Target Files:** `MilestoneGenerator.java`, `SelfDevGenomeHub.java`, `FunctionalityInventoryManager.java`
* **Objective:** Preserve atomic staging directory swaps (`genome/history/`) and ensure zero-change scans produce idempotent status outcomes (`GenomeUpdateStatus.UNCHANGED`).
* **Scope:** Verify SHA-256 hash inventory matching and Stage 8 functionality inventory persistence.

### 1.4 Priority 4: Workbench Tab Synchronization & SWT UI Thread Safeguards
* **Target Files:** `MultiPageEditor.java`, `ProcessAdapterFactory.java`, `EvoTableViewerHelper.java`
* **Objective:** Ensure tab switching in `MultiPageEditor` seamlessly re-synchronizes active AI sessions across views off the SWT UI thread.
* **Scope:** Verify `Display.asyncExec()` wrapping and `EvoTableViewerHelper` utilities.

---

## 2. VERIFICATION STRATEGY & ACCEPTANCE CRITERIA

1. **Self-Dev Launch:** Pre-launch network access checks must return `success = true` (either via firewall rule creation or loopback fallback) without throwing exceptions.
2. **Forge Training Guard:** Pre-flight validator must reject training jobs when dataset acquisition yields less than `requestedMinimumUsableBytes`.
3. **Genome Atomicity:** Snapshot creation must write dated artifacts under `genome/history/YYYY/YYYY-MM-DD/timestamp/` without corrupting live `genome/current/` state.
4. **Build & Test Pass:** The entire reactor must build and pass unit test compilation (`mvn test`) cleanly.
