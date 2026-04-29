package eu.kalafatic.evolution.controller.agents;

import java.util.List;
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
        return "You are a Final Response Agent. Your goal is to provide a simplistic but packed summary of the work done.\n\n" +
               "STRUCTURE:\n" +
               "1. WORK DONE: A nice simple structured description of the work performed.\n" +
               "2. PLAN: If EVO created a plan, simply describe it in a few words.\n" +
               "3. FILES: If EVO created or modified files, list them as links using [FILE:path] format.\n" +
               "4. TROUBLES: If EVO had some troubles, describe them briefly.\n\n" +
               "RULES:\n" +
               "- Be extremely concise (simplistic but packed).\n" +
               "- Use bullet points for lists.\n" +
               "- Ensure file links are correct [FILE:path].\n" +
               "- Do not include unnecessary conversational filler.";
    }

    @Override
    protected String getFooterInstructions() {
        return "Output the final response directly, starting with the WORK DONE section.";
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
