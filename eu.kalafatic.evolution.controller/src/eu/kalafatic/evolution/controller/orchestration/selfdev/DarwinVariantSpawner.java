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
    public List<JSONObject> spawn(String goal, List<DarwinStrategySeed> seeds, String basePrompt, String lineageContext, List<String> rejectedSiblings, TaskContext context) {
        List<JSONObject> variants = new ArrayList<>();
        Orchestrator orchestrator = context.getOrchestrator();

        // Sequential Evolution: Collect full JSON of already generated variants in this round
        List<JSONObject> currentRoundVariants = new ArrayList<>();

        for (DarwinStrategySeed seed : seeds) {
            context.log("[SPAWNER] Generating " + seed.getType() + " trajectory...");

            String seedPrompt = buildSeedPrompt(seed, basePrompt, lineageContext, rejectedSiblings, currentRoundVariants);
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
                currentRoundVariants.add(validated);
                context.log("[SPAWNER] Successfully generated " + seed.getType() + " trajectory.");
            } else if (seed.isMandatory()) {
                context.log("[SPAWNER] FAILED to generate mandatory " + seed.getType() + " trajectory after retries.");
            }
        }

        return variants;
    }

    private String buildSeedPrompt(DarwinStrategySeed seed, String basePrompt, String lineageContext, List<String> rejectedSiblings, List<JSONObject> currentRoundVariants) {
        StringBuilder sb = new StringBuilder();
        sb.append("SYSTEM:\n")
          .append("You are an adaptive engineering evolution engine generating ONE Darwin evolutionary branch trajectory.\n\n")
          .append("CRITICAL OBJECTIVE:\n")
          .append("Realize a specific COMPETING ENGINEERING FUTURE. Do NOT think in terms of roles (e.g., 'implementation', 'analytical').\n")
          .append("Think in terms of COMPETING ARCHITECTURAL PHILOSOPHIES and execution trajectories.\n\n")
          .append("RULES:\n")
          .append("- Output EXACTLY ONE JSON object.\n")
          .append("- Do NOT generate an array.\n")
          .append("- strategy_type is FIXED to: ").append(seed.getType()).append("\n")
          .append("- The variant MUST be semantically and conceptually distinct from previous trajectories.\n")
          .append("- Mutate the PHILOSOPHY, DEPTH, and TRADEOFFS, not just the wording.\n")
          .append("- Focus on concrete technical assumptions and operational strategies.\n")
          .append("- Do NOT include conversation or markdown blocks.\n\n");

        if (lineageContext != null && !lineageContext.isEmpty()) {
            sb.append("LINEAGE CONTINUITY (PERSISTENT EVOLUTION):\n")
              .append("You are evolving a surviving lineage. Inherit the successes and avoid the failures of your ancestors.\n")
              .append(lineageContext).append("\n");

            if (rejectedSiblings != null && !rejectedSiblings.isEmpty()) {
                sb.append("REJECTED SIBLING AWARENESS (DO NOT EXPLORE THESE PATHS):\n")
                  .append("The following trajectories were REJECTED in previous generations. Do NOT re-propose or pivot back to these engineering philosophies:\n");
                for (String rejected : rejectedSiblings) {
                    sb.append("- ").append(rejected).append("\n");
                }
                sb.append("\n");
            }
        }

        if (seed.getType() == DarwinStrategyType.PROBABLE_SURVIVOR) {
            sb.append("TRAJECTORY GOAL: PROBABLE SURVIVOR\n")
              .append("Propose the most direct and reliable engineering path to solve the goal.\n\n");
        }

        if (!currentRoundVariants.isEmpty()) {
            sb.append("EVOLUTIONARY MUTATION PRESSURE (PREVIOUS TRAJECTORIES):\n")
              .append("The following trajectories have already been explored in this round. You MUST intentionally mutate AWAY from their engineering philosophy:\n\n");
            for (JSONObject v : currentRoundVariants) {
                sb.append("--- Trajectory: ").append(v.optString("strategy_type")).append(" ---\n")
                  .append("Strategy: ").append(v.optString("strategy")).append("\n")
                  .append("Philosophy: ").append(v.optString("semantic_justification")).append("\n")
                  .append("Tradeoffs: ").append(v.optString("tradeoffs")).append("\n")
                  .append("Risks: ").append(v.optString("failure_risks")).append("\n\n");
            }
            sb.append("INSTRUCTION FOR THIS BRANCH: Maximize conceptual distance and tradeoff contrast from the above.\n\n");
        }

        if (seed.getInterpretation() != null) {
            sb.append("TARGET ENGINEERING FUTURE:\n")
              .append("Interpretation: ").append(seed.getInterpretation()).append("\n")
              .append("Architectural Assumption: ").append(seed.getAssumption()).append("\n")
              .append("Goal Detail: ").append(seed.getFutureGoal()).append("\n\n");
        }

        sb.append("TRAJECTORY CONTEXT:\n")
          .append(seed.getInstructions()).append("\n\n")
          .append("USER GOAL AND WORKSPACE CONTEXT:\n")
          .append(basePrompt).append("\n\n")
          .append("CRITICAL: If an EXPECTED TARGET ARTIFACT is provided in the context, you MUST use it in your actions. Do NOT use placeholders like 'actual path' or 'WRITE or DELETE'.\n\n");

        return sb.toString() +
               "REQUIRED SCHEMA (CRITICAL: PROVIDE SPECIFIC TECHNICAL VALUES):\n" +
               "{\n" +
               "  \"id\": \"v-" + seed.getType().name().toLowerCase() + "\",\n" +
               "  \"strategy_type\": \"" + seed.getType() + "\",\n" +
               "  \"strategy\": \"precise engineering strategy for this trajectory\",\n" +
               "  \"survival_argument\": \"why this specific future should survive technically\",\n" +
               "  \"tradeoffs\": \"specific technical tradeoffs compared to other trajectories\",\n" +
               "  \"failure_risks\": \"potential failure modes for this trajectory\",\n" +
               "  \"pros_cons\": \"detailed pros/cons analysis\",\n" +
               "  \"semantic_justification\": \"engineering philosophy justification\",\n" +
               "  \"projected_steps\": [\"next logical step 1\", \"next logical step 2\"],\n" +
               "  \"expected_outputs\": [\"expected file/artifact 1\"],\n" +
               "  \"score\": 0.5, // Numerical predicted fitness score between 0.0 and 1.0\n" +
               "  \"suffix\": \"" + seed.getType().name().toLowerCase() + "\",\n" +
               "  \"actions\": [\n" +
               "    {\n" +
               "      \"domain\": \"file\", // use: file, class, test, build, or structure\n" +
               "      \"operation\": \"WRITE\", // use: WRITE, DELETE, MKDIR, TEST, BUILD, or ANALYZE\n" +
               "      \"target\": \"actual_file_path.java\",\n" +
               "      \"description\": \"specific technical instruction for this action\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"hypothesis\": {\n" +
               "    \"description\": \"testable hypothesis for why this trajectory works\",\n" +
               "    \"expected_effects\": [\"measurable effect 1\"]\n" +
               "  },\n" +
               "  \"expected_effect\": {\n" +
               "    \"short_term\": \"expected result after execution\",\n" +
               "    \"long_term\": \"long-term architectural impact\",\n" +
               "    \"risk\": 0.5, // Risk score between 0.0 and 1.0\n" +
               "    \"reversibility\": 1.0 // Reversibility score between 0.0 and 1.0\n" +
               "  }\n" +
               "}";
    }
}
