package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;

/**
 * Contract for cognitive introspection and tracing.
 */
public interface ITraceContract {
    String ID = "contract.trace";

    void addNode(CausalNode node);
    List<CausalNode> getNodes();
}
