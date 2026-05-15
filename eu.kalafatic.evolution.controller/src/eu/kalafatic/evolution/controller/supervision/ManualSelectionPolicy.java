package eu.kalafatic.evolution.controller.supervision;

import java.util.*;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ActivationRecommendation;

/**
 * Policy that respects explicit user selection.
 */
public class ManualSelectionPolicy implements ResolverPolicy {
    private final String manualSelection;

    public ManualSelectionPolicy(String manualSelection) {
        this.manualSelection = manualSelection;
    }

    @Override
    public DecisionSnapshot resolve(String iterationId, List<EvaluationSignal> signals, List<ActivationRecommendation> recommendations) {
        String reason = "User explicitly selected variant: " + manualSelection;
        return new DecisionSnapshot(
                iterationId,
                manualSelection,
                Collections.singletonList(manualSelection),
                new HashMap<>(),
                new ArrayList<>(),
                reason,
                getName(),
                1.0,
                "Manual Selection: " + manualSelection
        );
    }

    @Override
    public String getName() {
        return "ManualSelectionPolicy";
    }
}
