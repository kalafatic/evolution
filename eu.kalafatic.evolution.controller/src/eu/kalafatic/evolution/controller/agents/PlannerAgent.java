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
        return "You are a workflow planner for Evo, an architect-first AI system.\n\n" +
                "RULES:\n" +
                "- MVP FIRST: Always start with a working implementation of the immediate technical request. Don't block execution with questions.\n" +
                "- BIG PICTURE: For architectural tasks, you MAY add a final 'Architect: Review & Expand' task to solicit feedback on purpose/usage AFTER the initial work is planned.\n" +
                "- ITERATION: If a task is complex, plan it in small, verifiable steps. Use 'loopToTaskId' for iterative improvement.\n" +
                "- Trust the refined prompt and shared memory for technical details (language, paths).\n" +
                "- Task names MUST start with agent types: 'File: [Action]', 'JavaDev: [Action]', 'Tester: [Action]', 'Architect: [Action]', etc.\n" +
                "- SIMPLE FILES: For simple file creation or modification (non-Java logic), use the 'File' agent and 'file' taskType. Do NOT use 'llm' taskType for simple writes.\n" +
                "- Task types: 'llm', 'file' (Task name: 'File: Write <path>'), 'shell', 'git', 'maven', 'approval'.\n\n" +
                "JSON Schema:\n" +
                "[ { \"id\": \"t1\", \"name\": \"Agent: Action\", \"description\": \"...\", \"taskType\": \"...\", \"approvalRequired\": boolean, \"loopToTaskId\": \"none\" } ]";
    }

    @Override
    public List<Task> plan(String request, TaskContext context) throws Exception {
        context.log("Planner: Decomposing request - " + request);

        // Fast-track for very simple file creation (no LLM planning needed)
        String cleanRequest = request.toLowerCase().trim();
        if (cleanRequest.matches("^(create|add|write)\\s+file\\s+[^\\s]+$")) {
            String path = cleanRequest.replaceFirst("^(create|add|write)\\s+file\\s+", "");
            context.log("Planner: Fast-track detected for simple file creation: " + path);
            List<Task> fastTasks = new ArrayList<>();
            Task t = OrchestrationFactory.eINSTANCE.createTask();
            t.setId("task1");
            t.setName("File: Write " + path);
            t.setDescription("Create an empty file named '" + path + "'.");
            t.setType("file");
            t.setApprovalRequired(false);
            fastTasks.add(t);
            return fastTasks;
        }

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

        // Use robust flexible extraction to handle cases where models might return a map of tasks
        JSONArray jsonArray = JsonUtils.extractJsonArrayFlexible(response);

        if (jsonArray == null) {
            context.log("Planner: Warning - AI response is not a JSON array or failed parsing. Using fallback llm task.");
            jsonArray = new JSONArray();
            JSONObject fallbackTask = new JSONObject();
            fallbackTask.put("id", "task0");
            // Use a neutral name to avoid incorrect specialized agent triggering via name matching
            fallbackTask.put("name", "General: Process Request");
            fallbackTask.put("description", request);
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
