# AI SUMMARY: EVO Platform Architecture

## What is this subsystem?
The high-level structural blueprint of the EVO platform. It organizes the system into functional domains: Orchestration, Evolution (Darwin), Mediation, and Forge.

## Why does it exist?
To provide a modular framework for autonomous software evolution. It ensures that reasoning (AI) is decoupled from physical execution (Git/Maven) and that all operations are session-isolated.

## Interaction Map
- **Orchestration** calls **Darwin** to generate solutions.
- **Darwin** uses **Mediation** to curate context for external LLMs.
- **Darwin** uses **Git/Maven tools** to verify proposed changes.
- All subsystems publish milestones to the **Runtime Event Bus**.

## Reading Order for Deeper Understanding
1. [DARWIN_SUMMARY.md](DARWIN_SUMMARY.md): The cognitive kernel.
2. [DISCOVERY_SUMMARY.md](../modules/DISCOVERY_SUMMARY.md): How the system understands code.
3. [WORKFLOW_INDEX.md](../WORKFLOW_INDEX.md): How these parts move together.
