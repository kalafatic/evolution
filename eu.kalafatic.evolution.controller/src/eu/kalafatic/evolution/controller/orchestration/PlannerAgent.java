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
    protected String getAgentInstructions() {
        return "You are acting as a Planner Agent. Your goal is to decompose high-level requests into atomic tasks.";
    }

    @Override
    public List<Task> plan(String request, TaskContext context) throws Exception {
        context.log("Planner: Decomposing request - " + request);

        String plannerPrompt = "You are a workflow planner for an agentic system. " +
                "Decompose the user request into a sequence of atomic, specialized tasks.\n" +
                "If the request is a simple greeting or a general question, just create one 'llm' task to respond.\n" +
                "Available task types:\n" +
                "- 'llm': For reasoning, planning, or general text generation.\n" +
                "- 'file': For writing or creating files (e.g., Java source code, POM, README). Task name should be 'Write <path/to/file>'. File paths MUST be relative to the project root and MUST NOT start with a slash or drive letter.\n" +
                "- 'shell': For executing shell commands. Use this for environment discovery (e.g., 'pwd', 'ls') or custom scripts.\n" +
                "- 'git': For version control actions (add, commit, push).\n" +
                "- 'maven': For building, testing, or packaging the project.\n" +
                "- 'approval': A specialized task that pauses the workflow and waits for the user to click 'Approve' or 'Reject'. Use this for critical steps like code application or final delivery.\n" +
                "- 'train_nn': For local project neural network training.\n" +
                "- 'train_llm': For local project LLM fine-tuning.\n" +
                "- 'train_agent': For local project agent behavior training.\n\n" +
                "Looping and Iteration:\n" +
                "- Any task can have a 'loopToTaskId' property. If present and not 'none', the orchestrator will jump back to the task with that ID after the current task completes.\n" +
                "- Use loops for iterative processes like: programmer-analyze -> improve -> implement -> test -> (loop to analyze if tests fail).\n\n" +
                "Output MUST be a valid JSON array of objects. Schema:\n" +
                "[ { \"id\": \"unique_id\", \"name\": \"Clear task description\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\"|\"approval\"|\"train_nn\"|\"train_llm\"|\"train_agent\", \"approvalRequired\": boolean, \"loopToTaskId\": \"id_to_jump_to\"|\"none\" } ]\n";

        // Use structured prompt building to include shared memory/history in planning
        String fullPrompt = buildPrompt(request, context, null);

        // Inject specialized planner instructions at the end of the built prompt's instructions section
        fullPrompt = fullPrompt.replace("INSTRUCTIONS:\n" + getAgentInstructions(), "INSTRUCTIONS:\n" + plannerPrompt);

        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Planner: Received response from AI: " + (response.length() > 100 ? response.substring(0, 100) + "..." : response));

        // Extracting JSON logic
        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");

        JSONArray jsonArray;
        if (start == -1 || end == -1 || end <= start) {
            context.log("Planner: Warning - AI response is not a JSON array. Using fallback llm task.");
            jsonArray = new JSONArray();
            JSONObject fallbackTask = new JSONObject();
            fallbackTask.put("id", "task0");
            fallbackTask.put("name", request);
            fallbackTask.put("taskType", "llm");
            jsonArray.put(fallbackTask);
        } else {
            try {
                jsonArray = new JSONArray(response.substring(start, end + 1));
            } catch (org.json.JSONException e) {
                context.log("Planner: Warning - Failed to parse AI response as JSON array. Using fallback llm task.");
                jsonArray = new JSONArray();
                JSONObject fallbackTask = new JSONObject();
                fallbackTask.put("id", "task0");
                fallbackTask.put("name", request);
                fallbackTask.put("taskType", "llm");
                jsonArray.put(fallbackTask);
            }
        }

        List<Task> tasks = new ArrayList<>();
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Task task = factory.createTask();
            task.setId(obj.optString("id", "task" + i));
            task.setName(obj.optString("name", "Task " + i));
            task.setType(obj.optString("taskType", "llm"));
            task.setApprovalRequired(obj.optBoolean("approvalRequired", false));
            task.setLoopToTaskId(obj.optString("loopToTaskId", "none"));
            tasks.add(task);
        }
        context.log("Planner: Generated " + tasks.size() + " tasks.");
        return tasks;
    }
}
