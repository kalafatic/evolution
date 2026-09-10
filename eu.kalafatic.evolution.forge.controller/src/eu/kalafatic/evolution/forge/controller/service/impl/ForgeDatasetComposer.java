package eu.kalafatic.evolution.forge.controller.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.forge.controller.api.ForgeJob.CompositionStrategy;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.ModelObjective;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.SourceProfile;

public class ForgeDatasetComposer {

    public CompositionStrategy computeComposition(List<SourceProfile> profiles, ModelObjective objective, long userTokenBudget) {
        CompositionStrategy strategy = new CompositionStrategy();
        if (userTokenBudget > 0) {
            strategy.setTokenBudget(userTokenBudget);
        } else {
            strategy.setTokenBudget(500_000_000L);
        }

        // Check if any source profile is an EVO_CODEBASE
        boolean hasEvoCodebase = false;
        if (profiles != null) {
            for (SourceProfile p : profiles) {
                if ("EVO_CODEBASE".equalsIgnoreCase(p.getSourceType()) || p.getSourcePath().toLowerCase().contains("evo")) {
                    hasEvoCodebase = true;
                    break;
                }
            }
        }

        // Adjust domain percentages based on target objective
        if (objective == ModelObjective.EVO_DEVELOPER_ASSISTANT || (objective == ModelObjective.AUTO && hasEvoCodebase)) {
            // Balanced mix: 40% General Conversation/Instruction (prevent catastrophic forgetting) + 60% EVO Codebase & Architecture
            strategy.setKnowledgePercent(0.30);
            strategy.setCodePercent(0.35);
            strategy.setInstructionPercent(0.20);
            strategy.setChatPercent(0.15);
        } else if (objective == ModelObjective.CODING) {
            strategy.setKnowledgePercent(0.20);
            strategy.setCodePercent(0.55);
            strategy.setInstructionPercent(0.15);
            strategy.setChatPercent(0.10);
        } else if (objective == ModelObjective.REASONING) {
            strategy.setKnowledgePercent(0.25);
            strategy.setCodePercent(0.35);
            strategy.setInstructionPercent(0.30);
            strategy.setChatPercent(0.10);
        } else if (objective == ModelObjective.CHAT) {
            strategy.setKnowledgePercent(0.20);
            strategy.setCodePercent(0.10);
            strategy.setInstructionPercent(0.30);
            strategy.setChatPercent(0.40);
        } else if (objective == ModelObjective.KNOWLEDGE) {
            strategy.setKnowledgePercent(0.70);
            strategy.setCodePercent(0.10);
            strategy.setInstructionPercent(0.10);
            strategy.setChatPercent(0.10);
        } else {
            // AUTO / GENERAL
            strategy.setKnowledgePercent(0.40);
            strategy.setCodePercent(0.25);
            strategy.setInstructionPercent(0.20);
            strategy.setChatPercent(0.15);
        }

        Map<String, Double> weights = new HashMap<>();
        if (profiles == null || profiles.isEmpty()) {
            strategy.setSourceWeights(weights);
            return strategy;
        }

        double totalQualityTokens = 0.0;
        for (SourceProfile p : profiles) {
            double qualityWeight = p.getQualityScore() > 0 ? p.getQualityScore() : 0.8;
            double effectiveTokens = p.getEstimatedTokens() * qualityWeight;
            totalQualityTokens += effectiveTokens;
        }

        for (SourceProfile p : profiles) {
            double qualityWeight = p.getQualityScore() > 0 ? p.getQualityScore() : 0.8;
            double weight = (totalQualityTokens > 0) ? (p.getEstimatedTokens() * qualityWeight) / totalQualityTokens : (1.0 / profiles.size());
            weights.put(p.getSourcePath(), weight);
        }

        strategy.setSourceWeights(weights);
        return strategy;
    }
}
