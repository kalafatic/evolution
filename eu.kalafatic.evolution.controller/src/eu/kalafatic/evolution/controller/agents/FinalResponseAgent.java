package eu.kalafatic.evolution.controller.agents;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;

/**
 * Specialized agent for generating a simplistic but packed final response.
 */
public class FinalResponseAgent extends BaseAiAgent {

    public FinalResponseAgent() {
        super("FinalResponse", "FinalResponse");
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a Final Response Agent. Your goal is to provide a concise, human-readable summary of the work performed.\n\n" +
               "RULES:\n" +
               "- Be extremely concise.\n" +
               "- Focus on the high-level outcome.\n" +
               "- Do not include technical logs or Darwin internals.\n" +
               "- Do not include file lists (these are handled by another layer).\n" +
               "- Do not include conversational filler.";
    }

    @Override
    protected String getFooterInstructions() {
        return "Output only the summary text.";
    }

    public String generateFinalResponse(String request, List<Task> tasks, TaskContext context) throws Exception {
        StringBuilder input = new StringBuilder();
        input.append("ORIGINAL REQUEST: ").append(request).append("\n\n");
        input.append("TASKS PERFORMED:\n");

        for (Task task : tasks) {
            input.append("- Task: ").append(task.getName()).append("\n");
            input.append("  Status: ").append(task.getStatus()).append("\n");
            if (task.getResultSummary() != null) {
                input.append("  Result Summary: ").append(task.getResultSummary()).append("\n");
            }
            if (task.getFeedback() != null) {
                input.append("  Feedback: ").append(task.getFeedback()).append("\n");
            }
            input.append("\n");
        }

        return process(input.toString(), context, null);
    }
}
