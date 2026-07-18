# Deep Analysis of Genome Module (eu.kalafatic.evolution.selfdev.genome)

## 1. Source Architecture
The `eu.kalafatic.evolution.selfdev.genome` module is the core domain component of Gemini's self-development/kernel evolution loop. It provides the programmatic abstractions and state models for representing, updating, and storing the "DNA" (genome) of the system itself.

### Key Packages and Source Roles:
1. **`eu.kalafatic.evolution.selfdev.genome.core`**:
   - Represents fundamental artifact classes: `GenomeArtifact`, `MediatedPackageArtifact`, `MetricArtifact`, and `ProjectSnapshot`.
   - Defines `Mode` and `ArtifactType` enums that control validation.
2. **`eu.kalafatic.evolution.selfdev.genome.hub`**:
   - Offers `SelfDevGenomeHub` as the primary singleton coordinator.
   - Orchestrates uploads of discovery packages and mediated packages, and generates upgrade plans using the upgrade engine.
3. **`eu.kalafatic.evolution.selfdev.genome.selfupgrade`**:
   - Implements `SecondhandUpgradeEngine` which takes an `UpgradeContext` and compiles structured, deterministic `UpgradePlan` structures comprising individual `ArchitecturalChange` and `FileChange` actions.
4. **`eu.kalafatic.evolution.selfdev.genome.milestone`**:
   - Contains `MilestoneGenerator` which programmatically scans the target workspace/project root, packages configuration snapshot data, and generates the current genome milestone description (`genome/current/genome.json`).
5. **`eu.kalafatic.evolution.selfdev.genome.repository`**:
   - Handles localized persistence (`LocalGenomeRepository`) to read and write genome configuration schemas.

---

## 2. Build Lifecycle (Tycho & OSGi integration)
* **Packaging Type**: `<packaging>eclipse-plugin</packaging>`
* **Compiler Compliance**: Java SE 21+ (`maven-compiler-plugin` configuration targeting JVM 21).

### Build Resolution Details:
* **Eclipse/OSGi Target Platform**: The module relies on the active p2 repositories (defined in the parent `pom.xml`) to resolve OSGi bundles and target platforms.
* **Manifest Headers**:
  - `Bundle-SymbolicName: eu.kalafatic.evolution.selfdev.genome`
  - `Export-Package`: Explicitly exports all its API packages (`core`, `hub`, `milestone`, `repository`, etc.) so that other platform OSGi plug-ins (such as `eu.kalafatic.evolution.controller` and the supervisor) can resolve and load them at runtime.
* **Third-Party Dependencies**: Declares direct Maven dependencies on `org.json:json`, `jackson-databind`, and `nanohttpd`.

---

## 3. On-The-Fly Deployment and Runtime Compilation
When running the pre-flight checks, `SelfDevBootstrapController.checkGenome()` dynamically recompiles and builds the genome module prior to executing its methods. This ensures any newly generated code/model changes in the workspace are immediately deployed:
1. **Multi-tiered Scanner**: Resolves the exact location of the `eu.kalafatic.evolution.selfdev.genome` module by checking parent directories, siblings of the project root, and the active workspace codebase paths.
2. **On-the-fly Compiler Invocation**: Spawns a background process running:
   `mvn clean compile -pl eu.kalafatic.evolution.selfdev.genome -am -DskipTests`
   which compiles the classes and registers them into the reactor classloader path.
3. **Reintegration**: Invokes `SelfDevGenomeHub.getInstance().updateGenome(...)` via reflection or direct OSGi service binding, creating/updating the critical `/genome/current/genome.json` file in the project workspace.
4. **Shaded Packaging (Supervisor)**: Since `eu.kalafatic.evolution.supervisor` uses a standard Maven shading lifecycle, declaring a dependency on `selfdev.genome` guarantees that when the shaded/standalone supervisor JAR is packaged, the compiled genome classes are compiled and shaded into the supervisor jar, making all genome core types available to the supervisor in headless mode.
