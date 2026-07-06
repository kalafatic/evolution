package eu.kalafatic.evolution.controller.orchestration.engines;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractionLevel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticDomain;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticDomainResolver;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticGenome;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

/**
 * AI Agent for discovering new semantic dimensions when the current genome is exhausted.
 * This ensures evolution can continue by identifying new technical decision points.
 */
public class DimensionDiscoveryAgent extends BaseAiAgent {

    public DimensionDiscoveryAgent(SessionContainer container) {
        super("DimensionDiscoveryAgent", "DimensionDiscoveryAgent", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a Semantic Dimension Discovery Agent (REALITY-GROUNDED).\n\n" +
               "GOAL: Identify new technical OR CONCEPTUAL decision points (dimensions) to continue evolutionary progress.\n" +
               "You are called when all previously identified dimensions have been resolved (locked).\n\n" +
               "CONTEXT:\n" +
               "1. The high-level GOAL of the organism.\n" +
               "2. The current SEMANTIC GENOME (locked dimensions and mutation history).\n" +
               "3. The active TRAJECTORY lineage.\n" +
               "4. TARGET REALITY MODEL (Hotspots, Knowledge Gaps, Facts, Subsystems).\n\n" +
               "TASK:\n" +
               "Discover 1-3 NEW unresolved semantic dimensions that represent meaningful technical or informational polymorphism.\n" +
               "CRITICAL: Dimensions MUST be rooted in the identified HOTSPOTS, FACTS, or KNOWLEDGE GAPS of the environment.\n" +
               "Avoid generic dimensions. Focus on specific challenges identified in Reality Discovery.\n\n" +
               "RULES:\n" +
               "1. Do NOT repeat locked dimensions.\n" +
               "2. Root dimensions in 'Target Reality' if provided.\n" +
               "3. Use appropriate Abstraction Levels (PHILOSOPHY, STRATEGY, ARCHITECTURE, DESIGN, IMPLEMENTATION, SYNTAX, CONCEPT, INTENT, KNOWLEDGE_BASE).\n" +
               "4. Use appropriate Semantic Domains (EXECUTION, PERSISTENCE, RESILIENCE, COMMUNICATION, STRUCTURE, VALIDATION, KNOWLEDGE, DATA, MEDIA).\n\n" +
               "OUTPUT FORMAT (JSON ONLY):\n" +
               "{\n" +
               "  \"dimensions\": [\n" +
               "    {\n" +
               "      \"id\": \"string_id\",\n" +
               "      \"description\": \"description of the decision point\",\n" +
               "      \"abstractionLevel\": \"LEVEL\",\n" +
               "      \"semanticDomain\": \"DOMAIN\",\n" +
               "      \"significanceScore\": float (0.0-1.0),\n" +
               "      \"ambiguityScore\": float (0.0-1.0),\n" +
               "      \"evolutionaryPressure\": float (0.0-1.0)\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }

    public List<EvolutionDimension> discover(GoalModel goal, SemanticGenome genome, Trajectory trajectory, TaskContext context) throws Exception {
        context.log("[DIMENSION_DISCOVERY] Discovering new dimensions for: " + goal.getPrimaryAction());

        StringBuilder sb = new StringBuilder();

        eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel realityModel = (eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) context
                .getOrchestrationState().getMetadata().get("targetRealityModel");
        if (realityModel != null) {
            sb.append("\n--- DISCOVERED TARGET REALITY (GROUNDING SOURCE) ---\n");
            sb.append("Architecture Summary: ").append(realityModel.getArchitectureSummary()).append("\n");

            if (!realityModel.getArchitecturalFacts().isEmpty()) {
                sb.append("\nARCHITECTURAL FACTS:\n");
                for (var f : realityModel.getArchitecturalFacts()) {
                    sb.append("- ").append(f.toString()).append("\n");
                }
            }

            sb.append("\nIDENTIFIED HOTSPOTS (PRIORITY EVOLUTION TARGETS):\n");
            for (eu.kalafatic.evolution.controller.mediation.model.Hotspot hotspot : realityModel.getHotspots()) {
                sb.append("- ").append(hotspot.getName()).append(" [").append(hotspot.getType()).append("]: ")
                        .append(hotspot.getDescription()).append(" (Significance: ").append(hotspot.getSignificance())
                        .append(")\n");
            }
            sb.append("\n");
        }

        sb.append("GOAL: ").append(goal.getPrimaryAction()).append("\n\n");

        sb.append("LOCKED DIMENSIONS (RESOLVED):\n");
        if (genome.getLockedDimensions().isEmpty()) {
            sb.append("- None\n");
        } else {
            for (String locked : genome.getLockedDimensions()) {
                sb.append("- ").append(locked).append("\n");
            }
        }
        sb.append("\n");

        sb.append("MUTATION HISTORY:\n");
        if (genome.getDiscoveredMutations().isEmpty()) {
            sb.append("- None\n");
        } else {
            for (var mut : genome.getDiscoveredMutations()) {
                sb.append("- ").append(mut.getStrategy()).append(" (Dimension: ").append(mut.getEngineeringDimensions().get("active_dimension")).append(")\n");
            }
        }

        if (trajectory != null && !trajectory.getMutationLineage().isEmpty()) {
             sb.append("\nTRAJECTORY LINEAGE:\n");
             for (String l : trajectory.getMutationLineage()) {
                 sb.append("- ").append(l).append("\n");
             }
        }

        String response = aiService.sendRequest(context.getOrchestrator(), buildPrompt(sb.toString(), context, null), context);
        JSONObject json = JsonUtils.extractJsonObject(response);

        List<EvolutionDimension> discovered = new ArrayList<>();
        if (json != null && json.has("dimensions")) {
            JSONArray array = json.getJSONArray("dimensions");
            SemanticDomainResolver domainResolver = new SemanticDomainResolver();
            for (int i = 0; i < array.length(); i++) {
                JSONObject dimObj = array.getJSONObject(i);
                String id = dimObj.getString("id");

                // Avoid duplicates
                if (genome.isLocked(id)) continue;

                String levelStr = dimObj.optString("abstractionLevel", "IMPLEMENTATION");
                AbstractionLevel level = AbstractionLevel.IMPLEMENTATION;
                try {
                    level = AbstractionLevel.valueOf(levelStr.trim().toUpperCase());
                } catch (Exception e) {}

                EvolutionDimension dim = new EvolutionDimension(
                    id,
                    dimObj.optString("description"),
                    level,
                    domainResolver.resolve(dimObj.optString("semanticDomain", "EXECUTION"))
                );
                dim.setSignificanceScore(dimObj.optDouble("significanceScore", 0.5));
                dim.setAmbiguityScore(dimObj.optDouble("ambiguityScore", 0.0));
                dim.setEvolutionaryPressure(dimObj.optDouble("evolutionaryPressure", 0.0));
                discovered.add(dim);
            }
        }

        context.log("[DIMENSION_DISCOVERY] Discovered " + discovered.size() + " new dimensions.");
        return discovered;
    }
}
