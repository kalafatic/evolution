package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

public class TaskPlanner extends BaseAiAgent {

    public TaskPlanner() {
        super("TaskPlanner", "SelfDevPlanner");
    }

    public static class PlanningResult {
        public final List<Task> tasks;
        public final String rationale;

        public PlanningResult(List<Task> tasks, String rationale) {
            this.tasks = tasks;
            this.rationale = rationale;
        }
    }

    public PlanningResult generateTasks(TaskContext context) throws Exception {
        context.log("[PLANNER] Analyzing project to generate improvement tasks...");

        String prompt = "You are a Self-Development Task Planner. Your goal is to improve the codebase.\n" +
                "Analyze the project structure and provided context. Generate 1 to 5 atomic, independent improvement tasks.\n" +
                "Tasks should focus on code quality, documentation, test coverage, or minor feature enhancements.\n" +
                "Forbidden: Changing build config (pom.xml), core orchestrator engine, or deployment scripts.\n\n" +
                "Provide a short rationale (max 3 sentences) for the chosen improvements.\n\n" +
                "Output MUST be a valid JSON object. Schema:\n" +
                "{\n" +
                "  \"rationale\": \"Explanation of why these tasks were chosen.\",\n" +
                "  \"tasks\": [ { \"id\": \"unique_id\", \"name\": \"Clear task description\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\", \"priority\": integer } ]\n" +
                "}\n";

        String response = aiService.sendRequest(context.getOrchestrator(), prompt, context);
        context.log("[PLANNER] AI response received.");

        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");

        if (start == -1 || end == -1) {
            context.log("[PLANNER] Error: AI response is not a valid JSON object. Response: " + response);
            return new PlanningResult(new ArrayList<>(), "Error: AI response not in JSON format.");
        }

        JSONObject rootObj = new JSONObject(response.substring(start, end + 1));
        String rationale = rootObj.optString("rationale", "No rationale provided.");
        JSONArray jsonArray = rootObj.optJSONArray("tasks");

        List<Task> tasks = new ArrayList<>();
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Task task = factory.createTask();
                task.setId(obj.optString("id", "sd-task-" + System.currentTimeMillis() + "-" + i));
                task.setName(obj.optString("name", "Unnamed Improvement Task"));
                task.setType(obj.optString("taskType", "llm"));
                task.setPriority(obj.optInt("priority", 1));
                tasks.add(task);
            }
        }

        context.log("[PLANNER] Generated " + tasks.size() + " tasks with rationale: " + rationale);
        return new PlanningResult(tasks, rationale);
    }
}
