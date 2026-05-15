package eu.kalafatic.evolution.controller.trajectory.strategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

public class BranchSelector {

    private static final double DEFAULT_BUDGET = 2.0;

    public List<EvolutionBranch> select(TaskContext context) {
        List<EvolutionBranch> available = BranchRegistry.getAvailableStrategies();

        // 1. Filter by applicability
        List<EvolutionBranch> applicable = available.stream()
                .filter(s -> s.isApplicable(context))
                .collect(Collectors.toList());

        // 2. Score and sort by priority
        applicable.sort(Comparator.comparingDouble((EvolutionBranch s) -> s.getPriority(context)).reversed());

        // 3. Select within budget
        List<EvolutionBranch> selected = new ArrayList<>();
        double currentCost = 0;
        Object budgetObj = context.getOrchestrationState().getMetadata().getOrDefault("evolution_budget", DEFAULT_BUDGET);
        double budget = (budgetObj instanceof Number) ? ((Number) budgetObj).doubleValue() : DEFAULT_BUDGET;

        for (EvolutionBranch strategy : applicable) {
            double cost = strategy.getCost(context);
            if (currentCost + cost <= budget) {
                selected.add(strategy);
                currentCost += cost;
            }
        }

        context.log("[KERNEL] Selected " + selected.size() + " strategies (Budget: " + currentCost + "/" + budget + "): " +
                    selected.stream().map(s -> s.getType().name()).collect(Collectors.joining(", ")));

        return selected;
    }
}
