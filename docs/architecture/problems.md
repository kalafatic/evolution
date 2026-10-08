# Architectural Problem Register & Audit Findings

## Architectural Audit Summary

This register details discovered architectural risks, technical debt, and anti-patterns identified during codebase analysis. Issues are categorized by severity (`HIGH`, `MEDIUM`, `LOW`) with evidence, impact, and actionable refactoring recommendations.

---

## Identified Architectural Problems

### 1. `eu.kalafatic.evolution.supervisor` Surefire Test Failure on Broad Test Suites
* **Severity**: **HIGH**
* **Subsystem**: Build System / Supervisor Module (`eu.kalafatic.evolution.supervisor`)
* **Evidence**: Running `mvn test -Dtest=EuKalafaticEvolutionAllTests` fails on module `:eu.kalafatic.evolution.supervisor` because surefire expects matching test classes, whereas `EuKalafaticEvolutionAllTests` is hosted in `eu.kalafatic.evolution.tests`.
* **Impact**: Full reactor global test runs require explicitly excluding `:eu.kalafatic.evolution.supervisor` (`-pl !eu.kalafatic.evolution.supervisor`) or setting `-Dsurefire.failIfNoSpecifiedTests=false`.
* **Recommendation**: Add a placeholder test suite runner in `eu.kalafatic.evolution.supervisor` or configure `failIfNoSpecifiedTests=false` in `eu.kalafatic.evolution.supervisor/pom.xml`.

---

### 2. Embedded NanoHTTPD Thread Model & Heavy Synchronous Handler Execution
* **Severity**: **MEDIUM**
* **Subsystem**: Server Subsystem (`eu.kalafatic.evolution.servers.EvolutionServer`)
* **Evidence**: REST HTTP handlers (e.g. `/forge/dataset/prepare`, `/develop/task`) trigger synchronous dataset acquisition or cognitive loop executions directly on NanoHTTPD request threads.
* **Impact**: Concurrent HTTP calls under heavy load can exhaust HTTP listener thread pools or block REST callers for up to 120s during LLM inference or dataset downloads.
* **Recommendation**: Offload long-running endpoint executions to asynchronous task queues returning a HTTP 202 Accepted status with a job status polling endpoint (`/jobs/:id`).

---

### 3. Memory Pressure during Parquet / Binary Dataset Ingestion
* **Severity**: **MEDIUM**
* **Subsystem**: Forge Data Pipeline (`eu.kalafatic.evolution.forge.data.impl.adapter.ParquetAdapter`)
* **Evidence**: Snappy decompressed block allocations in `ParquetAdapter` were capped at 2MB per block and string candidate extractions capped at 50,000 strings to prevent `OutOfMemoryError`.
* **Impact**: Uncapped streaming of large multi-gigabyte Parquet or JSONL files can cause high JVM heap memory pressure if default heap sizes are small (`-Xmx1g`).
* **Recommendation**: Implement chunked disk-backed buffer swapping for dataset streams exceeding 500 MB.

---

### 4. Direct OS Execution Dependencies for Windows Firewall Preparation
* **Severity**: **LOW**
* **Subsystem**: Self-Dev Network Access (`eu.kalafatic.evolution.controller.orchestration.selfdev.net.WindowsNetworkAccessProvider`)
* **Evidence**: `WindowsNetworkAccessProvider` executes PowerShell commands (`Get-NetFirewallRule`, `New-NetFirewallRule`) during Self-Dev startup.
* **Impact**: On non-administrator accounts or restricted Windows corporate environments, PowerShell execution policies may throw permission errors or log warnings.
* **Recommendation**: Gracefully fallback to local loopback socket binding (`127.0.0.1`) when firewall modification permissions are unavailable.
