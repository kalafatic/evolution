package eu.kalafatic.evolution.controller.parsers.structured;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Engine for repairing malformed JSON output from LLMs.
 */
public class JsonRepairEngine {

    public String repair(String text) {
        if (text == null || text.trim().isEmpty()) return text;

        String repaired = text.trim();

        // 1. Remove Markdown Wrappers and Code Fences
        repaired = removeMarkdown(repaired);

        // 2. Normalize Code Fences
        repaired = normalizeCodeFences(repaired);

        // 3. Strip conversational noise (like <think> blocks)
        repaired = stripNoise(repaired);

        // 4. Find the actual JSON part
        repaired = extractPotentialJson(repaired);

        // 5. Repair malformed brackets and truncated objects
        repaired = repairStructure(repaired);

        return repaired;
    }

    private String removeMarkdown(String text) {
        // Remove ```json ... ``` or ``` ... ```
        if (text.contains("```")) {
            int firstMatch = text.indexOf("```");
            int lastMatch = text.lastIndexOf("```");
            if (firstMatch != lastMatch) {
                String content = text.substring(firstMatch + 3, lastMatch).trim();
                if (content.startsWith("json")) {
                    content = content.substring(4).trim();
                }
                return content;
            }
        }
        return text;
    }

    private String normalizeCodeFences(String text) {
        // Sometimes LLMs use multiple fences or weird ones
        return text.replace("``", "`");
    }

    private String stripNoise(String text) {
        // Strip <think> blocks
        String cleaned = text.replaceAll("(?is)<think>.*?</think>", "");
        // Strip Log Contamination
        cleaned = cleaned.replaceAll("^\\[.*?\\]\\s+", "");
        cleaned = cleaned.replaceAll("^\\[.*?\\]\\s+\\[.*?\\]\\s+", "");
        return cleaned;
    }

    private String extractPotentialJson(String text) {
        int firstBrace = text.indexOf("{");
        int lastBrace = text.lastIndexOf("}");

        if (firstBrace != -1) {
            if (lastBrace > firstBrace) {
                return text.substring(firstBrace, lastBrace + 1);
            } else {
                return text.substring(firstBrace);
            }
        }
        return text;
    }

    private String repairStructure(String json) {
        if (json == null || json.isEmpty()) return json;

        List<Character> stack = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            sb.append(c);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{') stack.add('{');
                else if (c == '[') stack.add('[');
                else if (c == '}') {
                    if (!stack.isEmpty() && stack.get(stack.size() - 1) == '{') stack.remove(stack.size() - 1);
                } else if (c == ']') {
                    if (!stack.isEmpty() && stack.get(stack.size() - 1) == '[') stack.remove(stack.size() - 1);
                }
            }
        }

        String result = sb.toString().trim();

        if (inString) {
            result += "\"";
        }

        // Remove trailing comma if present (common in truncated JSON)
        while (result.endsWith(",") || result.endsWith(" ") || result.endsWith("\n") || result.endsWith("\r")) {
            result = result.substring(0, result.length() - 1).trim();
        }

        // Close objects and arrays in reverse order of their opening
        for (int i = stack.size() - 1; i >= 0; i--) {
            char open = stack.get(i);
            if (open == '{') result += "}";
            else if (open == '[') result += "]";
        }

        return result;
    }
}
