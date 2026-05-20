package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Spawner for Darwin evolutionary branch variants.
 * Executes isolated generation requests for each strategy seed.
 */
public class DarwinVariantSpawner {
    private final AiService aiService;
    private final DarwinVariantValidator validator;

    public DarwinVariantSpawner(AiService aiService) {
        this.aiService = aiService;
        this.validator = new DarwinVariantValidator();
    }

    /**
     * Spawns variants for the given strategies.
     */
    public List<JSONObject> spawn(String goal, List<DarwinStrategySeed> seeds, String basePrompt, TaskContext context) {
        List<JSONObject> variants = new ArrayList<>();
        Orchestrator orchestrator = context.getOrchestrator();

        for (DarwinStrategySeed seed : seeds) {
            context.log("[SPAWNER] Generating " + seed.getType() + " variant...");

            String seedPrompt = buildSeedPrompt(seed, basePrompt);
            JSONObject validated = null;

            for (int retry = 0; retry < 2; retry++) {
                try {
                    String response = aiService.sendRequest(orchestrator, seedPrompt, context);
                    validated = validator.validate(response, seed.getType(), context);
                    if (validated != null) {
                        break;
                    }
                    context.log("[SPAWNER] Validation failed for " + seed.getType() + ". Retry " + (retry + 1) + "/2...");
                } catch (Exception e) {
                    context.log("[SPAWNER] Error during generation for " + seed.getType() + ": " + e.getMessage());
                }
            }

            if (validated != null) {
                variants.add(validated);
                context.log("[SPAWNER] Successfully generated " + seed.getType() + " variant.");
            } else if (seed.isMandatory()) {
                context.log("[SPAWNER] FAILED to generate mandatory " + seed.getType() + " variant after retries.");
            }
        }

        return variants;
    }

    private String buildSeedPrompt(DarwinStrategySeed seed, String basePrompt) {
        return "SYSTEM:\n" +
               "You are generating ONE Darwin evolutionary branch variant.\n\n" +
               "RULES:\n" +
               "- Output EXACTLY ONE JSON object.\n" +
               "- Do NOT generate an array.\n" +
               "- strategy_type is FIXED to: " + seed.getType() + "\n" +
               "- Do NOT generate markdown code blocks (```json ... ```).\n" +
               "- Do NOT include conversational text or explanations outside the JSON.\n" +
               "- The variant MUST be semantically distinct but STRICTLY GROUNDED in the user goal.\n" +
               "- Avoid generic architectural advice; focus on concrete engineering actions for this specific task.\n\n" +
               "FIXED STRATEGY TYPE:\n" +
               seed.getType() + "\n\n" +
               "STRATEGY INSTRUCTIONS:\n" +
               seed.getInstructions() + "\n\n" +
               "CONTEXT AND GOAL:\n" +
               basePrompt + "\n\n" +
               "REQUIRED SCHEMA:\n" +
               "{\n" +
               "  \"id\": \"v-" + seed.getType().name().toLowerCase() + "\",\n" +
               "  \"strategy_type\": \"" + seed.getType() + "\",\n" +
               "  \"strategy\": \"high-level intent description\",\n" +
               "  \"survival_argument\": \"detailed justification of why this trajectory is valuable\",\n" +
               "  \"tradeoffs\": \"explicit technical tradeoffs\",\n" +
               "  \"failure_risks\": \"potential risks and failure modes\",\n" +
               "  \"pros_cons\": \"analysis of this specific hypothesis\",\n" +
               "  \"semantic_justification\": \"why this should exist in the architecture\",\n" +
               "  \"projected_steps\": [\"step 1\", \"step 2\"],\n" +
               "  \"expected_outputs\": [\"artifact 1\"],\n" +
               "  \"score\": 0.8,\n" +
               "  \"suffix\": \"" + seed.getType().name().toLowerCase() + "\",\n" +
               "  \"actions\": [\n" +
               "    {\n" +
               "      \"domain\": \"file|test|build|structure\",\n" +
               "      \"operation\": \"WRITE|DELETE|MKDIR|TEST|BUILD|ANALYZE\",\n" +
               "      \"target\": \"path/to/artifact\",\n" +
               "      \"description\": \"instruction for this action\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"hypothesis\": {\n" +
               "    \"description\": \"causal explanation\",\n" +
               "    \"expected_effects\": [\"outcome 1\"]\n" +
               "  },\n" +
               "  \"expected_effect\": {\n" +
               "    \"short_term\": \"string\",\n" +
               "    \"long_term\": \"string\",\n" +
               "    \"risk\": 0.1,\n" +
               "    \"reversibility\": 1.0\n" +
               "  }\n" +
               "}";
    }
}
