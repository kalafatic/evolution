package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

import eu.kalafatic.evolution.controller.supervision.ActivationResolver;
import eu.kalafatic.evolution.controller.supervision.DecisionSnapshot;

/**
 * Contract for decision resolvers.
 */
public interface IResolverContract {
    String ID = "contract.resolver";

    void resolve(DecisionSnapshot snapshot);
}
