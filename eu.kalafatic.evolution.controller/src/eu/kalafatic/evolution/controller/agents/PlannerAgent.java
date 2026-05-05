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
import eu.kalafatic.evolution.controller.orchestration.ContextBuilder;

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
                "- Task names SHOULD be descriptive (e.g., 'Update ContextBuilder').\n" +
                "- Task types MUST include the agent role if a specialized agent is needed (e.g., 'JavaDev', 'Tester', 'Architect', 'File', 'Maven', 'Git').\n" +
                "- Generic task types: 'llm', 'file' (Task name: 'Write <path>'), 'shell', 'git', 'maven', 'approval'.\n\n" +
                "JSON Schema:\n" +
                "[ { \"id\": \"t1\", \"name\": \"Action Name\", \"description\": \"...\", \"taskType\": \"JavaDev | llm | file | ...\", \"approvalRequired\": boolean, \"loopToTaskId\": \"none\" } ]";
    }

    @Override
    public List<Task> plan(String request, TaskContext context) throws Exception {
        context.log("Planner: Decomposing request - " + request);

        // Deterministic Prompt Building (using ContextBuilder for consistency)
        String fullPrompt = ContextBuilder.buildStrategicPrompt(type, getAgentInstructions(), getFooterInstructions(), request, context, null);
        context.log("Evo-Planner-Thinking: " + fullPrompt);

        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
        context.log("Evo-Planner-Response: " + response);
        context.log("Planner: Received response from AI: " + (response.length() > 100 ? response.substring(0, 100) + "..." : response));

        // Pure structured extraction
        JSONArray jsonArray = JsonUtils.extractJsonArrayFlexible(response);

        if (jsonArray == null) {
            context.log("Planner: ERROR - AI response is not a JSON array. Returning failure to Kernel.");
            return null; // Let IterationManager decide fallback strategy
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
