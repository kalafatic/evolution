# EVO / EVOLUTION
# UNIVERSAL FINAL PROJECT ANALYSIS MILESTONE
## Complete Repository Intelligence, Architectural Reconstruction, Genome Documentation and Milestone Baseline

**Document type:** Universal analytical milestone
**Project:** EVO / Evolution
**Primary objective:** Produce a comprehensive, evidence-based technical knowledge base of the existing repository.
**Execution mode:** Analysis and documentation only.
**Implementation changes:** Prohibited.
**Intended consumers:** EVO Genome, Milestone Generator, functionality inventory, future Jules tasks, AI-assisted development processes, and other authorized project analysis tools.

---

# 1. MISSION

Perform a complete, systematic, evidence-based analysis of the existing EVO / Evolution project.

The objective is to reconstruct and document the actual state of the project as it exists in the repository at the time of analysis.

This is not a request to design a new architecture, rewrite the application, refactor existing classes, implement missing functionality, or produce a speculative future-state design.

The existing codebase is the primary source of truth.

Your task is to understand the project as an interconnected software system, identify what has actually been implemented, determine how its components interact, establish which capabilities are operational or incomplete, and produce persistent documentation that future development and analytical processes can consume without repeatedly rediscovering the entire codebase.

The result must be a reliable technical baseline for the continued evolution of EVO.

The documentation must be useful to both human developers and automated AI agents.

It must explain not only WHAT exists, but also:

- WHY individual components exist, when this can be established from evidence.
- HOW components work internally.
- HOW components interact with other components.
- WHERE important responsibilities and decision-making authorities reside.
- HOW data and control flow through the application.
- WHICH dependencies, contracts, and architectural assumptions connect the system.
- WHICH functionality is implemented, partially implemented, missing, broken, unverified, or merely planned.
- WHICH architectural constraints future development must preserve.
- WHICH improvements are justified by demonstrated evidence.
- HOW future Genome and Milestone processes should use the resulting knowledge.

Do not optimize for the number of documents, the number of analyzed files, or the apparent sophistication of the report.

Optimize for technical correctness, completeness, traceability, usefulness, and long-term maintainability.

---

# 2. NON-NEGOTIABLE PRESERVATION RULES

## 2.1 Analyze the existing evolutionary state

Treat the existing EVO codebase as the current evolutionary state of the system.

The purpose of the analysis is to understand and document that state, not to replace it with a new implementation.

Preserve the conceptual distinction between:

1. Existing, verified functionality.
2. Existing but incompletely verified functionality.
3. Existing defects or architectural weaknesses.
4. Missing functionality.
5. Planned or proposed functionality.
6. Future improvement opportunities.

Never merge these categories into a single description of what the system supposedly does.

## 2.2 No production-code modifications

This milestone is strictly analytical.

Do not:

- Modify Java source files.
- Modify existing application configuration.
- Modify EMF models or generated EMF code.
- Modify build configurations.
- Modify Tycho, Maven, OSGi, Eclipse RCP, or product definitions.
- Modify tests.
- Modify dependency versions.
- Introduce new libraries or dependencies.
- Create new production classes.
- Refactor existing implementations.
- Change public APIs or runtime behavior.
- Fix discovered defects.
- Generate replacement implementations.
- Execute automated formatting or code-generation operations that modify existing files.
- Perform Git operations that alter repository history or working-tree state.

The only permitted repository changes are the creation of the analytical documentation artifacts explicitly required by this milestone.

Do not overwrite or delete existing documentation.

If a documentation change would require modifying an existing authoritative document, create a new, appropriately linked analytical document instead.

Do not commit, push, merge, reset, clean, or otherwise alter Git history or tracked project content.

If the environment cannot guarantee a safe, read-only inspection of the production codebase, stop before performing any potentially destructive operation.

## 2.3 Preserve the existing architecture

The project uses Java 21 and Eclipse RCP / OSGi, with technologies including EMF and SWT Browser.

These are known project characteristics, not permission to assume that every module uses them identically.

Verify the actual implementation.

Do not recommend replacing the existing technology stack simply because another architecture, framework, language, or library might be more fashionable.

Do not introduce Spring, alternative language implementations, parallel service layers, duplicate discovery mechanisms, or competing architectural abstractions as speculative recommendations.

When an architectural concern exists, first identify the smallest existing component, contract, or responsibility relevant to that concern.

## 2.4 Evidence before conclusions

Every important architectural conclusion must be supported by repository evidence.

Prefer evidence in this order:

1. Actual implementation.
2. Direct callers and callees.
3. Interfaces, contracts, and concrete implementations.
4. Configuration and dependency declarations.
5. Tests and executable verification.
6. Logs, build results, and runtime artifacts, when available and safe to inspect.
7. Existing technical documentation.
8. Comments, naming conventions, and inferred intent.

Existing documentation must not automatically override contradictory implementation evidence.

When sources disagree, document the disagreement.

Never invent implementation details, successful execution results, dependencies, execution paths, or completed functionality.

If something cannot be verified, state that explicitly.

---

# 3. INITIAL REPOSITORY DISCOVERY

Before producing the analytical documentation, establish the structure and current state of the repository.

Inspect the existing project rather than assuming that its current organization matches an earlier understanding.

Determine:

- Repository root and current branch.
- Existing working-tree changes.
- Top-level modules and directories.
- Maven and Tycho reactor structure.
- Eclipse plug-ins and feature projects.
- Product definitions.
- OSGi bundle boundaries.
- Java source and resource locations.
- EMF models, generated models, and model resources.
- Tests and test infrastructure.
- Configuration files.
- Documentation directories.
- Genome-related modules and documentation.
- Milestone-related modules and documentation.
- Existing functionality inventory and metadata.
- Scripts and build tooling.
- Existing runtime and diagnostic artifacts, where available.
- Existing conventions for documentation names, dates, versions, and cross-references.

Inspect existing Git changes without modifying or reverting them.

Distinguish pre-existing changes from the documentation artifacts created by this milestone.

Do not indiscriminately enumerate or ingest:

- Compiled binaries.
- Generated output directories.
- Dependency caches.
- Large archives.
- Runtime dumps.
- Temporary build output.
- Credentials or secrets.
- Unrelated user data.

Inspect generated or runtime artifacts only when they provide relevant evidence that cannot be obtained more directly.

Do not assume that a directory name proves the functionality it contains is operational.

Do not assume that a module is obsolete merely because it appears old or lacks recent changes.

## 3.1 Establish the analysis timestamp

Record the actual analysis start time and completion time.

Use a consistent timestamp format, preferably:

YYYY-MM-DDTHH:mm:ss with the applicable timezone.

For date-based documentation directories, preserve the repository's existing naming convention where possible.

For this milestone, the intended date-based directory is:

`genome-docs/29102026/`

The prompt itself must have a uniquely identifiable timestamped filename:

`genome-docs/29102026/FINAL_EVOLUTION_ANALYSIS_PROMPT_20261029_2044.md`

If this path already exists, do not overwrite it. Select a unique timestamp or collision-safe suffix and document the actual filename.

The timestamp in the filename must reflect the prompt artifact's actual creation time or the explicit timestamp of this supplied prompt, rather than being silently changed to an unrelated date.

Record the Git commit identifier when available.

If the repository is not in a clean state, explicitly document that fact.

Do not claim that the analysis represents a clean release or production baseline unless that has been established.

---

# 4. ANALYZE THE EXISTING GENOME AND MILESTONE MECHANISMS

Before defining the final documentation structure, investigate the existing Genome and Milestone implementations.

Determine how they currently work, which data they consume, where they store their results, and which contracts they already establish.

Identify:

- Genome-related modules, classes, interfaces, and responsibilities.
- Milestone-related modules, classes, interfaces, and responsibilities.
- How analysis tasks are created and executed.
- How repository information is discovered.
- How project functionality is represented.
- How metadata and functionality inventories are produced.
- How milestones are generated, persisted, updated, and consumed.
- How task status and execution results are represented.
- Which existing models or files act as sources of truth.
- Whether Genome and Milestone share information directly or indirectly.
- Whether there are duplicate inventories, inconsistent metadata, or conflicting ownership.
- Which existing mechanisms can consume additional documentation without code changes.
- Which limitations prevent automatic consumption of the proposed documentation.

Do not create a second Genome, a parallel Milestone engine, a competing metadata model, or another functionality discovery subsystem.

The new documentation must complement the existing mechanisms.

If an integration is not currently supported, document the limitation and describe the relevant existing interface or extension point. Do not implement an integration in this milestone.

---

# 5. COMPLETE PROJECT INVENTORY

Build a structured inventory of the repository.

The inventory must cover all relevant first-party project modules and components.

For each module or substantial component, document:

- Exact module or component name.
- Repository-relative location.
- Module type.
- Primary responsibility.
- Important classes and interfaces.
- Main entry points.
- Important dependencies.
- Main callers and consumers.
- Main outputs and side effects.
- Relevant configuration.
- Relevant tests.
- Architectural ownership.
- Known limitations.
- Current verification status.
- Relevant documentation references.

Use concise summaries for straightforward modules and deeper analysis for central or architecturally significant modules.

Do not give every file the same analytical depth.

Prioritize files that define:

- Application lifecycle.
- Session management.
- Event infrastructure.
- Public interfaces.
- Core execution paths.
- Process orchestration.
- State transitions.
- Persistence.
- Resource discovery.
- Configuration.
- Inter-module communication.
- Security boundaries.
- AI inference.
- Build and self-development processes.
- Genome and Milestone behavior.

For generated code, identify the generation mechanism and distinguish generated implementation from handwritten logic.

For third-party dependencies, document their role and boundaries without attempting to analyze their complete source code.

Produce a navigable inventory rather than a raw dump of filenames.

---

# 6. ARCHITECTURAL RECONSTRUCTION

Reconstruct the actual architecture from the implementation.

Document the system's major architectural layers, components, boundaries, and interactions.

Do not force the project into an assumed architectural pattern.

If the system does not fit a clean layered architecture, document its real structure and the relevant exceptions.

Analyze at least the following areas when present in the repository:
- Application structure and lifecycle
- Core architecture and responsibilities
- Dependencies and communication
- Architectural diagrams (Mermaid format)
- Execution modes and platforms
