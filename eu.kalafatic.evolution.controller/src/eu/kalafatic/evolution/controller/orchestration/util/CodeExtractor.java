package eu.kalafatic.evolution.controller.orchestration.util;

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

        // Strip <think> blocks if any remain
        trimmed = trimmed.replaceAll("(?is)<think>.*?</think>", "").trim();

        if (trimmed.contains("```")) {
            int firstBackticks = trimmed.indexOf("```");
            int firstNewline = trimmed.indexOf("\n", firstBackticks);
            int lastBackticks = trimmed.lastIndexOf("```");

            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBackticks).trim();
            } else if (lastBackticks > firstBackticks + 3) {
                // Handle cases where there might not be a newline after the language tag
                // or the code block is inline-ish but using triple backticks
                return trimmed.substring(firstBackticks + 3, lastBackticks).trim();
            }
        }
        return trimmed;
    }
}
