package eu.kalafatic.evolution.controller.parsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Utility for robustly extracting and parsing JSON from LLM responses.
 *
 * @evo.lastModified: 15:A
 * @evo.origin: self
 */
public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Robustly restores a typed object from a metadata value that may be a Map (deserialized from JSON)
     * or the object itself.
     */
    public static <T> T restoreFromMetadata(Object value, Class<T> type, String label, TaskContext context) {
        if (value == null) return null;
        if (type.isInstance(value)) return type.cast(value);

        if (value instanceof Map) {
            try {
                return mapper.convertValue(value, type);
            } catch (Exception e) {
                if (context != null) {
                    context.log("[JSON_UTILS] Warning: Failed to restore " + label + " from Map: " + e.getMessage());
                }
            }
        }
        return null;
    }

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

        // Strip Log Contamination (e.g., [Default] [DARWIN_BRANCHES])
        text = text.replaceAll("^\\[.*?\\]\\s+", "");
        text = text.replaceAll("^\\[.*?\\]\\s+\\[.*?\\]\\s+", "");

        // DARWIN TAG EXTRACTION: Prioritize content between explicit tags
        if (text.contains("<BEGIN_DARWIN_JSON>") && text.contains("<END_DARWIN_JSON>")) {
            int tagStart = text.indexOf("<BEGIN_DARWIN_JSON>") + "<BEGIN_DARWIN_JSON>".length();
            int tagEnd = text.indexOf("<END_DARWIN_JSON>");
            if (tagEnd > tagStart) {
                String tagged = text.substring(tagStart, tagEnd).trim();
                try {
                    return new JSONObject(tagged);
                } catch (JSONException e) {
                    // fall back to standard extraction
                }
            }
        }

        int firstStart = text.indexOf("{");
        int lastEnd = text.lastIndexOf("}");

        if (firstStart != -1 && lastEnd != -1 && lastEnd > firstStart) {
            // Try simple extraction first (greedy)
            String fullPart = text.substring(firstStart, lastEnd + 1);
            try {
                return new JSONObject(fullPart);
            } catch (JSONException e) {
                // Greedy failed, likely multiple objects or nested content.
                // Attempt to find the first valid one using balanced brace matching.
                int searchPos = firstStart;
                while (searchPos <= lastEnd) {
                    int start = text.indexOf("{", searchPos);
                    if (start == -1) break;

                    int braceCount = 0;
                    int end = -1;
                    for (int i = start; i < text.length(); i++) {
                        char c = text.charAt(i);
                        if (c == '{') braceCount++;
                        else if (c == '}') braceCount--;
                        if (braceCount == 0) {
                            end = i;
                            break;
                        }
                    }

                    if (end != -1) {
                        String candidate = text.substring(start, end + 1);
                        try {
                            return new JSONObject(candidate);
                        } catch (JSONException ex) {
                            searchPos = start + 1;
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        // Final fallback: try to repair truncated JSON if braces were present but failed to parse
        if (firstStart != -1 && (lastEnd == -1 || lastEnd < firstStart)) {
            String candidate = text.substring(firstStart);
            String repaired = repairTruncatedJson(candidate);
            try {
                return new JSONObject(repaired);
            } catch (JSONException e) {
                // repair failed
            }
        }

        // Final fallback: try to parse key-value pairs if standard JSON extraction failed or no braces found
        return attemptKeyValueParsing(text);
    }

    /**
     * Attempts to repair a truncated JSON string by adding missing closing braces and brackets.
     */
    private static String repairTruncatedJson(String json) {
        if (json == null) return null;

        List<Character> stack = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
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

        StringBuilder sb = new StringBuilder(json.trim());

        if (inString) {
            sb.append('"');
        }

        // Remove trailing comma if present (common in truncated JSON)
        int lastIdx = sb.length() - 1;
        while (lastIdx >= 0) {
            char c = sb.charAt(lastIdx);
            if (Character.isWhitespace(c) || c == ',') {
                sb.deleteCharAt(lastIdx);
                lastIdx = sb.length() - 1;
            } else {
                break;
            }
        }

        // Close objects and arrays in reverse order of their opening
        for (int i = stack.size() - 1; i >= 0; i--) {
            char open = stack.get(i);
            if (open == '{') sb.append('}');
            else if (open == '[') sb.append(']');
        }

        return sb.toString();
    }

    /**
     * Converts a JSONArray to a List of Strings, robustly handling objects or non-string elements.
     * Useful for smaller models that might return a list of objects instead of a list of strings.
     *
     * @param arr The JSONArray to convert.
     * @return A list of strings.
     */
    public static List<String> toStringList(JSONArray arr) {
        if (arr == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            Object obj = arr.opt(i);
            if (obj == null) continue;

            if (obj instanceof String) {
                result.add((String) obj);
            } else if (obj instanceof JSONObject) {
                JSONObject jObj = (JSONObject) obj;
                // Try specific fields often returned by LLMs for structured lists
                if (jObj.has("reason")) {
                    if (jObj.has("part")) {
                        result.add(jObj.optString("part", "") + ": " + jObj.optString("reason", ""));
                    } else {
                        result.add(jObj.optString("reason", ""));
                    }
                } else if (jObj.has("description")) {
                    if (jObj.has("field")) {
                        result.add(jObj.optString("field", "") + ": " + jObj.optString("description", ""));
                    } else {
                        result.add(jObj.optString("description", ""));
                    }
                } else if (jObj.has("value")) {
                    result.add(jObj.optString("value", ""));
                } else {
                    result.add(jObj.toString());
                }
            } else {
                result.add(obj.toString());
            }
        }
        return result;
    }

    /**
     * Fallback for small models that output "Key: Value" lines instead of JSON.
     */
    private static JSONObject attemptKeyValueParsing(String text) {
        JSONObject obj = new JSONObject();
        String[] lines = text.split("\\r?\\n");
        boolean foundAny = false;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("-")) continue;

            int colonPos = line.indexOf(":");
            if (colonPos > 0 && colonPos < line.length() - 1) {
                String rawKey = line.substring(0, colonPos).trim();
                String value = line.substring(colonPos + 1).trim();

                // Clean up key (remove quotes, dots, etc.)
                String key = rawKey.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$", "");

                if (!key.isEmpty()) {
                    // Normalize known keys to avoid case-sensitivity issues with fallbacks
                    String normalizedKey = normalizeKey(key);

                    // Try to parse value as boolean or number if possible
                    if ("true".equalsIgnoreCase(value)) obj.put(normalizedKey, true);
                    else if ("false".equalsIgnoreCase(value)) obj.put(normalizedKey, false);
                    else if (value.matches("-?\\d+")) obj.put(normalizedKey, Integer.parseInt(value));
                    else if (value.matches("-?\\d*\\.\\d+")) obj.put(normalizedKey, Double.parseDouble(value));
                    else obj.put(normalizedKey, value);
                    foundAny = true;
                }
            }
        }
        return foundAny ? obj : null;
    }

    private static String normalizeKey(String key) {
        if ("isambiguous".equalsIgnoreCase(key)) return "isAmbiguous";
        if ("refinedprompt".equalsIgnoreCase(key)) return "refinedPrompt";
        if ("objective".equalsIgnoreCase(key)) return "objective";
        if ("category".equalsIgnoreCase(key)) return "category";
        if ("intent".equalsIgnoreCase(key)) return "intent";
        if ("confidence".equalsIgnoreCase(key)) return "confidence";
        if ("missinginformation".equalsIgnoreCase(key)) return "missingInformation";
        if ("clarificationquestion".equalsIgnoreCase(key)) return "clarificationQuestion";
        if ("rootcause".equalsIgnoreCase(key)) return "rootCause";
        if ("repeatfailure".equalsIgnoreCase(key)) return "repeatFailure";
        if ("suggestedstrategy".equalsIgnoreCase(key)) return "suggestedStrategy";
        return key;
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

        // Strip Log Contamination
        text = text.replaceAll("^\\[.*?\\]\\s+", "");
        text = text.replaceAll("^\\[.*?\\]\\s+\\[.*?\\]\\s+", "");

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

        // 1. Try standard array extraction if [ is present and before {
        if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
            JSONArray arr = extractJsonArray(text);
            if (arr != null && arr.length() > 0) return arr;
        }

        // 2. Try to find multiple JSON objects and wrap them into an array
        JSONArray multiObjArr = new JSONArray();
        int searchPos = 0;
        while (searchPos < text.length()) {
            int start = text.indexOf("{", searchPos);
            if (start == -1) break;

            // Try to find the matching closing brace
            int braceCount = 0;
            int end = -1;
            for (int i = start; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;

                if (braceCount == 0) {
                    end = i;
                    break;
                }
            }

            if (end != -1) {
                String candidate = text.substring(start, end + 1);
                try {
                    JSONObject obj = new JSONObject(candidate);
                    multiObjArr.put(obj);
                    searchPos = end + 1;
                } catch (JSONException e) {
                    searchPos = start + 1;
                }
            } else {
                break;
            }
        }
        if (multiObjArr.length() > 1) return multiObjArr;

        // 3. Fallback: try object extraction and conversion if only one object was found or step 2 failed
        if (firstBrace != -1) {
            JSONObject obj = extractJsonObject(text);
            if (obj != null) {
                // If the object contains a single key that is an array, return that array
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

                // Sort keys if they appear to be numeric
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
                    } else if (val instanceof JSONArray) {
                        JSONArray subArr = (JSONArray) val;
                        for (int i = 0; i < subArr.length(); i++) {
                            arr.put(subArr.get(i));
                        }
                    }
                }
                if (arr.length() > 0) return arr;
            }
        }

        return extractJsonArray(text);
    }
}
