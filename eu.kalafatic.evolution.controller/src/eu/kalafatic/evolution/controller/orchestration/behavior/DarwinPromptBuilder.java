package eu.kalafatic.evolution.controller.orchestration.behavior;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.goal.SemanticEnvelope;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;
import eu.kalafatic.evolution.controller.orchestration.selfdev.MutationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticGenome;

/**
 * Unified builder for Darwin evolutionary prompts.
 * Assembles prompts from reusable sections to ensure consistency across the pipeline.
 */
public class DarwinPromptBuilder {
    private final StringBuilder sb = new StringBuilder();
    private final PromptComposer composer = new PromptComposer();
    private final TaskContext context;

    public DarwinPromptBuilder(TaskContext context) {
        this.context = context;
    }

    public DarwinPromptBuilder addSystem(String role) {
        sb.append("🔴 SYSTEM / ROLE\n\n").append(role).append("\n\n");
        return this;
    }

    public DarwinPromptBuilder addGoal(String goal) {
        sb.append(composer.composeGoal(goal)).append("\n\n");
        return this;
    }

    public DarwinPromptBuilder addCapability(eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType capability) {
        sb.append("🧠 CAPABILITY: ").append(capability).append("\n\n");
        return this;
    }

    public DarwinPromptBuilder addReality() {
        OrchestrationState state = context.getOrchestrationState();
        String projectStructure = (String) state.getMetadata().get("projectStructure");
        if (projectStructure != null) {
            sb.append("📂 REPOSITORY REALITY (GROUNDING SOURCE)\n\n")
              .append(projectStructure).append("\n\n");
        }

        eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel realityModel =
            (eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) state.getMetadata().get("targetRealityModel");
        if (realityModel != null) {
            sb.append("🎯 TARGET REALITY GROUNDING\n")
              .append("Domain: ").append(realityModel.getDomain()).append("\n")
              .append("Purpose: ").append(realityModel.getPurpose()).append("\n")
              .append("Hotspots: ").append(realityModel.getHotspots().stream().map(h -> h.getName()).collect(Collectors.joining(", "))).append("\n\n");
        }
        return this;
    }

    public DarwinPromptBuilder addSemanticEnvelope() {
        Object envObj = context.getOrchestrationState().getMetadata().get("semanticEnvelope");
        SemanticEnvelope envelope = null;
        if (envObj instanceof SemanticEnvelope) {
            envelope = (SemanticEnvelope) envObj;
        } else if (envObj instanceof Map) {
            envelope = new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .convertValue(envObj, SemanticEnvelope.class);
        }
        if (envelope != null) {
            sb.append("--- SEMANTIC ENVELOPE (EVOLUTIONARY MANDATE) ---\n")
              .append("MANDATORY CONCEPTS: ").append(envelope.getMandatoryConcepts()).append("\n")
              .append("ALLOWED MUTATION DIMENSIONS: ").append(envelope.getAllowedMutationDimensions()).append("\n")
              .append("FORBIDDEN SEMANTIC REGIONS: ").append(envelope.getForbiddenRegions()).append("\n")
              .append("MAX ABSTRACTION DEPTH: ").append(envelope.getMaxAbstractionDepth()).append("\n\n");
        }
        return this;
    }

    public DarwinPromptBuilder addGenomeMemory(SemanticGenome genome) {
        if (genome != null) {
            StringBuilder memSb = new StringBuilder();
            if (!genome.getRejectedMutations().isEmpty()) {
                memSb.append("FORBIDDEN MUTATIONS (REJECTED BY SEMANTIC VALIDATOR):\n");
                for (MutationRecord rejected : genome.getRejectedMutations()) {
                    memSb.append("- ").append(rejected.getStrategy()).append(" (Reason: ").append(rejected.getTradeoffs()).append(")\n");
                }
            }
            if (!genome.getDiscoveredMutations().isEmpty()) {
                memSb.append("\nEXPLORED MUTATIONS (ALREADY ATTEMPTED):\n");
                for (MutationRecord explored : genome.getDiscoveredMutations()) {
                    memSb.append("- ").append(explored.getStrategy()).append("\n");
                }
            }
            if (memSb.length() > 0) {
                sb.append(composer.composeSiblingMemory(memSb.toString())).append("\n\n");
            }
        }
        return this;
    }

    public DarwinPromptBuilder addLineage(String lineageContext) {
        if (lineageContext != null && !lineageContext.isEmpty()) {
            sb.append(composer.composeLineage(lineageContext)).append("\n\n");
        }
        return this;
    }

    public DarwinPromptBuilder addMutationDimension(EvolutionDimension dimension) {
        if (dimension != null) {
            sb.append("### ACTIVE MUTATION DIMENSION ###\n")
              .append("ID: ").append(dimension.getId()).append("\n")
              .append("DESCRIPTION: ").append(dimension.getDescription()).append("\n")
              .append("ABSTRACTION LEVEL: ").append(dimension.getAbstractionLevel()).append("\n")
              .append("MANDATE: Propose a unique implementation strategy strictly for THIS dimension. Keep other architectural decisions fixed.\n\n");
        }
        return this;
    }

    public DarwinPromptBuilder addConstraints(String constraints) {
        sb.append(composer.composeConstraints(constraints)).append("\n\n");
        return this;
    }

    public DarwinPromptBuilder addJsonSchema(String schema) {
        sb.append(composer.composeJsonSchema(schema)).append("\n\n");
        return this;
    }

    public DarwinPromptBuilder addExecutionDirective(String directive) {
        sb.append("🧭 EVOLUTION DIRECTIVE\n\n").append(directive).append("\n\n");
        return this;
    }

    public String build() {
        return sb.toString().trim();
    }
}
