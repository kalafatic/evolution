package eu.kalafatic.evolution.controller.orchestration.capability.contracts;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import java.util.List;

/**
 * Contract for cognitive introspection and tracing.
 */
public interface ITraceContract {
    String ID = "contract.trace";

    void addNode(CausalNode node);
    List<CausalNode> getNodes();
}
