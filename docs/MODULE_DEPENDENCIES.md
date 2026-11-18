# EVO — Module Dependencies

Reactor order is derived by Maven/Tycho from the OSGi `Require-Bundle` / `Import-Package` graph. Key dependency facts:

## Core chain (must build in order)
```
eu.kalafatic.utils
  → eu.kalafatic.evolution.model (EMF)
      → eu.kalafatic.evolution.model.edit
          → eu.kalafatic.evolution.model.editor
eu.kalafatic.evolution.forge.*  (math → model → tokenizer → data → runtime → trainer → agent.api → observability → controller → agent)
eu.kalafatic.evolution.selfdev.genome
eu.kalafatic.evolution.servers        (embeds + EXPORTS jackson, nanohttpd, sqlite, jbcrypt, org.json)
eu.kalafatic.evolution.creatic
eu.kalafatic.evolution.enr
eu.kalafatic.evolution.controller     → model, utils, genome (reexport), servers (reexport), forge.*
eu.kalafatic.evolution.view           → controller (reexport), model, model.edit, selfdev.genome, forge.*
eu.kalafatic.evolution.supervisor     (plain JAR; jackson + nanohttpd + selfdev.genome pom deps)
eu.kalafatic.evolution.feature        → all application bundles
eu.kalafatic.evolution.repository     → feature (materializes products)
```

## External platform dependencies
- Eclipse 2025-12 SimRel (org.eclipse.* platform bundles).
- **GEF Classic** p2 repo (`https://download.eclipse.org/tools/gef/classic/release/latest`): `org.eclipse.zest.core`, `org.eclipse.zest.layouts`, `org.eclipse.draw2d`, `org.eclipse.gef` — used by `view`, `utils`, `module`, `forge.controller`. SimRel no longer carries these bundles.
- Orbit (org.json bundle).
- Jackson 2.17/2.20 embedded in `servers`/`forge.controller`/`forge.model`/`forge.agent` via `Bundle-ClassPath` lib jars; `servers` exports `com.fasterxml.jackson.*` for `controller`/`media`.

## Notable cross-bundle re-exports
- `controller` re-exports `model`, `utils`, `selfdev.genome`, `servers` → downstream bundles get EMF/jackson packages transitively.
- `view` re-exports `controller`.

## Test modules
- `eu.kalafatic.evolution.controller.tests` (largest, requires controller + test infra)
- `eu.kalafatic.evolution.model.tests`, `quality.tests`, `selfdev.genome.tests`, `tests`
- `eu.kalafatic.evolution.supervisor` (JUnit tests inside the JAR module)

## Non-reactor
- `eu.kalafatic.evolution.gpu` — standalone Maven JAR (jackson-databind 2.17.0, JUnit 4), Java 21 + `--enable-preview`.
