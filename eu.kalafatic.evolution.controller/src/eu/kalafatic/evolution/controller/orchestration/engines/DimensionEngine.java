package eu.kalafatic.evolution.controller.orchestration.engines;
import java.util.List;

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
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;

public class DimensionEngine {
    private final GenomeDimensionScheduler dimensionScheduler = new GenomeDimensionScheduler();
    private DimensionDiscoveryAgent discoveryAgent;

    public void setDiscoveryAgent(DimensionDiscoveryAgent discoveryAgent) {
        this.discoveryAgent = discoveryAgent;
    }

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

    public EvolutionDimension selectNextDimension(SemanticGenome genome, TaskContext context, GoalModel goal, Trajectory trajectory) {
        // STAGNATION HANDLING: If last iteration yielded no significant changes, boost priorities
        Boolean lastSignificant = (Boolean) context.getOrchestrationState().getMetadata().get("lastRealityCheckSignificant");
        if (lastSignificant != null && !lastSignificant) {
            handleStagnation(genome, context);
            context.getOrchestrationState().getMetadata().remove("lastRealityCheckSignificant");
        }

        EvolutionDimension activeDimension = dimensionScheduler.selectNextDimension(genome);
        if (activeDimension == null && discoveryAgent != null) {
            context.log("[DARWIN] Genome exhausted. Discovering new semantic dimensions...");
            try {
                List<EvolutionDimension> discovered = discoveryAgent.discover(goal, genome, trajectory, context);
                if (!discovered.isEmpty()) {
                    for (EvolutionDimension dim : discovered) {
                        genome.addDimension(dim);
                    }
                    // Re-schedule now that we have new dimensions
                    activeDimension = dimensionScheduler.selectNextDimension(genome);
                }
            } catch (Exception e) {
                context.log("[DARWIN] Dimension discovery failed: " + e.getMessage());
            }
        }

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

    /**
     * Handles evolutionary stagnation by re-prioritizing unlocked dimensions.
     * Increases evolutionary pressure and ambiguity scores to force exploration of new territories.
     */
    public void handleStagnation(SemanticGenome genome, TaskContext context) {
        context.log("[DARWIN] Stagnation detected. Re-evaluating dimension priorities to break local equilibrium.");
        for (EvolutionDimension dim : genome.getDimensions()) {
            if (!genome.isLocked(dim.getId())) {
                // Increase pressure and ambiguity to make this dimension more attractive for the next iteration
                double newPressure = Math.min(1.0, dim.getEvolutionaryPressure() + 0.25);
                double newAmbiguity = Math.min(1.0, dim.getAmbiguityScore() + 0.15);

                dim.setEvolutionaryPressure(newPressure);
                dim.setAmbiguityScore(newAmbiguity);

                context.log("[DARWIN] Boosted Dimension [" + dim.getId() + "] -> Pressure: " + newPressure + ", Ambiguity: " + newAmbiguity);
            }
        }
    }
}
