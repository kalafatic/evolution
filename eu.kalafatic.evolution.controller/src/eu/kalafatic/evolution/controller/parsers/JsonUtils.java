package eu.kalafatic.evolution.controller.parsers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * Extracts a JSONArray from a string, with fallback to converting a JSONObject to an array.
     * Useful for smaller models that might return a map of objects instead of a list.
     *
     * @param text The raw text from the LLM.
     * @return The extracted JSONArray or null if no valid data is found.
     */
    public static JSONArray extractJsonArrayFlexible(String text) {
        if (text == null) return null;

        // Strip <think> blocks
        text = text.replaceAll("(?is)<think>.*?</think>", "");

        int firstBracket = text.indexOf("[");
        int firstBrace = text.indexOf("{");

        // If bracket is first, try standard array extraction
        if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
            JSONArray arr = extractJsonArray(text);
            if (arr != null && arr.length() > 0) return arr;
        }

        // If brace is first or array extraction failed/empty, try object extraction and conversion
        if (firstBrace != -1) {
            JSONObject obj = extractJsonObject(text);
            if (obj != null) {
                // If the object contains a single key that is an array, return that array
                // (e.g. {"tasks": [...]})
                if (obj.length() == 1) {
                    String key = (String) obj.keys().next();
                    Object val = obj.get(key);
                    if (val instanceof JSONArray) {
                        return (JSONArray) val;
                    }
                }

                JSONArray arr = new JSONArray();
                List<String> keys = new ArrayList<>();
                obj.keys().forEachRemaining(k -> keys.add((String) k));

                // Sort keys if they appear to be numeric to preserve LLM intended order
                boolean allNumeric = keys.stream().allMatch(k -> k.matches("\\d+"));
                if (allNumeric) {
                    keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
                } else {
                    Collections.sort(keys);
                }

                for (String key : keys) {
                    Object val = obj.get(key);
                    if (val instanceof JSONObject) {
                        arr.put(val);
                    }
                }
                if (arr.length() > 0) return arr;
            }
        }

        // Final fallback: try array extraction again (in case it was after an object)
        return extractJsonArray(text);
    }
}
