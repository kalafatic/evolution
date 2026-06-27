package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.EvolutionPhaseMachine;

public class DefaultPhaseEngine implements PhaseEngine {
    private final EvolutionPhaseMachine machine = new EvolutionPhaseMachine();

    @Override
    public EvolutionPhase getInitialPhase() {
        return machine.getInitialPhase();
    }

    @Override
    public EvolutionPhase next(EvolutionPhase current) {
        return machine.next(current);
    }

    @Override
    public boolean isTerminal(EvolutionPhase phase) {
        return machine.isTerminal(phase);
    }

    @Override
    public String toLegacyString(EvolutionPhase phase) {
        return EvolutionPhaseMachine.toLegacyString(phase);
    }
}
