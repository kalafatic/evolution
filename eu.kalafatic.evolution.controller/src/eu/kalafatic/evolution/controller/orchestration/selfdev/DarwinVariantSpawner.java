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

        // Sequential Evolution: Collect summaries of already generated variants in this round
        List<String> currentRoundStrategies = new ArrayList<>();

        for (DarwinStrategySeed seed : seeds) {
            context.log("[SPAWNER] Generating " + seed.getType() + " variant...");

            String seedPrompt = buildSeedPrompt(seed, basePrompt, currentRoundStrategies);
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
                currentRoundStrategies.add(validated.optString("strategy"));
                context.log("[SPAWNER] Successfully generated " + seed.getType() + " variant.");
            } else if (seed.isMandatory()) {
                context.log("[SPAWNER] FAILED to generate mandatory " + seed.getType() + " variant after retries.");
            }
        }

        return variants;
    }

    private String buildSeedPrompt(DarwinStrategySeed seed, String basePrompt, List<String> currentRoundStrategies) {
        StringBuilder sb = new StringBuilder();
        sb.append("SYSTEM:\n")
          .append("You are generating ONE Darwin evolutionary branch variant.\n\n")
          .append("RULES:\n")
          .append("- Output EXACTLY ONE JSON object.\n")
          .append("- Do NOT generate an array.\n")
          .append("- strategy_type is FIXED to: ").append(seed.getType()).append("\n")
          .append("- Do NOT generate markdown code blocks (```json ... ```).\n")
          .append("- Do NOT include conversational text or explanations outside the JSON.\n")
          .append("- The variant MUST be semantically distinct but STRICTLY GROUNDED in the user goal.\n")
          .append("- Avoid generic architectural advice; focus on concrete engineering actions for this specific task.\n\n");

        if (!currentRoundStrategies.isEmpty()) {
            sb.append("SEQUENTIAL MUTATION DIVERGENCE:\n")
              .append("The following strategies have already been generated in this mutation chain. You MUST intentionally diverge from these architectural paths to ensure variety:\n");
            for (String s : currentRoundStrategies) {
                sb.append("- ").append(s).append("\n");
            }
            sb.append("\n");
        }

        sb.append("FIXED STRATEGY TYPE:\n")
          .append(seed.getType()).append("\n\n")
          .append("STRATEGY INSTRUCTIONS:\n")
          .append(seed.getInstructions()).append("\n\n")
          .append("CONTEXT AND GOAL:\n")
          .append(basePrompt).append("\n\n");

        return sb.toString() +
               "REQUIRED SCHEMA (CRITICAL: DO NOT echo placeholder text, provide REAL technical values):\n" +
               "{\n" +
               "  \"id\": \"v-" + seed.getType().name().toLowerCase() + "\",\n" +
               "  \"strategy_type\": \"" + seed.getType() + "\",\n" +
               "  \"strategy\": \"<precise engineering strategy for this task>\",\n" +
               "  \"survival_argument\": \"<why this branch should survive based on technical merit>\",\n" +
               "  \"tradeoffs\": \"<technical tradeoffs of this specific approach>\",\n" +
               "  \"failure_risks\": \"<potential failure modes for this branch>\",\n" +
               "  \"pros_cons\": \"<detailed pros/cons analysis>\",\n" +
               "  \"semantic_justification\": \"<architectural justification>\",\n" +
               "  \"projected_steps\": [\"<next logical step 1>\", \"<next logical step 2>\"],\n" +
               "  \"expected_outputs\": [\"<expected file/artifact 1>\"],\n" +
               "  \"score\": 0.0-1.0, // Numerical predicted fitness score\n" +
               "  \"suffix\": \"" + seed.getType().name().toLowerCase() + "\",\n" +
               "  \"actions\": [\n" +
               "    {\n" +
               "      \"domain\": \"file|test|build|structure\",\n" +
               "      \"operation\": \"WRITE|DELETE|MKDIR|TEST|BUILD|ANALYZE\",\n" +
               "      \"target\": \"<actual path or identifier>\",\n" +
               "      \"description\": \"<specific technical instruction for this action>\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"hypothesis\": {\n" +
               "    \"description\": \"<testable hypothesis for why this works>\",\n" +
               "    \"expected_effects\": [\"<measurable effect 1>\"]\n" +
               "  },\n" +
               "  \"expected_effect\": {\n" +
               "    \"short_term\": \"<expected result after execution>\",\n" +
               "    \"long_term\": \"<long-term architectural impact>\",\n" +
               "    \"risk\": 0.0-1.0, // Risk score\n" +
               "    \"reversibility\": 0.0-1.0 // Reversibility score\n" +
               "  }\n" +
               "}";
    }
}
