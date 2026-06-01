package eu.kalafatic.evolution.controller.orchestration.intent;

/**
 * Evaluates the confidence score of the intent analysis.
 */
public class ConfidenceEvaluator {

    /**
     * Evaluates the confidence score for IntentExpansionResult.
     */
    public static double evaluate(IntentExpansionResult result) {
        double score = 1.0;

        if (result.getDominantIntent() == null || result.getDominantIntent().trim().isEmpty()) {
            score -= 0.4;
        }

        if (!result.getMissingInformation().isEmpty()) {
            score -= Math.min(0.4, result.getMissingInformation().size() * 0.1);
        }

        if (!result.getAmbiguities().isEmpty()) {
            score -= Math.min(0.2, result.getAmbiguities().size() * 0.05);
        }

        if (!result.getContradictions().isEmpty()) {
            score -= Math.min(0.3, result.getContradictions().size() * 0.15);
        }

        return Math.max(0.0, Math.min(1.0, score));
    }
}
