package eu.kalafatic.evolution.controller.orchestration;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Specialized agent for planning tasks from natural language.
 */
public class PlannerAgent extends BaseAiAgent implements IPlanner {

    public PlannerAgent() {
        super("Planner", "Planner");
    }

    @Override
    public List<Task> plan(String request, TaskContext context) throws Exception {
        context.log("Planner: Decomposing request - " + request);

        String plannerPrompt = "You are a workflow planner for an agentic system. " +
                "Decompose the user request into a sequence of atomic, specialized tasks.\n" +
                "Available task types:\n" +
                "- 'llm': For reasoning, planning, or general text generation.\n" +
                "- 'file': For writing or creating files (e.g., Java source code, POM, README). Task name should be 'Write <path/to/file>'.\n" +
                "- 'git': For version control actions (add, commit, push).\n" +
                "- 'maven': For building, testing, or packaging the project.\n" +
                "- 'train_nn': For local project neural network training.\n" +
                "- 'train_llm': For local project LLM fine-tuning.\n" +
                "- 'train_agent': For local project agent behavior training.\n\n" +
                "Output MUST be a valid JSON array of objects. Schema:\n" +
                "[ { \"id\": \"unique_id\", \"name\": \"Clear task description\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\"|\"train_nn\"|\"train_llm\"|\"train_agent\" } ]\n\n" +
                "Request: " + request;

        String response = aiService.sendRequest(context.getOrchestrator(), plannerPrompt);
        context.log("Planner: Received response from AI: " + (response.length() > 100 ? response.substring(0, 100) + "..." : response));

        // Extracting JSON logic
        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");
        if (start == -1 || end == -1 || end <= start) {
            throw new Exception("LLM failed to return a valid JSON array. Response: " + response);
        }

        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(response.substring(start, end + 1));
        } catch (org.json.JSONException e) {
            throw new Exception("Failed to parse LLM response as JSON array: " + e.getMessage() + ". Response: " + response);
        }

        List<Task> tasks = new ArrayList<>();
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Task task = factory.createTask();
            task.setId(obj.optString("id", "task" + i));
            task.setName(obj.optString("name", "Task " + i));
            task.setType(obj.optString("taskType", "llm"));
            tasks.add(task);
        }
        context.log("Planner: Generated " + tasks.size() + " tasks.");
        return tasks;
    }
}
