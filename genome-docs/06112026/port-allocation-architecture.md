# EVO Platform Authoritative Port Allocation Architecture

## 1. Executive Summary

This document specifies the authoritative, deterministic port allocation convention and management architecture for the entire EVO platform.

EVO has **absolute priority** over TRYST and other applications. Network service assignments in EVO are deterministic: default assignments must never silently shift, and port conflicts must be detected prior to service startup and diagnosed with actionable OS process metadata (PID, process name, command line).

---

## 2. Port Allocation Standard

The platform follows a standardized **+20000 base offset** from original port ranges with a consistent increment of **100**.

### 2.1 Authoritative Service-to-Port Mapping Table

| Service Identifier | Service Description | Original Base Port | Authoritative EVO Port | Protocol | Binding Address | Auto-Start |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **`HTTP_PRIMARY`** | EVO Primary HTTP Server & REST API | `8080` | **`28080`** | HTTP/REST | `127.0.0.1` | Yes |
| **`HTTP_SECONDARY`** | EVO Secondary HTTP / Portal / AI Chat | `8180` | **`28180`** | HTTP/REST | `127.0.0.1` | Yes |
| **`MCP`** | Model Context Protocol (MCP) & Demo Docs Server | `8280` | **`28280`** | JSON-RPC / HTTP | `127.0.0.1` | Yes |
| **`DEVELOP`** | EVO Autonomous Coding Agent Endpoint | `8380` | **`28380`** | HTTP/REST | `127.0.0.1` | Yes |
| **`SUPERVISOR_API`** | Self-Development Supervisor REST API | `8480` | **`28480`** | HTTP/REST | `127.0.0.1` | Yes |
| **`SUPERVISOR_CONTROL`**| Self-Development Supervisor Control Plane | `8580` | **`28580`** | HTTP/Web UI | `127.0.0.1` | Yes |
| **`AUXILIARY`** | Reserved Auxiliary Service Endpoint | `8680` | **`28680`** | HTTP | `127.0.0.1` | No |

---

## 3. Migration of Legacy Port Assignments

Existing installations utilized fragmented ports across 38080, 48080, 58080, and 8089. These are deterministically migrated on startup by `PortAllocationManager` and `PortPersistence`:

1. **`48080` / `8080` (Primary HTTP Server)**:
   - Migrated to **`28080`**.
   - Affects `EvolutionServer`, `ServerManager`, `scripts/start-headless.sh`, and web templates (`chat.html`, `forge.html`, `develop.html`, `mutation.html`).
2. **`58080` / `48081` / `8180` (Secondary Server & Portal)**:
   - Migrated to **`28180`**.
   - Affects `ServerSettings.portalPort`, `ServerSettings.portalUrl`, and secondary background server threads.
3. **`38080` / `48082` / `8280` (MCP Server & Documentation Server)**:
   - Migrated to **`28280`**.
   - Affects `McpDemoServerManager`, `McpConfig`, `DemoConfiguration`, `Orchestrator.mcpServerUrl`, and `McpSettingsPage`.
4. **`8089` (Supervisor REST API)**:
   - Migrated to **`28480`**.
   - Affects `SupervisorMain`, `EVOSupervisorControlServer`, `SupervisorUiWindow`, `SupervisorRuntime`, and `SupervisorClient`.
5. **`28080` (Legacy Supervisor Control Plane)**:
   - Realigned to **`28580`** to eliminate collision with the primary HTTP server on port 28080.
   - Affects `EVOSupervisorControlServer` and `SupervisorGroup`.

---

## 4. Centralized Architectural Components

Located in package `eu.kalafatic.evolution.controller.resource`:

```
           +---------------------------------------------------+
           |                   ResourceManager                 |
           +-------------------------+-------------------------+
                                     |
               +---------------------+---------------------+
               |                                           |
               v                                           v
   +------------------------+                 +------------------------+
   |      PortRegistry      |                 |    PortPersistence     |
   | (Authoritative Mapping)|                 | (EMF + Atomic Backup)  |
   +-----------+------------+                 +-----------+------------+
               |                                           |
               v                                           v
   +------------------------+                 +------------------------+
   | PortAllocationManager  | <-------------> |  PortLifecycleManager  |
   | (Integrity & Migration)|                 |   (8-Stage Lifecycle)  |
   +------------------------+                 +-----------+------------+
                                                           |
                                                           v
                                              +------------------------+
                                              | PortAvailabilityChecker|
                                              | (OS Process Diagnostics|
                                              +------------------------+
```

1. **`PortRegistry`**:
   - Central authoritative enum defining service identity, default ports, protocols, and standard aliases.
2. **`PortConfiguration`**:
   - Immutable data model holding service ID, assigned port, default port, protocol, binding host, and configuration version.
3. **`PortAvailabilityChecker`**:
   - Cross-platform socket verification.
   - On conflict, inspects the OS using Windows `netstat -ano`, Unix `lsof -t`, `fuser`, `ss`, and Java 21 `ProcessHandle` to identify PID, executable path, and arguments.
4. **`PortAllocationManager`**:
   - Enforces port range boundaries (`1024-65535`).
   - Prevents intra-EVO port collisions.
   - Detects legacy configurations and executes non-destructive migrations.
5. **`PortPersistence`**:
   - Binds directly to the authoritative EMF model (`ServerSettings`, `SupervisorSettings`, `Orchestrator`).
   - Maintains an atomic local disk backup (`~/.evo/ports.properties`) to ensure configuration survives export, workspace switches, and clean upgrades.
6. **`PortLifecycleManager`**:
   - Coordinates the standard 8-stage startup and shutdown sequence across all platform listeners.

---

## 5. Startup & Shutdown Lifecycle

All EVO network services follow an authoritative 8-step lifecycle:

```
[1. Load Config] ---> [2. Validate Integrity] ---> [3. Resolve Ports]
                                                            |
                                                            v
[6. Start Listeners] <-- [5. Init Dependents] <-- [4. Check Availability]
       |
       v
[7. Register Endpoints] ---> [8. Publish Readiness (evo_ready.txt)]
```

1. **Load Authoritative Configuration**: Load port assignments from EMF models and local configuration storage.
2. **Validate Configuration Integrity**: Check port bounds (1024-65535) and verify zero duplicate assignments inside EVO.
3. **Resolve Service Assignments**: Map each active service to its assigned port and target binding address.
4. **Check Port Availability & Conflict Inspection**: Probe socket availability. If occupied, log structured diagnostics detailing conflicting PID and process name. Fails fast without shifting ports.
5. **Initialize Dependent Services**: Instantiate controllers and server models.
6. **Start Listeners**: Bind server sockets via NanoHTTPD or native sockets.
7. **Register Actual Bound Endpoints**: Track bound listeners in `PortLifecycleManager` registry.
8. **Publish Service Readiness**: Write `self-dev-run/evo_ready.txt` timestamp marker and publish OSGi service availability.

On shutdown, `shutdownAll()` stops all managed listeners and unbinds sockets, guaranteeing zero port leaks.

---

## 6. Collision Policy & Troubleshooting

### 6.1 Collision Policy Invariants
- **Deterministic**: Default ports must never silently shift to an ephemeral or shifted port.
- **Non-Destructive**: EVO will never terminate an external process automatically.
- **Diagnostic-First**: Immediate detailed logging of PID, command line, and remediation instructions.

### 6.2 Troubleshooting Diagnostics

When a port is occupied, EVO emits a diagnostic report:

```
PORT CONFLICT DETECTED on port 28080 for service [EVO Primary HTTP / REST Server].
Occupied by PID: 12345 (myapp.exe) Command: myapp.exe --listen 28080
Remedy: Stop or reconfigure the conflicting process, or free port 28080 before launching EVO.
EVO ports are deterministic and will not silently shift.
```

To resolve:
1. Identify the process via the reported PID.
2. Free the port on Linux/macOS:
   ```bash
   lsof -i :28080
   kill <PID>
   ```
3. Free the port on Windows:
   ```cmd
   netstat -ano | findstr :28080
   taskkill /PID <PID> /F
   ```
4. Restart EVO.
