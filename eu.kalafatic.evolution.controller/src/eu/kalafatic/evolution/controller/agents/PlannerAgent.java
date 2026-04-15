package eu.kalafatic.evolution.controller.agents;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.IPlanner;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
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
        return "You are a workflow planner for an agentic system (Jules). " +
                "Your first step is to analyze the request needs and severity.\n\n" +
                "CATEGORIZATION:\n" +
                "- CONVERSATIONAL: Greetings, simple questions, or small talk. (Low Severity)\n" +
                "- INFORMATIONAL: Requests for system status, project overview, or explanations. (Medium Severity)\n" +
                "- OPERATIONAL: Requests that modify files, run builds, or execute shell commands. (High Severity)\n\n" +
                "AMBIGUITY HANDLING (The 'Jules' way):\n" +
                "- If a request is OPERATIONAL but lacks enough detail (e.g., 'create file a' without content or location), " +
                "DO NOT treat it as CONVERSATIONAL.\n" +
                "- Instead, create a task to provide thoughts, suggestions, and ask for clarification.\n" +
                "- Task name example: 'General: Clarify operational request'.\n" +
                "- Description should include: 'Analyze the ambiguous operational request. Provide suggestions on what the user might want (e.g., file extension, location, initial content) and ask for explicit approval/clarification before proceeding.'\n" +
                "- IMPORTANT: If the user explicitly asks to 'Execute the simplest working solution.', IGNORE the ambiguity and immediately plan the minimal functional tasks to achieve the goal based on shared memory.\n\n" +
                "STRATEGY:\n" +
                "- For CONVERSATIONAL requests, create a single 'llm' task and set 'approvalRequired' to false.\n" +
                "- For INFORMATIONAL requests, use agents to gather data. 'approvalRequired' should generally be false.\n" +
                "- For OPERATIONAL requests, decompose into specialized tasks. 'approvalRequired' should be true for tasks that change state.\n" +
                "- Prefer using explicit agent names in task titles (e.g., 'Architect: Analyze project', 'JavaDev: Implement feature').\n\n" +
                "Available task types:\n" +
                "- 'llm': For reasoning, planning, clarification, or general text generation.\n" +
                "- 'file': For writing or creating files. Task name: 'Write <path/to/file>'.\n" +
                "- 'shell': For executing shell commands.\n" +
                "- 'git': For version control actions.\n" +
                "- 'maven': For building or testing.\n" +
                "- 'approval': Explicitly pause for user input.\n\n" +
                "Output MUST be a valid JSON array of objects. Ensure no duplicate keys are present. All fields are REQUIRED.\n" +
                "Schema:\n" +
                "[ { \"id\": \"unique_id\", \"name\": \"[Agent]: [Action]\", \"description\": \"Detailed instructions for the agent\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\"|\"approval\", \"approvalRequired\": boolean, \"loopToTaskId\": \"id_to_jump_to\"|\"none\" } ]\n" +
                "EXAMPLES:\n" +
                "- Greeting: [ { \"id\": \"t1\", \"name\": \"General: Respond to greeting\", \"description\": \"Politely acknowledge user.\", \"taskType\": \"llm\", \"approvalRequired\": false } ]\n" +
                "- Ambiguous 'create file a': [ { \"id\": \"t1\", \"name\": \"General: Clarify file creation\", \"description\": \"Ask where 'a' should be created, what its extension is, and what content it should have. Provide suggestions.\", \"taskType\": \"llm\", \"approvalRequired\": false } ]\n" +
                "- Detailed request: [ { \"id\": \"t1\", \"name\": \"JavaDev: Create Main.java\", \"description\": \"Write a basic Hello World class to src/Main.java\", \"taskType\": \"file\", \"approvalRequired\": true } ]\n";
    }

    @Override
    public List<Task> plan(String request, TaskContext context) throws Exception {
        context.log("Planner: Decomposing request - " + request);

        // Use structured prompt building to include shared memory/history in planning
        String fullPrompt = buildPrompt(request, context, null);

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
            task.setDescription(obj.optString("description", ""));
            task.setType(obj.optString("taskType", "llm"));
            task.setApprovalRequired(obj.optBoolean("approvalRequired", false));
            task.setLoopToTaskId(obj.optString("loopToTaskId", "none"));
            tasks.add(task);
        }
        return tasks;
    }
}
