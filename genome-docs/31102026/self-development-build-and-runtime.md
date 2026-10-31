# Self-Development Build, Export & Runtime Supervision

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/self-development-build-and-runtime.md`

---

## 1. Maven/Tycho Product Build & Export

- **Toolchain**: Java 21, Maven wrapper 3.9.12 (`./mvnw`).
- **Product Export Command**: `./mvnw clean package -P export-product -DskipTests`.
- **Product Layout Validation**: `EvoRcpDeployer` verifies native launcher (`evo`/`evo.exe`), `evo.ini`, `configuration/config.ini`, and non-empty `plugins/` directory.

---

## 2. Runtime Readiness vs Process Startup

Process launch alone does not represent readiness. Readiness is verified through:
1. Marker file generation: `evo_ready.txt`.
2. REST endpoint verification: HTTP GET `/server/status` returning HTTP 200 `OK`.
3. OSGi bundle state check: HTTP GET `/server/osgi`.
4. Subsequent project initialization via POST `/server/project/create` and inference submission via POST `/task`.
