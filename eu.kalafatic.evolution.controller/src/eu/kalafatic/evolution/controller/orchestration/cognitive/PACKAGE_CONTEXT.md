# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/cognitive/

## Domain: general

## Components
* `CapabilityDetector.java`: Interface for independent capability detectors.
* `CodeDetector.java`: Detector for coding-related capabilities using evidence accumulation.
* `ArchitectureDetector.java`: Detector for architecture-related capabilities using evidence accumulation.
* `EvolutionDetector.java`: Detector for evolution-related capabilities using evidence accumulation.
* `SelfDevDetector.java`: Detector for self-development capabilities using evidence accumulation.
* `Evidence.java`: Represents a single piece of evidence for a capability.
* `CapabilityAnalysis.java`: Represents the full cognitive analysis of an interaction, containing all candidates.
* `CapabilitySignal.java`: Represents a single cognitive signal detected from an interaction.
* `CognitiveAnalysisPipeline.java`: Multi-stage cognitive analysis pipeline based on evidence accumulation.
* `CognitiveStateEngine.java`: Orchestrates the cognitive state transitions and routing using the analysis pipeline.
* `CapabilityScoringEngine.java`: Manages capability scores and implements hysteresis to prevent oscillation.
* `SessionIntent.java`: High-level intent behind a user session (LEARNING, BUILDING, ANALYZING, TROUBLESHOOTING, EVOLVING).
* `CognitiveTrajectoryEngine.java`: Tracks the cognitive trajectory of the session.
* `CognitiveDirection.java`: Enumeration of cognitive directions.
* `CapabilityType.java`: Fundamental cognitive capabilities of the platform (CHAT, CODE, ARCHITECTURE, EVOLUTION, SELF_DEV).
* `SessionCognitiveState.java`: Stores the holistic cognitive state of a session.
