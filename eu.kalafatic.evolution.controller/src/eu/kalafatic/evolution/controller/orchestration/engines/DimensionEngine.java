package eu.kalafatic.evolution.controller.orchestration.engines;

import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractionLevel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GenomeDimensionScheduler;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticDomain;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticGenome;

public class DimensionEngine {
    private final GenomeDimensionScheduler dimensionScheduler = new GenomeDimensionScheduler();

    public SemanticGenome createGenome(GoalModel goal, IntentExpansionResult expansion, TaskContext context) {
        Object genomeObj = context.getOrchestrationState().getMetadata().get("semanticGenome");
        SemanticGenome genome = eu.kalafatic.evolution.controller.parsers.JsonUtils.restoreFromMetadata(genomeObj, SemanticGenome.class, "semanticGenome", context);
        if (genome != null && genome != genomeObj) {
            context.getOrchestrationState().getMetadata().put("semanticGenome", genome);
        }

        if (genome == null) {
            genome = new SemanticGenome(goal.getPrimaryAction());
            context.getOrchestrationState().getMetadata().put("semanticGenome", genome);
        }

        // Populate dimensions from intent expansion if available and not already present
        if (expansion != null && genome.getDimensions().isEmpty()) {
            context.log("[DARWIN] Seeding SemanticGenome with " + expansion.getUnresolvedDimensions().size() + " unresolved dimensions.");
            for (EvolutionDimension dim : expansion.getUnresolvedDimensions()) {
                genome.addDimension(dim);
            }
        }

        return genome;
    }

    public EvolutionDimension selectNextDimension(SemanticGenome genome, TaskContext context) {
        EvolutionDimension activeDimension = dimensionScheduler.selectNextDimension(genome);
        if (activeDimension == null) {
            // FALLBACK: Default Implementation dimension
            activeDimension = new EvolutionDimension("IMPLEMENTATION", "General implementation and refinement", AbstractionLevel.IMPLEMENTATION, SemanticDomain.EXECUTION);
        }

        context.getOrchestrationState().getMetadata().put("current_dimension", activeDimension.getId());
        context.log("[DARWIN] Scheduled Mutation Dimension: " + activeDimension.getId());
        return activeDimension;
    }

    public void detectUnresolvedDimensions(EvolutionAssessment initialAssessment, TaskContext context) {
        if (initialAssessment != null && initialAssessment.hasUnresolvedDimensions()) {
            context.log("[DARWIN] Unresolved dimensions detected: " +
                initialAssessment.getUnresolvedDimensions().stream().map(d -> d.getId()).collect(Collectors.joining(", ")));
        }
    }
}
