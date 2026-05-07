package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.ConversationState;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;

/**
 * Manages the clarification process when user requests are incomplete, ambiguous or contradictory.
 */
public class ClarificationManager {

    private double confidenceThreshold = EvolutionConstants.DEFAULT_CONFIDENCE_THRESHOLD;

    public void setConfidenceThreshold(double threshold) {
        this.confidenceThreshold = threshold;
    }

    /**
     * Determines if clarification is needed based on the analysis result.
     */
    public boolean shouldClarify(IntentAnalysisResult result) {
        if (result.getConfidenceScore() < confidenceThreshold) {
            return true;
        }
        return result.isAmbiguous();
    }

    /**
     * Generates a clarification question based on missing info, ambiguities and contradictions.
     */
    public String generateClarificationQuestion(IntentAnalysisResult result, TaskContext context) {
        if (result.getClarificationQuestion() != null && !result.getClarificationQuestion().trim().isEmpty()) {
            return result.getClarificationQuestion().trim();
        }

        StringBuilder sb = new StringBuilder();

        if (!result.getContradictions().isEmpty()) {
            sb.append("I've noticed some contradictions: ").append(String.join(", ", result.getContradictions())).append(". ");
        }

        if (!result.getMissingInformation().isEmpty()) {
            MissingRequirement req = result.getMissingInformation().get(0);
            sb.append("I'm missing some information about '").append(req.getField()).append("': ").append(req.getDescription()).append(". ");
        } else if (!result.getAmbiguities().isEmpty()) {
            Ambiguity amb = result.getAmbiguities().get(0);
            sb.append("The part '").append(amb.getPart()).append("' is a bit unclear: ").append(amb.getReason()).append(". ");
        } else if (result.getConfidenceScore() < confidenceThreshold) {
            sb.append("I'm not entirely sure I understand the goal. Could you provide more details? ");
        }

        sb.append("\n\nPlease clarify to proceed.");

        return sb.toString().trim();
    }

    /**
     * Updates the conversation state with clarification info.
     */
    public void updateState(ConversationState state, IntentAnalysisResult result, String question) {
        List<String> pending = new ArrayList<>();
        if (!result.getContradictions().isEmpty()) pending.addAll(result.getContradictions());
        for (MissingRequirement req : result.getMissingInformation()) pending.add(req.toString());
        for (Ambiguity amb : result.getAmbiguities()) pending.add(amb.toString());

        state.setPendingQuestions(pending);
        state.setRequirementMet(false);
        state.addMessage("Evo Clarification: " + question);
    }
}
