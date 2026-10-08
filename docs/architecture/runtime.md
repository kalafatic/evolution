# Runtime Topology & OSGi Container Architecture

## OSGi Equinox Runtime Container

EVO executes inside an OSGi Equinox framework container provided by the Eclipse Rich Client Platform (RCP). Bundles are managed dynamically through standard OSGi lifecycles (`INSTALLED`, `RESOLVED`, `STARTING`, `ACTIVE`, `STOPPING`, `UNINSTALLED`).

```
+---------------------------------------------------------------------------------+
|                          OSGi Equinox Framework Container                       |
|                                                                                 |
|   +-----------------------+  +-----------------------+  +-------------------+   |
|   |  eu.kalafatic.view   |  | eu.kalafatic.servers  |  | eu.kalafatic.ctrl |   |
|   |  State: ACTIVE        |  | State: ACTIVE         |  | State: ACTIVE     |   |
|   +-----------------------+  +-----------------------+  +-------------------+   |
|                                                                                 |
|   +-------------------------------------------------------------------------+   |
|   | NanoHTTPD Embedded Servers:                                             |   |
|   |   - EvolutionServer (Port 48081 / 48091)                                |   |
|   |   - SupervisorServer (Port 8089 / 8099)                                 |   |
|   |   - SupervisorControlServer (Port 28080 / 28090)                        |   |
|   |   - MCP Server (Port 38080)                                             |   |
|   +-------------------------------------------------------------------------+   |
+---------------------------------------------------------------------------------+
```

---

## Process Models & Multi-Process Self-Dev Isolation

EVO operates across two primary runtime process topologies:

### 1. Single RCP Workbench Mode
When launched via `evo.exe` or `evo` binary, the OSGi framework initializes the Workbench UI (`ApplicationWorkbenchWindowAdvisor`), registers HTTP server services, and loads the active EMF project.

### 2. Multi-Process Self-Dev DEBUG Isolation Mode
During Self-Development loops, process collision on network ports and workspace directories is prevented by applying an explicit **DEBUG Port Isolation** mechanism:
* **Port Offset**: When `context.isDebugMode() == true`, a port offset of `+10` is added to all configured service ports:
  * Supervisor Service: Configured `8089` -> Effective `8099`
  * Control Service: Configured `28080` -> Effective `28090`
  * Evolution HTTP Server: Configured `48081` -> Effective `48091`
* **Process Ownership & PID Tracking**: `SelfDevContext` tracks child process metadata (`supervisorPid`, `supervisorProcess`, `uiRcpSupervisorProcess`) with explicit `ProcessOwnership` classification (`CURRENT_RUN`, `OTHER_RUN`, `PARENT_RCP`, `UNKNOWN`).
* **Dual Execution Mode**: `SupervisorRuntime` launches BOTH Process 1 (Headless Java Supervisor) AND Process 2 (UI RCP Supervisor with `--ui`) on distinct offset ports, recording both PIDs and ensuring clean termination upon task completion without terminating the parent workbench.

---

## OSGi Diagnostics & Dynamic State Endpoint

`EvolutionServer` provides a GET `/server/osgi` HTTP REST endpoint that queries the OSGi `BundleContext` directly at runtime. It returns JSON detailing:
* Total installed bundles
* Bundle symbolic names, versions, and states (`ACTIVE`, `RESOLVED`, `INSTALLED`)
* Unresolved bundle header diagnostics (`Require-Bundle`, `Import-Package`, `Fragment-Host`)

`EvoRcpVerifier` uses this endpoint during Self-Dev startup verification to guarantee no critical EVO bundle remains stuck in state `INSTALLED`.
