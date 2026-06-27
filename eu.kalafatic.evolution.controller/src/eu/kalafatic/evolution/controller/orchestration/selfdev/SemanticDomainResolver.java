package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes semantic domain strings from LLM output into SemanticDomain enums.
 */
public class SemanticDomainResolver {

    private static final Map<String, SemanticDomain> ALIAS_MAP = new HashMap<>();

    static {
        ALIAS_MAP.put("COMMUNICATION", SemanticDomain.COMMUNICATION);
        ALIAS_MAP.put("MESSAGE_IO", SemanticDomain.COMMUNICATION);
        ALIAS_MAP.put("OUTPUT", SemanticDomain.COMMUNICATION);
        ALIAS_MAP.put("PERSISTENCE", SemanticDomain.PERSISTENCE);
        ALIAS_MAP.put("STORAGE", SemanticDomain.PERSISTENCE);
        ALIAS_MAP.put("DATABASE", SemanticDomain.PERSISTENCE);
        ALIAS_MAP.put("EXECUTION", SemanticDomain.EXECUTION);
        ALIAS_MAP.put("LOGIC", SemanticDomain.EXECUTION);
        ALIAS_MAP.put("RESILIENCE", SemanticDomain.RESILIENCE);
        ALIAS_MAP.put("ERROR_HANDLING", SemanticDomain.RESILIENCE);
        ALIAS_MAP.put("STRUCTURE", SemanticDomain.STRUCTURE);
        ALIAS_MAP.put("MODEL", SemanticDomain.STRUCTURE);
        ALIAS_MAP.put("VALIDATION", SemanticDomain.VALIDATION);
        ALIAS_MAP.put("CHECK", SemanticDomain.VALIDATION);
    }

    public SemanticDomain resolve(String input) {
        if (input == null) return SemanticDomain.EXECUTION;

        String normalized = input.trim().toUpperCase().replace(" ", "_");

        // Direct match
        try {
            return SemanticDomain.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Alias map lookup
            if (ALIAS_MAP.containsKey(normalized)) {
                return ALIAS_MAP.get(normalized);
            }

            // Fuzzy match / Substring
            for (Map.Entry<String, SemanticDomain> entry : ALIAS_MAP.entrySet()) {
                if (normalized.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return SemanticDomain.EXECUTION; // Fallback
    }
}
