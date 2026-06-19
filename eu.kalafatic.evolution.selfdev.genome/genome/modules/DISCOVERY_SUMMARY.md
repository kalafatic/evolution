# AI SUMMARY: Discovery & Mediation

## What is this subsystem?
The eyes and ears of EVO. It scans the target project and curates context for LLMs.

## Why does it exist?
To build a high-fidelity semantic model of the project and ensure that external LLMs receive only the highest-signal information (avoiding token waste and noise).

## Key Mechanisms
- **Recursive Reconstruction**: Discovery deepens as it identifies knowledge gaps.
- **Context Curation**: Selecting the 4-16 most "authoritative" files.
- **Prompt Synthesis**: Generating architecturally informed instructions.

## Interaction Map
- **Discovery** feeds the **TargetRealityModel**.
- **Mediation** produces the **Export Package (ZIP)**.
