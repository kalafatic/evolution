package eu.kalafatic.evolution.controller.orchestration.capability.contracts;

import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.StateSnapshot;
import eu.kalafatic.evolution.controller.orchestration.selfdev.FailureMemory;
import eu.kalafatic.evolution.controller.orchestration.evolution.Trajectory;
import java.util.List;

/**
 * Contract for mutation engines (Darwin).
 */
public interface IMutationContract {
    String ID = "contract.mutation";

    List<BranchVariant> generateVariants(String goal, StateSnapshot snapshot, FailureMemory failureMemory, Trajectory trajectory) throws Exception;
}
