package eu.kalafatic.evolution.controller.supervision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Collections;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.orchestration.workspace.TrajectoryMemory;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;

/**
 * ActivationResolver implements the "Survival of the Fittest" logic for Darwin branches.
 * It evaluates candidate variants using multiple scoring policies and selects the winner.
 */
public class ActivationResolver implements ICapability {

    public static final String ID = "eu.kalafatic.evolution.supervision.resolver";

    private final List<ResolverPolicy> policies;
    private final TrajectoryMemory memory;

    public ActivationResolver(TrajectoryMemory memory) {
        this.memory = memory;
        this.policies = new ArrayList<>();
        // Hardened policy stack
        this.policies.add(new SemanticCoherencePolicy());
        this.policies.add(new ComplexityCostPolicy());
        this.policies.add(new StabilityImpactPolicy());
        this.policies.add(new TrajectoryStabilityPolicy(memory));
        this.policies.add(new CriticalFailurePolicy());
        this.policies.add(new HighestScorePolicy());
    }

    public ActivationResolver() {
        this(null);
    }

    /**
     * Resolves the winner among competing variants.
     */
    public DecisionSnapshot resolve(String iterationId, List<BranchVariant> variants, List<EvaluationSignal> signals) {
        if (variants == null || variants.isEmpty()) {
            return createNullDecision(iterationId);
        }

        Map<String, Map<String, Double>> policyScores = new HashMap<>();
        Map<String, Double> aggregatedScores = new HashMap<>();

        // 1. Calculate individual policy scores
        for (BranchVariant variant : variants) {
            Map<String, Double> scores = new HashMap<>();
            double totalScore = 0.0;

            // Integrate Signal feedback into scoring
            double signalBoost = calculateSignalBoost(variant, signals);

            for (ResolverPolicy policy : policies) {
                double score = policy.evaluate(variant);
                scores.put(policy.getClass().getSimpleName(), score);
                totalScore += score;
            }

            double finalScore = (totalScore / policies.size()) * 0.7 + (signalBoost * 0.3);
            policyScores.put(variant.getId(), scores);
            aggregatedScores.put(variant.getId(), finalScore);
        }

        // 2. Rank variants
        List<String> rankedIds = variants.stream()
                .map(BranchVariant::getId)
                .sorted((id1, id2) -> Double.compare(aggregatedScores.get(id2), aggregatedScores.get(id1)))
                .collect(Collectors.toList());

        String winnerId = rankedIds.get(0);
        double confidence = calculateConfidence(aggregatedScores, winnerId);
        double disagreement = calculateDisagreement(policyScores, winnerId);

        String reason = generateReason(winnerId, aggregatedScores.get(winnerId), disagreement, signals);

        DecisionSnapshot snapshot = new DecisionSnapshot(
                iterationId,
                winnerId,
                rankedIds,
                aggregatedScores,
                new ArrayList<>(), // Critical failures
                reason,
                "MultiPolicyWeightedResolver",
                confidence,
                "Survival based on Semantic, Complexity, Stability and Signal feedback",
                disagreement
        );

        // Update trajectories with fitness history
        updateTrajectoryMetrics(variants, aggregatedScores);

        // Set re-evaluation flags
        if (disagreement > 0.6) {
            snapshot.setExplorationTriggered(true);
        }

        // Track stability over time
        snapshot.setAvgLongTermStability(calculateStabilityTrend(winnerId));

        return snapshot;
    }

    private double calculateSignalBoost(BranchVariant variant, List<EvaluationSignal> signals) {
        if (signals == null || signals.isEmpty()) return 0.5;
        return signals.stream()
            .filter(s -> s.getVariantId().equals(variant.getId()))
            .mapToDouble(EvaluationSignal::getScore)
            .average()
            .orElse(0.5);
    }

    private double calculateConfidence(Map<String, Double> aggregatedScores, String winnerId) {
        if (aggregatedScores.size() < 2) return 1.0;
        double winnerScore = aggregatedScores.get(winnerId);
        double runnerUpScore = aggregatedScores.values().stream()
                .filter(s -> s < winnerScore)
                .max(Double::compare)
                .orElse(0.0);
        return Math.min(1.0, (winnerScore - runnerUpScore) / (1.0 - runnerUpScore + 0.1));
    }

    private double calculateDisagreement(Map<String, Map<String, Double>> policyScores, String winnerId) {
        Map<String, Double> winnerPolicyScores = policyScores.get(winnerId);
        if (winnerPolicyScores == null || winnerPolicyScores.size() < 2) return 0.0;

        double min = winnerPolicyScores.values().stream().mapToDouble(d -> d).min().orElse(0.0);
        double max = winnerPolicyScores.values().stream().mapToDouble(d -> d).max().orElse(1.0);
        return max - min;
    }

    private String generateReason(String winnerId, double score, double disagreement, List<EvaluationSignal> signals) {
        StringBuilder sb = new StringBuilder();
        sb.append("Variant ").append(winnerId).append(" selected with fitness ").append(String.format("%.2f", score));
        long variantSignals = signals.stream().filter(s -> s.getVariantId().equals(winnerId)).count();
        sb.append(" based on ").append(variantSignals).append(" reality signals.");
        if (disagreement > 0.4) {
            sb.append(" Warning: High policy disagreement (").append(String.format("%.2f", disagreement)).append(")");
        }
        return sb.toString();
    }

    private void updateTrajectoryMetrics(List<BranchVariant> variants, Map<String, Double> aggregatedScores) {
        if (memory == null) return;
        for (BranchVariant variant : variants) {
            Trajectory t = memory.getTrajectory(variant.getTrajectoryId());
            if (t != null) {
                double score = aggregatedScores.get(variant.getId());
                t.setFitnessScore(score);
                t.getFitnessHistory().add(score);

                // Adaptive step forecasting: if fitness is high, extend projection
                if (score > 0.8 && t.getProjectedSteps().size() < 3) {
                    t.getProjectedSteps().add("REINFORCE: " + variant.getStrategy());
                }
            }
        }
    }

    private double calculateStabilityTrend(String variantId) {
        // Placeholder for stability trend analysis via TrajectoryMemory
        return 0.75;
    }

    private DecisionSnapshot createNullDecision(String iterationId) {
        return new DecisionSnapshot(iterationId, "NONE", new ArrayList<>(), new HashMap<>(),
                List.of("No variants available"), "Execution halted: No viable candidates", "NullResolver", 0.0, "N/A", 0.0);
    }

    @Override
    public String getCapabilityId() {
        return ID;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CapabilityStatus getStatus() {
        return CapabilityStatus.INITIALIZED;
    }

    @Override
    public void initialize(CapabilityContext context) throws CapabilityException {
        // No-op
    }

    @Override
    public void start() throws CapabilityException {
        // No-op
    }

    @Override
    public void stop() throws CapabilityException {
        // No-op
    }

    @Override
    public List<String> getSupportedContracts() {
        return Collections.singletonList("eu.kalafatic.evolution.supervision.IResolverContract");
    }

    @Override
    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public CapabilityHealth getHealth() {
        return new CapabilityHealth(1.0, "Healthy", 0);
    }
}
