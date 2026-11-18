# EVO – Agent Instructions

## 1. Project identity

EVO (Evolution Platform) is an existing, evolving Java codebase built on Java 21, Eclipse RCP, Equinox/OSGi, Tycho, EMF, SWT and SWT Browser.

EVO is an evolutionary AI platform. Its existing implementation is the accumulated architectural genome of the platform. Improve and evolve that implementation; do not replace it with a new architecture.

## 2. Core rules

- This is an existing, evolving Java codebase. Improve its existing architecture; do not rewrite or replace it.
- Preserve public behavior, APIs, session/event flows and compatibility unless the task explicitly requires a change.
- Prefer minimal, targeted fixes over broad refactoring.
- Before changing code, inspect the relevant implementation, callers, interfaces, extensions and tests.
- Reuse existing services and abstractions. Do not introduce parallel architectures, duplicate managers or competing sources of truth.
- Do not delete files or perform broad cleanup unless explicitly requested.
- Distinguish confirmed facts from assumptions. Never invent classes, methods, paths, configuration or test results.
- When asked to implement, implement the change; do not stop after analysis or provide a proposed fix as if it were completed.
- If blocked, identify the exact obstacle, try a different valid inspection or implementation route, and report what remains incomplete.

## 3. Technology constraints

- Java 21, Eclipse RCP, OSGi, Tycho, EMF, SWT and SWT Browser.
- No Spring, Node.js runtime, Python runtime or unrelated frameworks.
- Follow the existing bundle structure, `MANIFEST.MF`, `plugin.xml`, target platform and Tycho conventions.
- Keep configuration in the existing EMF-based configuration architecture.
- Use existing Eclipse/PDE APIs and platform services where appropriate rather than introducing parallel infrastructure.

## 4. Darwin architecture

- All model inference must go through the existing Darwin Engine path.
- Respect the existing orchestration chain: `OrchestratorServiceImpl` → `KernelFacade` → `KernelFactory` / `SessionContainer` / `IterationManager` → `ADarwinEngine`.
- `IterationManager` is the sole authority for iteration transitions.
- Do not create another session manager, iteration controller or parallel orchestration pipeline.
- `DarwinFlow` and mode-specific implementations must reuse the existing shared engine architecture.
- Extend existing modes and capabilities without breaking their established behavior.
- Before changing orchestration, trace submission, session creation, context construction, iteration transitions, tool execution, inference, result handling and event propagation.

## 5. Modes and DEVELOPER architecture

- Preserve the semantics and routing of existing modes, including `SIMPLE_CHAT`, `ASSISTED_CODING`, `DARWIN_MODE`, `SELF_DEV_MODE`, `INTENT_RECONSTRUCTION`, `MEDIATED`, `FORGE` and `DREAM`.
- Do not silently merge modes or route a mode to a different execution path as a shortcut.
- `DEVELOPER` is a Darwin Engine capability/mode, not a separate competing orchestration engine.
- Keep Developer mode distinct from the existing self-development and mediated workflows while reusing shared Darwin infrastructure.
- Preserve the existing target-selection and output/review conventions unless a task explicitly changes them.
- Use existing Architecture Scanner, Genome, Eclipse Java/PDE tooling and context/tool infrastructure where appropriate. Inspect what already exists before adding capabilities.
- Eclipse tooling integration should reuse native workspace, Java Model, PDE, search, call/type hierarchy, compiler and resource APIs where available.
- Do not create a second project model, workspace manager or source-analysis subsystem if existing Eclipse/EVO services already provide the capability.

## 6. Coding-agent integration

Darwin Engine remains the orchestrator. Coding agents are execution capabilities available to Darwin; they are not replacements for Darwin.

The intended architecture supports modular adapters for external coding agents, including OpenCode and Aider, alongside the experimental native EVO Coder.

- Implement or extend a common existing agent/adapter contract where one exists; do not introduce a competing abstraction.
- Keep agent-specific process startup, configuration, input/output, streaming, cancellation, progress, permissions and result collection behind the adapter boundary.
- Do not hardcode OpenCode, Aider or the native Coder into the Darwin orchestration core.
- Keep external agents replaceable and allow Darwin to select or invoke them through existing context, tools and orchestration.
- The native EVO Coder is experimental and separate from the external-agent adapters. Do not turn it into a third mandatory coding-agent architecture or a replacement for Darwin.
- Preserve the ability to compare and experiment with different agents without duplicating session or task lifecycle management.
- Inspect actual CLI/API behavior and installed versions before relying on undocumented command-line flags or output formats.

## 7. Configuration and resources

- Reuse the central `ResourceManager` facade and existing EMF configuration.
- Do not duplicate path discovery, resource lookup, port allocation or configuration persistence logic.
- Keep existing EVO server and port conventions intact unless explicitly instructed otherwise.
- Treat EMF models and their established persistence/loading mechanisms as authoritative.
- Resolve paths relative to the correct project, workspace or configured target; do not silently use the process working directory as a substitute.
- Preserve existing configuration compatibility and defaults unless the task explicitly requires a migration.

## 8. Self-development

- Preserve the existing self-development workflow and its lifecycle.
- Understand the existing repository copy, isolated run workspace, source/build/export/log/runtime directories, preflight, build, validation, deployment, supervisor and runtime handoff before changing them.
- Repair existing preflight tasks and lifecycle defects in place; do not replace the workflow with a parallel pipeline.
- Preserve diagnostics, logs, failure states and traceability.
- Do not report build, deployment or runtime success unless the corresponding operation actually completed and was verified.

## 9. Forge and local LLM

- Reuse the existing Forge, dataset, training, model acquisition, export and Ollama integration.
- Preserve existing profiles, limits, artifact conventions and model configuration unless explicitly requested.
- Keep acquisition, dataset preparation, training, checkpoint/resume, export and inference as distinct stages.
- Do not silently change model defaults, epochs, checkpoint frequency, context settings or training parameters.
- Verify actual model identifiers, runtime configuration and GPU/CPU placement rather than inferring them from a display label.
- Avoid introducing another model registry or inference path outside Darwin's established architecture.

## 10. Servers and lifecycle

- Preserve the existing EVO server services, startup/shutdown lifecycle, configuration and port allocation.
- Current intended port conventions are:
  - HTTP: `28080`
  - Portal/secondary: `28180`
  - MCP: `28280`
  - DEVELOP: `28380`
  - Supervisor REST: `28480`
  - Supervisor Control TCP: `28580`
  - Auxiliary: `28680`
- Treat these as project conventions, not permission to hardcode values where the existing configuration/allocation service should be used.
- Respect RCP startup and shutdown, OSGi service lifecycle and SWT lifecycle.
- Do not create duplicate server lifecycle controllers or silently fall back to unrelated default ports.
- Verify actual bind addresses, persisted configuration and startup logs when diagnosing server issues.

## 11. UI principles

- Preserve existing Eclipse RCP/SWT behavior and public user workflows.
- Prefer native SWT and the existing SWT Browser/HTML/CSS approach already used by the project.
- Keep UI professional, clear, maintainable and consistent with the existing application.
- Avoid unnecessary graph libraries and heavyweight UI frameworks.
- All SWT widget updates must run on the correct UI thread using the established project patterns.
- Do not remove existing pages, controls, context menus or functionality when improving the UI.

## 12. Implementation and verification

For implementation tasks:

1. Identify the actual EVO repository root and relevant bundles.
2. Inspect the relevant implementation, callers, interfaces, extensions, configuration and tests.
3. Trace the real execution path and establish the root cause.
4. Define the smallest complete change consistent with the architecture.
5. Implement the requested change in the existing code.
6. Run focused tests and the relevant Maven/Tycho build where feasible.
7. Check bundle manifests, OSGi service wiring, extension registrations, EMF compatibility and SWT lifecycle when affected.
8. Inspect the actual changed files and ensure no unrelated changes were introduced.
9. Report what was changed and what was actually verified.

For analysis-only tasks, do not modify source files.

Do not claim compilation, tests, runtime behavior or implementation success unless actually executed and verified. If verification cannot be run, state why and distinguish unverified work from completed work.

## 13. Git and destructive operations

- Never run `git`, `gh` or GitHub operations.
- Never commit, push, pull, fetch, clone, switch branches, stage files or modify repository history.
- Git is controlled exclusively by the user through Eclipse EGit.
- Do not run destructive shell commands or delete project files.
- Do not perform broad cleanup, mass formatting or unrelated changes.
- You may inspect source files and build/test output, but do not use Git commands even for read-only status or diff inspection.

## 14. Completion report

For implementation tasks, report:
- Confirmed root cause or requirement.
- Modified files and relevant classes/methods.
- Architectural reasoning and impact.
- Build and test commands actually executed.
- Actual build/test results.
- Runtime verification, if performed.
- Remaining issues and anything not verified.

For analysis tasks, report evidence, source locations, confirmed findings, hypotheses and recommended minimal changes.

## 15. Governing principle

EVO must evolve without losing its architectural genome. Understand the existing system, preserve its authoritative services and lifecycle, and make the smallest verified improvement that solves the requested problem.
