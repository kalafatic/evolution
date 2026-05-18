package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates clarification strategies based on intent expansion results.
 */
public class ClarificationPlanner {

    public enum Strategy {
        AUTO_INFER,      // Low ambiguity, proceed with assumptions
        BRANCH_PARALLEL, // Medium ambiguity, create parallel Darwin hypotheses
        CLARIFY_USER     // High ambiguity, must ask user
    }

    public Strategy determineStrategy(IntentExpansionResult result, eu.kalafatic.evolution.controller.orchestration.TaskContext context) {
        if (result.getConfidence() != null && result.getConfidence().getOverallConfidence() < 0.4) {
            return Strategy.CLARIFY_USER;
        }

        boolean highAmbiguity = result.getDimensions().stream()
                .anyMatch(d -> d.getAmbiguityScore() > 0.7 || d.isRequiresUserInput());

        if (highAmbiguity) {
            // Refinement: If heuristic analysis is very confident it's a simple atomic task,
            // be more lenient with LLM-discovered "ambiguities" to avoid clarification fatigue.
            if (context != null && context.getOrchestrationState() != null) {
                AtomicIntentAnalysis atomic = (AtomicIntentAnalysis) context.getOrchestrationState().getMetadata().get("atomicAnalysis");
                if (atomic != null && atomic.isAtomic() && atomic.getConfidence() >= 0.8 && !atomic.isMultiStep()) {
                    context.log("[KERNEL] High heuristic atomic confidence (" + atomic.getConfidence() + "). Proceeding despite potential ambiguities.");
                    return Strategy.AUTO_INFER;
                }
            }

            // Check for clarification fatigue
            if (context != null && context.getSemanticWorkspace() != null) {
                int priorClarifications = context.getSemanticWorkspace().findArtifactsByType("clarification-conclusion").size();
                if (priorClarifications >= 3 && result.getHypotheses().size() > 1) {
                    context.log("[KERNEL] Clarification fatigue detected (" + priorClarifications + "). Falling back to BRANCH_PARALLEL.");
                    return Strategy.BRANCH_PARALLEL;
                }
            }
            return Strategy.CLARIFY_USER;
        }

        if (result.getHypotheses().size() > 1) {
            return Strategy.BRANCH_PARALLEL;
        }

        return Strategy.AUTO_INFER;
    }

    public List<String> generateQuestions(IntentExpansionResult result) {
        return result.getDimensions().stream()
                .filter(d -> d.getAmbiguityScore() > 0.5 || d.isRequiresUserInput())
                .sorted(Comparator.comparingDouble(IntentDimension::getAmbiguityScore).reversed())
                .map(d -> "About " + d.getName() + ": " + d.getRationale() + " (Candidates: " + d.getCandidateValues() + ")")
                .collect(Collectors.toList());
    }

    public String formatClarificationRequest(IntentExpansionResult result) {
        List<String> questions = generateQuestions(result);
        if (questions.isEmpty()) return null;

        StringBuilder sb = new StringBuilder("I've analyzed your request but found some ambiguities that would help me provide a better result:\n\n");
        for (String q : questions) {
            sb.append("- ").append(q).append("\n");
        }
        sb.append("\nCould you please clarify these points?");
        return sb.toString();
    }
}
