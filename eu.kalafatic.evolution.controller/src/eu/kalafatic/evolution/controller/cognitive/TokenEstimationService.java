package eu.kalafatic.evolution.controller.cognitive;

/**
 * Single token-counting authority for the platform. Uses a character-based
 * estimator (chars/4) as the deterministic fallback; the estimate is always
 * reported as an estimate, never as exact usage. ENR usage counters (when
 * available) are the authoritative measured values for generation.
 */
public class TokenEstimationService {

    private static final TokenEstimationService INSTANCE = new TokenEstimationService();

    public static TokenEstimationService getInstance() {
        return INSTANCE;
    }

    /**
     * Estimated token count of the given text. Falls back to 0 for null/empty.
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Deterministic char/4 heuristic with a minimum of 1 token per
        // non-empty input; consistent with the existing ContextCurator
        // heuristic and safe as an upper-bound approximation.
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    public boolean fits(String text, int budgetTokens) {
        return estimateTokens(text) <= budgetTokens;
    }

    /**
     * Trims a list of scored items to fit a token budget, dropping the
     * lowest-scored items first. Returns the retained indices in order.
     */
    public java.util.List<Integer> selectWithinBudget(java.util.List<String> items,
            java.util.List<Double> scores, int budgetTokens) {
        java.util.List<Integer> kept = new java.util.ArrayList<>();
        if (items == null || items.isEmpty()) {
            return kept;
        }
        java.util.List<Integer> order = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            order.add(i);
        }
        // highest score first
        order.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        int used = 0;
        for (int idx : order) {
            int tokens = estimateTokens(items.get(idx));
            if (used + tokens > budgetTokens && !kept.isEmpty()) {
                continue;
            }
            kept.add(idx);
            used += tokens;
        }
        kept.sort(Integer::compareTo);
        return kept;
    }
}
