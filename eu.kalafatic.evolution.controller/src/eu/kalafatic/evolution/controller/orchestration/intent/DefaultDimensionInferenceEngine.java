package eu.kalafatic.evolution.controller.orchestration.intent;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;

/**
 * Default implementation of DimensionInferenceEngine that delegates to IntentExpansionEngine.
 */
public class DefaultDimensionInferenceEngine implements DimensionInferenceEngine {

    private final IntentExpansionEngine expansionEngine;

    public DefaultDimensionInferenceEngine(IntentExpansionEngine expansionEngine) {
        this.expansionEngine = expansionEngine;
    }

    @Override
    public EvolutionAssessment analyze(String goal, TaskContext context) throws Exception {
        context.log("[KERNEL] Discovering unresolved semantic dimensions via DimensionInferenceEngine.");

        IntentExpansionResult expansion = expansionEngine.expand(goal, context);
        EvolutionAssessment assessment = new EvolutionAssessment();

        // Significance filtering
        List<EvolutionDimension> significant = expansion.getUnresolvedDimensions().stream()
            .filter(dim -> dim.getSignificanceScore() >= 0.2) // Threshold for cognitive granularity
            .collect(Collectors.toList());

        for (EvolutionDimension dim : significant) {
            assessment.addDimension(dim);
        }

        // Store expansion result for legacy/downstream usage
        context.getOrchestrationState().getMetadata().put("intentExpansion", expansion);

        return assessment;
    }
}
