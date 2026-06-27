package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

/**
 * Supported platform modes.
 */
public enum PlatformType {
    SIMPLE_CHAT,
    ASSISTED_CODING,
    DARWIN_MODE,
    SELF_DEV_MODE,
    HYBRID_MANUAL_EXPORT
}
