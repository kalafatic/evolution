package eu.kalafatic.evolution.controller.workflow;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

public interface RuntimeEventListener {
    void onEvent(RuntimeEvent event);
}
