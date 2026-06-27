package eu.kalafatic.evolution.controller.orchestration.capability.contracts;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.supervision.ActivationResolver;
import eu.kalafatic.evolution.controller.supervision.DecisionSnapshot;

/**
 * Contract for decision resolvers.
 */
public interface IResolverContract {
    String ID = "contract.resolver";

    void resolve(DecisionSnapshot snapshot);
}
