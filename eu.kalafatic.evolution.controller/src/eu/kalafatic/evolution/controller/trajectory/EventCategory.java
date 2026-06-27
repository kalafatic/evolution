package eu.kalafatic.evolution.controller.trajectory;

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
