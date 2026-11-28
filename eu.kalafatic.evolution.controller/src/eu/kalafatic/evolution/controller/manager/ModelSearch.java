package eu.kalafatic.evolution.controller.manager;

import java.util.Locale;

/**
 * Shared search/filter semantics for the model management UI and tests.
 * Search is case-insensitive, supports multiple whitespace separated terms and
 * always operates on the complete dataset supplied by the caller.
 */
public final class ModelSearch {

    private ModelSearch() {
    }

    /**
     * True when every whitespace-separated term occurs in at least one field.
     */
    public static boolean matchesAllTerms(String query, String... fields) {
        if (query == null || query.isBlank()) {
            return true;
        }
        StringBuilder haystack = new StringBuilder();
        if (fields != null) {
            for (String field : fields) {
                if (field != null) {
                    haystack.append(field).append(' ');
                }
            }
        }
        String text = haystack.toString().toLowerCase(Locale.ROOT);
        for (String term : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!term.isEmpty() && !text.contains(term)) {
                return false;
            }
        }
        return true;
    }

    /**
     * True when the configured selection is "All"/empty or equals one of the
     * candidate values (case-insensitive).
     */
    public static boolean matchesSelection(String selected, String... candidates) {
        if (selected == null || selected.isBlank() || "All".equalsIgnoreCase(selected)) {
            return true;
        }
        if (candidates != null) {
            for (String candidate : candidates) {
                if (candidate != null && selected.equalsIgnoreCase(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }
}
