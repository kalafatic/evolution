# Guidance & Cognitive Assistance

## Overview
The Guidance system provides real-time, context-aware assistance to the user. It acts as the "Pre-frontal Cortex" of EVO, interpreting platform signals to suggest next steps, identify risks, and explain complex evolutionary outcomes.

## Key Components

### GuidanceEngine
Analyzes the stream of events from the `RuntimeEventBus` to match predefined patterns or trigger LLM-based guidance generation.
- **Location**: `eu.kalafatic.evolution.creatic` bundle.

### ContextAssistant
Provides suggestions during the initial task formulation phase. It helps users refine their goals to be more "evolvable."

### CognitiveStateEngine
Tracks the session's "Cognitive Direction" and "Confidence." It detects when the evolution is converging or "spinning its wheels."
- **Metrics**: Velocity, Acceleration, Trend Stability.

## Guidance Features
- **Spontaneous Tips**: Short, actionable advice based on the current page or active tool.
- **Cognitive Pressure Warnings**: Alerts when the design space is too ambiguous or implementation risk is high.
- **Convergence Summaries**: Explains why a particular variant was selected as the winner.

## Related
- [EVENT_BUS.md](../architecture/EVENT_BUS.md)
- [ARCHITECTURE.md](../architecture/ARCHITECTURE.md)
