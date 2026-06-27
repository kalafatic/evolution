package eu.kalafatic.evolution.controller.trajectory;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

public enum EventCategory {
    KERNEL,     // System Lifecycle
    FLOW,       // Orchestration Control
    AGENT,      // Reasoning Layer
    EXECUTION,  // Tools & Side Effects
    UI,         // Presentation Layer
    SUPERVISOR, // Governance Layer
    WORKSPACE,  // Semantic Reasoning Environment
    FITNESS,    // Stability & Performance signals
    SYSTEM,     // Low-level runtime events
    COGNITIVE   // LLM reasoning & trajectory state
}
