package eu.kalafatic.evolution.controller.orchestration.capability;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

public enum CapabilityStatus {
    INITIALIZED,
    STARTED,
    STOPPED,
    ERROR
}
