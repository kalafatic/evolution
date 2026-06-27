package eu.kalafatic.evolution.controller.orchestration.selfdev;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

public enum DimensionState {
    DISCOVERED,
    ACTIVE,
    BRANCHED,
    SELECTED,
    LOCKED,
    REOPENED,
    COLLAPSED,
    MERGED,
    PRUNED
}
