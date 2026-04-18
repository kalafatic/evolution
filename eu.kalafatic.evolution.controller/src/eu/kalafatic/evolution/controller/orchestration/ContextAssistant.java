package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Pre-processing assistant that improves user experience and mode selection.
 */
public class ContextAssistant {
    private final LlmRouter llmRouter = new LlmRouter();

    public ContextAssistResult analyze(String input, TaskContext context) throws Exception {
        Orchestrator orchestrator = context.getOrchestrator();
        ConversationState state = ConversationState.load(context.getSharedMemory(), context.getThreadId());

        StringBuilder sb = new StringBuilder();
        sb.append("You are a Context Assist layer for an AI platform.\n");
        sb.append("Your goal is to help users select the correct PlatformMode and provide missing inputs.\n\n");

        sb.append("SUPPORTED MODES:\n");
        sb.append("- SIMPLE_CHAT: normal conversation, casual questions, greetings.\n");
        sb.append("- ASSISTED_CODING: help me write/debug code, specific coding tasks.\n");
        sb.append("- DARWIN_MODE: try multiple solutions, optimize, iterate, complex problem solving.\n");
        sb.append("- SELF_DEV_MODE: improve yourself, refactor system, modify Evo's own code.\n");
        sb.append("- UNKNOWN: use this if you cannot determine the mode even with context.\n\n");

        sb.append("BEHAVIOR RULES:\n");
        sb.append("- Detect intent and suggest the most appropriate mode.\n");
        sb.append("- confidence: HIGH if intent is clear and all info is present. MEDIUM or LOW if clarification is needed.\n");
        sb.append("- If confidence is HIGH, auto-select mode.\n");
        sb.append("- If confidence is MEDIUM or LOW, prepare a short clarification question (max 2-3 options).\n");
        sb.append("- Collect missing info (missing code, unclear goal, no constraints) in 'missingInfo'.\n");
        sb.append("- Suggest optional steps for UX improvement in 'suggestedSteps'.\n");
        sb.append("- Default to SIMPLE_CHAT if unsure.\n");
        sb.append("- SAFETY: If intent suggests SELF_DEV_MODE, ALWAYS set confidence to MEDIUM to trigger explicit confirmation.\n\n");

        sb.append("Return ONLY JSON:\n");
        sb.append("{\n");
        sb.append("  \"mode\": \"SIMPLE_CHAT | ASSISTED_CODING | DARWIN_MODE | SELF_DEV_MODE | UNKNOWN\",\n");
        sb.append("  \"confidence\": \"LOW | MEDIUM | HIGH\",\n");
        sb.append("  \"clarifiedGoal\": \"...a concise summary of the goal...\",\n");
        sb.append("  \"missingInfo\": [\"item1\", \"item2\"],\n");
        sb.append("  \"suggestedSteps\": [\"step1\", \"step2\"]\n");
        sb.append("}\n\n");

        if (state.getLastMessages() != null && !state.getLastMessages().isEmpty()) {
            sb.append("--- CONVERSATION HISTORY ---\n");
            sb.append(String.join("\n", state.getLastMessages())).append("\n");
            sb.append("--- END HISTORY ---\n\n");
        }

        sb.append("User Input: \"").append(input).append("\"");

        String prompt = sb.toString();
        String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;
        String response = llmRouter.sendRequest(orchestrator, prompt, 0.0f, proxyUrl, context);

        return parseResponse(response);
    }

    private ContextAssistResult parseResponse(String response) {
        ContextAssistResult result = new ContextAssistResult();
        try {
            JSONObject json = new JSONObject(cleanResponse(response));

            String modeStr = json.optString("mode", "SIMPLE_CHAT");
            if ("UNKNOWN".equals(modeStr)) {
                result.setMode(null); // Explicitly unknown
            } else {
                try {
                    result.setMode(PlatformType.valueOf(modeStr));
                } catch (IllegalArgumentException e) {
                    result.setMode(PlatformType.SIMPLE_CHAT);
                }
            }

            String confStr = json.optString("confidence", "LOW");
            try {
                result.setConfidence(ConfidenceLevel.valueOf(confStr));
            } catch (IllegalArgumentException e) {
                result.setConfidence(ConfidenceLevel.LOW);
            }

            result.setClarifiedGoal(json.optString("clarifiedGoal", ""));

            JSONArray missing = json.optJSONArray("missingInfo");
            if (missing != null) {
                for (int i = 0; i < missing.length(); i++) {
                    result.getMissingInfo().add(missing.getString(i));
                }
            }

            JSONArray steps = json.optJSONArray("suggestedSteps");
            if (steps != null) {
                for (int i = 0; i < steps.length(); i++) {
                    result.getSuggestedSteps().add(steps.getString(i));
                }
            }
        } catch (Exception e) {
            // Fallback to defaults
        }
        return result;
    }

    private String cleanResponse(String response) {
        String trimmed = response.trim();
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
