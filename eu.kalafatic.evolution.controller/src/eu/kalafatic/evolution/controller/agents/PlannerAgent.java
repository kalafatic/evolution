package eu.kalafatic.evolution.controller.agents;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.orchestration.IPlanner;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
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
    protected String getFooterInstructions() {
        return "You MUST output a valid JSON array of objects. Do not include any conversational preamble or follow-up text outside the JSON structure.";
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a workflow planner for an agentic system (Evo).\n" +
                "IMPORTANT: You only receive requests that have been classified as ACTION_REQUEST.\n\n" +
                "PLANNING RULES:\n" +
                "- NEVER invent domain data (e.g. if user says 'create file', don't assume a filename if not provided).\n" +
                "- ONLY use explicit user input or shared memory.\n" +
                "- If critical data is missing (e.g. filename, command), create a SINGLE clarification task instead of multiple operational steps.\n" +
                "- Task names must start with agent types (e.g., 'Architect: Analyze', 'JavaDev: Implement').\n\n" +
                "AMBIGUITY HANDLING:\n" +
                "- If the request is too vague to plan (e.g. 'create something'), return a task with name 'General: Request Clarification' and description asking ONE specific question.\n" +
                "- If the user says 'Execute the simplest working solution.', use shared memory to determine the path.\n\n" +
                "STRATEGY:\n" +
                "- Decompose operational requests into specialized tasks.\n" +
                "- 'approvalRequired' should be true for any task modifying files or running commands.\n\n" +
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
                "- Detailed request: [ { \"id\": \"t1\", \"name\": \"JavaDev: Create Main.java\", \"description\": \"Write a basic Hello World class to src/Main.java\", \"taskType\": \"file\", \"approvalRequired\": true } ]\n";
    }

    @Override
    public List<Task> plan(String request, TaskContext context) throws Exception {
        context.log("Planner: Decomposing request - " + request);

        // Step 3: Minimal Planner Guard (Updated for IntentGate Architecture)
        // This is a fail-safe in case the IntentGate incorrectly passed a non-actionable request.
        // It now also considers shared memory for context.
        String contextLower = (request + " " + context.getSharedMemory()).toLowerCase();
        if (!contextLower.matches(".*\\b(create|write|fix|generate|modify|delete|run|execute|add|implement|test|build|check|simple|example)\\b.*")) {
            context.log("Planner: Safety Guard - Request and context do not clearly contain an action. Returning clarification task.");
            List<Task> fallbackTasks = new ArrayList<>();
            Task t = OrchestrationFactory.eINSTANCE.createTask();
            t.setId("task0");
            t.setName("General: Ask for clarification");
            t.setDescription("I understand you want to take an action, but I need a more specific verb (e.g., create, fix, run) to proceed.");
            t.setType("llm");
            t.setApprovalRequired(false);
            fallbackTasks.add(t);
            return fallbackTasks;
        }

        // Adjust instructions based on PlatformMode
        PlatformMode mode = context.getPlatformMode();
        String modeInstructions = "";
        if (mode.getType() == PlatformType.ASSISTED_CODING) {
            modeInstructions = "\nPLATFORM MODE: ASSISTED_CODING. Keep the plan very simple (1-2 tasks max). Focus on the most direct solution.\n";
        } else if (mode.getType() == PlatformType.DARWIN_MODE) {
            modeInstructions = "\nPLATFORM MODE: DARWIN_MODE. Provide a comprehensive multi-step plan for evaluation.\n";
        }

        // Use structured prompt building to include shared memory/history in planning
        String fullPrompt = buildPrompt(request + modeInstructions, context, null);
        context.log("Evo-Planner-Thinking: " + fullPrompt);

        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Evo-Planner-Response: " + response);
        context.log("Planner: Received response from AI: " + (response.length() > 100 ? response.substring(0, 100) + "..." : response));

        // Use robust extraction
        JSONArray jsonArray = JsonUtils.extractJsonArray(response);

        if (jsonArray == null) {
            context.log("Planner: Warning - AI response is not a JSON array or failed parsing. Using fallback llm task.");
            jsonArray = new JSONArray();
            JSONObject fallbackTask = new JSONObject();
            fallbackTask.put("id", "task0");
            fallbackTask.put("name", request);
            fallbackTask.put("taskType", "llm");
            jsonArray.put(fallbackTask);
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
