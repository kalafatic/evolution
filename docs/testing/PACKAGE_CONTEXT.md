# PACKAGE CONTEXT

## Directory: docs/testing/

## Domain: general

## Components
* `DARWIN_MODE.md`: Competitive evolutionary solving. The `DarwinEngine` generates multiple competing "variants" (experimental Git branches) for each goal, evaluates them against build/test metrics, and merges only the fittest solution. 1. Open the **AiChatPage**. 2. Activate Darwin Mode:
* `SIMPLE_CHAT.md`: Direct interaction with the AI without triggering background orchestration or code changes. The `EvolutionOrchestrator` bypasses the planning loop and dispatches the request directly to the `GeneralAgent`. 1. Open the **AiChatPage**. 2. Enter a general query (e.g., `"How does the Evolution platform handle multiple modes?"`).
* `ASSISTED_CODING.md`: Guided multi-step tasks where the user maintains control over the plan and execution. The system decomposes the request into atomic `Task` objects (file, maven, git, shell) and pauses for user review. 1. Open the **AiChatPage**. 2. Enable iterative development:
* `HEADLESS_AND_BUILD.md`: To automate the full Tycho build process, use the provided build script: ```bash ./scripts/build.sh
* `PACKAGE_CONTEXT.md`: 
* `SELF_DEV_MODE.md`: Autonomous self-modification mode. The `SelfDevSupervisor` manages a multi-iteration loop (default 5 iterations) using the Darwinian engine. Unlike other modes, this mode allows the system to modify its own source code within predefined `allowedPaths`. 1. Open the **AiChatPage**. 2. Activate Self-Dev Mode:
