# Configuration Architecture & Resource Model

## Configuration Authority: EMF Model & `ResourceManager`

Persistent system configuration in EVO is governed by a single authoritative configuration layer: the Eclipse Modeling Framework (EMF) model defined in `eu.kalafatic.evolution.model/model/evolution.ecore` and managed at runtime via `eu.kalafatic.evolution.controller.resource.ResourceManager`.

Configuration is **never** duplicated in secondary properties files, JSON files, or loose configuration dumps.

```
                  +-----------------------------------+
                  |         EMF Resource File         |
                  |     (evo_config.xml / .evo)       |
                  +-----------------+-----------------+
                                    |
                                    v
                  +-----------------------------------+
                  |        ProjectModelManager        |
                  |     (Loads/Saves EMF Resource)    |
                  +-----------------+-----------------+
                                    |
                                    v
                  +-----------------------------------+
                  |          ResourceManager          |
                  |   (Single Configuration Authority)|
                  +-----------------+-----------------+
                                    |
            +-----------------------+-----------------------+
            |                                               |
            v                                               v
+-----------------------+                       +-----------------------+
|  Services & Endpoints |                       |  Filesystem Boundaries|
|  (IP, Port, URL, Mode)|                       | (Git Repo, Workspace) |
+-----------------------+                       +-----------------------+
```

---

## Configuration Hierarchy & Precedence Rules

When resolving configuration settings at runtime, EVO applies the following strict precedence hierarchy (highest to lowest):

1. **Self-Dev Run Context (`SelfDevContext`)**: Transient, per-run overrides (e.g. DEBUG port offsets, run-specific directory paths `<workspace>/self-dev/run_<id>/`).
2. **EMF Persistent Project Model (`EvolutionProject` / `ForgeSession`)**: Persistent settings stored in `evo_config.xml` (e.g. service URLs, model sizes, training epoch defaults, dataset selections).
3. **OS Environment Variables**: Platform configuration defaults (`${user.home}`, `${EVO_ROOT}`).
4. **Hardcoded Invariant Defaults**: Fallback defaults defined in `ResourceManager` (e.g. Supervisor port default `8089`, Control port default `28080`, Evolution HTTP port default `48081`).

---

## Absolute Filesystem Boundary Rules

All filesystem paths resolved through `ResourceManager` or `ResolvedSelfDevResources` MUST strictly conform to canonical root boundaries. No path scanning, heuristic searching, or arbitrary directory probing is permitted.

```java
// Path Validation Rule in ResourceManager
public static void validateCanonicalPath(Path path) {
    String normalized = path.toAbsolutePath().normalize().toString();
    boolean validGit = normalized.startsWith(getUserGitRoot());
    boolean validWorkspace = normalized.startsWith(getUserWorkspaceRoot());
    if (!validGit && !validWorkspace) {
        throw new IllegalArgumentException("Canonical path violation: " + normalized);
    }
}
```

### The Three Canonical Filesystem Roots:
1. **Source Root**: `${user.home}/git/` (e.g., `${user.home}/git/evolution`) — READ-ONLY canonical Git repositories.
2. **Persistent Workspace**: `${user.home}/workspace/` (e.g., `${user.home}/workspace/datasets`, `${user.home}/workspace/models`).
3. **Runtime Execution**: `${user.home}/workspace/runtime/` (e.g., `${user.home}/workspace/runtime/builds`, `${user.home}/workspace/runtime/instances`).
