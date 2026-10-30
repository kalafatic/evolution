# Self-Development Build, Export & External Runtime Management

**Date:** October 30, 2026
**Document ID:** `genome-docs/30102026/self-development-build-runtime.md`

---

## 1. Maven/Tycho Build Execution

- **Toolchain**: Java 21 (Temurin 21.0.9.10), Maven wrapper 3.9.12 (`./mvnw`).
- **Product Export Command**: `./mvnw clean package -P export-product -DskipTests`.
- **Target Module**: `eu.kalafatic.evolution.repository`.

---

## 2. Product Export & Runtime Validation

Before process launch, `EvoRcpDeployer` and `EvoRcpRuntime` validate required product structure:
1. Native executable: `evo` (Linux/Mac) or `evo.exe` (Windows).
2. JVM configuration: `evo.ini`.
3. Non-empty `plugins/` bundle directory.
4. Equinox configuration: `configuration/config.ini`.

---

## 3. Runtime Health Verification

`EvoRcpVerifier` performs multi-milestone health checks:
- **Milestone 1**: Process creation & launch verification.
- **Milestone 2**: File marker creation (`evo_ready.txt`).
- **Milestone 3**: HTTP GET `/server/status` returning HTTP 200 `OK`.
- **Milestone 4**: HTTP GET `/server/osgi` confirming OSGi bundle resolution.
