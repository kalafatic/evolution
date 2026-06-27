package eu.kalafatic.evolution.controller.orchestration.behavior;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.HashSet;
import java.util.Set;

public class BehaviorProfile {
    private final Set<BehaviorTrait> traits = new HashSet<>();

    public void addTrait(BehaviorTrait trait) {
        traits.add(trait);
    }

    public boolean hasTrait(BehaviorTrait trait) {
        return traits.contains(trait);
    }

    public Set<BehaviorTrait> getTraits() {
        return traits;
    }
}
