package eu.kalafatic.evolution.controller.orchestration.engines;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionNode;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;

public class LineageEngine {

    public String getParentId(EvolutionTree tree) {
        String nodeToExpandId = tree.getCurrentWinnerId();
        if (nodeToExpandId == null && tree.getRootId() != null) {
            nodeToExpandId = tree.getRootId();
        }
        return nodeToExpandId;
    }

    public String handleBranchRevival(EvolutionTree tree, String currentParentId, TaskContext context) {
        if (currentParentId != null) {
            EvolutionNode winnerNode = tree.getNode(currentParentId);
            if (winnerNode != null && winnerNode.getFitnessScore() < 0.3) {
                context.log("[DARWIN] Current lineage fitness low (" + winnerNode.getFitnessScore() + "). Attempting Branch Revival...");

                // Search for a rejected sibling with higher potential fitness (or simply any sibling)
                List<EvolutionNode> siblings = tree.getSiblings(currentParentId);
                EvolutionNode bestAlternative = siblings.stream()
                        .filter(s -> !"REJECTED_SEMANTIC".equals(s.getStatus()))
                        .sorted((a, b) -> Double.compare(b.getFitnessScore(), a.getFitnessScore()))
                        .findFirst().orElse(null);

                if (bestAlternative != null && (bestAlternative.getFitnessScore() > winnerNode.getFitnessScore())) {
                    context.log("[DARWIN] REVIVING BRANCH: " + bestAlternative.getMutationIdentity() + " (Fitness: " + bestAlternative.getFitnessScore() + ")");
                    return bestAlternative.getId();
                }
            }
        }
        return currentParentId;
    }

    public Trajectory getActiveTrajectory(TaskContext context) {
        IterationRecord lastWinner = context.getKernelContext().getMemoryService().getRecords().stream()
                .filter(r -> "ACTIVE".equals(r.getActivationState()))
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastWinner != null && lastWinner.getBranchId() != null) {
             return context.getSemanticWorkspace().getTrajectoryMemory().getTrajectory(lastWinner.getBranchId());
        }
        return null;
    }

    public void recordRejection(String goal, String message, TaskContext context) {
        IterationRecord record = new IterationRecord();
        record.setIteration(context.getOrchestrationState().getIterationCount());
        record.setGoal(goal);
        record.setStrategy("Darwin Variant Selection");
        record.setResult("FAIL");
        record.setStatus("REJECTED");
        record.setErrorMessage(message);
        record.setTimestamp(System.currentTimeMillis());
        context.getKernelContext().getMemoryService().saveRecord(record);
    }
}
