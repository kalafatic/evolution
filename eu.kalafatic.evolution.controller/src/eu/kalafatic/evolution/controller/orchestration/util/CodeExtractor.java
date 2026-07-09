package eu.kalafatic.evolution.controller.orchestration.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Utility for extracting code from LLM responses containing markdown code blocks.
 */
public class CodeExtractor {

    /**
     * Extracts code from a string that may contain markdown code blocks.
     *
     * @param text The raw text from the LLM.
     * @return The extracted code or the original text if no block is found.
     */
    public static String extractCode(String text) {
        if (text == null) return "";
        String trimmed = text.trim();

        // 1. Strip <think>...</think> blocks (case-insensitive, multi-line)
        trimmed = trimmed.replaceAll("(?is)<think>.*?</think>", "").trim();
        if (trimmed.isEmpty()) return "";

        // 1.5. If the text already starts with common Java file starters, return it as-is
        if (trimmed.startsWith("package ") ||
            trimmed.startsWith("import ") ||
            trimmed.startsWith("public ") ||
            trimmed.startsWith("class ") ||
            trimmed.startsWith("interface ") ||
            trimmed.startsWith("enum ") ||
            trimmed.startsWith("abstract ") ||
            trimmed.startsWith("final ") ||
            trimmed.startsWith("@") ||
            trimmed.startsWith("//") ||
            trimmed.startsWith("/*")) {
            return trimmed;
        }

        // 2. Check if the text is a JSON object
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            String codeFromJson = extractFromJson(trimmed);
            if (codeFromJson != null && !codeFromJson.isEmpty()) {
                return codeFromJson;
            }
        }

        // 3. Check for markdown code blocks (``` ... ```)
        if (trimmed.contains("```")) {
            int firstBackticks = trimmed.indexOf("```");
            int lastBackticks = trimmed.lastIndexOf("```");
            if (lastBackticks > firstBackticks) {
                // Find the first newline after the backticks (to skip language identifiers like ```java or ```json)
                int firstNewline = trimmed.indexOf("\n", firstBackticks);
                String blockContent;
                if (firstNewline != -1 && lastBackticks > firstNewline) {
                    blockContent = trimmed.substring(firstNewline + 1, lastBackticks).trim();
                } else {
                    blockContent = trimmed.substring(firstBackticks + 3, lastBackticks).trim();
                }

                // If the extracted block itself is a JSON object, extract from it recursively
                if (blockContent.startsWith("{") && blockContent.endsWith("}")) {
                    String codeFromJson = extractFromJson(blockContent);
                    if (codeFromJson != null && !codeFromJson.isEmpty()) {
                        return codeFromJson;
                    }
                }

                // If the block is not JSON, it is likely the actual code
                if (!blockContent.isEmpty()) {
                    return blockContent;
                }
            }
        }

        // 4. Handle prefix labels like "CODE:", "Implementation:", "Java Code:" etc.
        Pattern labelPattern = Pattern.compile("(?i)(?:Implementation|CODE|Java Code|Source Code|Implementation Code):\\s*\\n?(.*)", Pattern.DOTALL);
        Matcher labelMatcher = labelPattern.matcher(trimmed);
        if (labelMatcher.find()) {
            String contentAfterLabel = labelMatcher.group(1).trim();
            // Recurse on the content after label
            String codeFromAfterLabel = extractCode(contentAfterLabel);
            if (!codeFromAfterLabel.isEmpty() && !codeFromAfterLabel.equals(contentAfterLabel)) {
                return codeFromAfterLabel;
            }
            trimmed = contentAfterLabel;
        }

        // 5. If we still have a JSON-like text, but not perfectly wrapped or has markdown around it
        // Try to find any JSON object substring
        if (trimmed.contains("{") && trimmed.contains("}")) {
            int firstBrace = trimmed.indexOf("{");
            int lastBrace = trimmed.lastIndexOf("}");
            if (lastBrace > firstBrace) {
                String potentialJson = trimmed.substring(firstBrace, lastBrace + 1).trim();
                String codeFromJson = extractFromJson(potentialJson);
                if (codeFromJson != null && !codeFromJson.isEmpty()) {
                    return codeFromJson;
                }
            }
        }

        // 6. Look for "class ", "interface ", "enum " signature fallback
        if (trimmed.contains("class ") || trimmed.contains("interface ") || trimmed.contains("enum ")) {
            // Find the earliest occurrence of imports or class declaration
            int startIdx = trimmed.indexOf("import ");
            if (startIdx == -1 || (trimmed.indexOf("class ") != -1 && startIdx > trimmed.indexOf("class "))) {
                startIdx = trimmed.indexOf("class ");
            }
            if (startIdx == -1 || (trimmed.indexOf("interface ") != -1 && trimmed.indexOf("interface ") < startIdx)) {
                startIdx = trimmed.indexOf("interface ");
            }
            if (startIdx == -1 || (trimmed.indexOf("enum ") != -1 && trimmed.indexOf("enum ") < startIdx)) {
                startIdx = trimmed.indexOf("enum ");
            }

            if (startIdx != -1) {
                String subset = trimmed.substring(startIdx).trim();
                // If there's a closing ``` after it, strip it
                int backticksIdx = subset.indexOf("```");
                if (backticksIdx != -1) {
                    return subset.substring(0, backticksIdx).trim();
                }
                return subset;
            }
        }

        return trimmed;
    }

    private static String extractFromJson(String jsonText) {
        try {
            JSONObject obj = new JSONObject(jsonText);
            // Step A: check common keys case-insensitively
            String[] preferredKeys = {
                "code", "CODE", "implementation", "IMPLEMENTATION",
                "java", "JAVA", "java_code", "JAVA_CODE",
                "source", "SOURCE", "source_code", "SOURCE_CODE",
                "class_code", "CLASS_CODE", "code_content", "CODE_CONTENT"
            };
            for (String key : preferredKeys) {
                if (obj.has(key)) {
                    String val = obj.optString(key, "");
                    if (val != null && !val.trim().isEmpty()) {
                        return val.trim();
                    }
                }
            }

            // Step B: If preferred keys are not found, iterate through all keys and find any string value containing "class " / "interface " or "import "
            for (Object keyObj : obj.keySet()) {
                String key = (String) keyObj;
                Object valObj = obj.get(key);
                if (valObj instanceof String) {
                    String valStr = (String) valObj;
                    if (valStr.contains("class ") || valStr.contains("interface ") || valStr.contains("import java.") || valStr.contains("public class ")) {
                        return valStr.trim();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore JSON parsing issues
        }
        return null;
    }
}
