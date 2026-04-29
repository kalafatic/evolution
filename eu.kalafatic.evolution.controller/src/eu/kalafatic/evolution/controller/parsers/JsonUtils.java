package eu.kalafatic.evolution.controller.parsers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Utility for robustly extracting and parsing JSON from LLM responses.
 *
 * @evo.lastModified: 15:A
 * @evo.origin: self
 */
public class JsonUtils {

    /**
     * Extracts a JSONObject from a string that may contain conversational noise.
     *
     * @param text The raw text from the LLM.
     * @return The extracted JSONObject or null if no valid object is found.
     */
    public static JSONObject extractJsonObject(String text) {
        if (text == null) return null;

        // Strip <think> blocks
        text = text.replaceAll("(?is)<think>.*?</think>", "");

        int firstStart = text.indexOf("{");
        int lastEnd = text.lastIndexOf("}");

        if (firstStart == -1 || lastEnd == -1 || lastEnd <= firstStart) {
            return null;
        }

        // Try simple extraction first (greedy)
        String fullPart = text.substring(firstStart, lastEnd + 1);
        try {
            return new JSONObject(fullPart);
        } catch (JSONException e) {
            // Greedy failed, likely multiple objects. Attempt to find the first valid one.
            int searchPos = firstStart;
            while (searchPos < lastEnd) {
                int end = text.indexOf("}", searchPos);
                if (end == -1) break;

                String candidate = text.substring(firstStart, end + 1);
                try {
                    return new JSONObject(candidate);
                } catch (JSONException ex) {
                    searchPos = end + 1;
                }
            }
        }
        return null;
    }

    /**
     * Extracts a JSONArray from a string that may contain conversational noise.
     *
     * @param text The raw text from the LLM.
     * @return The extracted JSONArray or null if no valid array is found.
     */
    public static JSONArray extractJsonArray(String text) {
        if (text == null) return null;

        // Strip <think> blocks
        text = text.replaceAll("(?is)<think>.*?</think>", "");

        int start = text.indexOf("[");
        int end = text.lastIndexOf("]");

        if (start != -1 && end != -1 && end > start) {
            String jsonPart = text.substring(start, end + 1);
            try {
                return new JSONArray(jsonPart);
            } catch (JSONException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Attempts to parse text as a JSONObject, with fallback to extraction.
     */
    public static JSONObject parseObject(String text) {
        try {
            return new JSONObject(text);
        } catch (JSONException e) {
            return extractJsonObject(text);
        }
    }

    /**
     * Attempts to parse text as a JSONArray, with fallback to extraction.
     */
    public static JSONArray parseArray(String text) {
        try {
            return new JSONArray(text);
        } catch (JSONException e) {
            return extractJsonArray(text);
        }
    }
}
