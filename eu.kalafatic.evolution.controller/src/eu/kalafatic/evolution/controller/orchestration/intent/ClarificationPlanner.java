package eu.kalafatic.evolution.controller.orchestration.intent;

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
        InterpretationState state = result.getState();
        context.consoleLog("[KERNEL] Determining clarification strategy for InterpretationState: " + state);

        // 1. CLEAR: one dominant interpretation -> proceed immediately
        if (state == InterpretationState.CLEAR) {
            return Strategy.AUTO_INFER;
        }

        // 2. EVOLVABLE: intent clear, multiple strategies -> spawn Darwin branches
        if (state == InterpretationState.EVOLVABLE) {
            return Strategy.BRANCH_PARALLEL;
        }

        // 3. RISK-BASED OVERRIDE: Clarify only when execution is blocked or high-risk
        if (state == InterpretationState.NEEDS_CLARIFICATION || state == InterpretationState.BLOCKED || state == InterpretationState.CONTRADICTORY) {

            // Refinement: If heuristic analysis is very confident it's a simple atomic task,
            // be more lenient with LLM-discovered "ambiguities" to avoid clarification fatigue.
            if (context != null && context.getOrchestrationState() != null) {
                AtomicIntentAnalysis atomic = (AtomicIntentAnalysis) context.getOrchestrationState().getMetadata().get("atomicAnalysis");
                if (atomic != null && atomic.isAtomic() && atomic.getConfidence() >= 0.8 && !atomic.isMultiStep()) {
                    context.log("[KERNEL] High heuristic atomic confidence (" + atomic.getConfidence() + "). Proceeding despite " + state + " state.");
                    return Strategy.AUTO_INFER;
                }
            }

            // Refinement: Clarify ONLY when execution risk is high or dominant confidence is low
            if (result.getDominantConfidence() < 0.6 || result.getExecutionRiskScore() > 0.7) {
                return Strategy.CLARIFY_USER;
            } else {
                context.log("[KERNEL] Ambiguity detected but risk is low and confidence is sufficient. Falling back to BRANCH_PARALLEL.");
                return Strategy.BRANCH_PARALLEL;
            }
        }

        // Fallback for legacy compatibility or undefined states
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

    public String formatClarificationRequest(IntentExpansionResult result, TaskContext context) {
        List<String> questions = generateQuestions(result);
        if (questions.isEmpty()) return null;

        StringBuilder sb = new StringBuilder("I've analyzed your request but found some ambiguities that would help me provide a better result:\n\n");
        for (String q : questions) {
            sb.append("- ").append(q).append("\n");
        }
        sb.append("\nCould you please clarify these points?");

        // Attach DARWIN_BRANCHES tag with hypotheses if available for semantic selection
        if (result.getHypotheses() != null && !result.getHypotheses().isEmpty()) {
            JSONObject json = new JSONObject();
            json.put("iteration", context.getOrchestrationState().getIterationCount());
            JSONArray variants = new JSONArray();
            for (eu.kalafatic.evolution.controller.orchestration.intent.IntentHypothesis h : result.getHypotheses()) {
                JSONObject v = new JSONObject();
                v.put("id", h.getId());
                v.put("strategy", h.getDescription());
                v.put("score", h.getConfidence());
                v.put("status", "RECOMMENDED");
                variants.put(v);
            }
            json.put("variants", variants);
            sb.append("\n\n[DARWIN_BRANCHES] ").append(json.toString());
        }

        return sb.toString();
    }
}
