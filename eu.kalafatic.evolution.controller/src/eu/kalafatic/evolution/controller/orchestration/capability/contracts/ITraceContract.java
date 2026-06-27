package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

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
