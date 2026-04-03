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

    public List<Task> generateTasks(TaskContext context) throws Exception {
        context.log("[PLANNER] Analyzing project to generate improvement tasks...");

        String prompt = "You are a Self-Development Task Planner. Your goal is to improve the codebase.\n" +
                "Analyze the project structure and provided context. Generate 1 to 5 atomic, independent improvement tasks.\n" +
                "Tasks should focus on code quality, documentation, test coverage, or minor feature enhancements.\n" +
                "Forbidden: Changing build config (pom.xml), core orchestrator engine, or deployment scripts.\n\n" +
                "Output MUST be a valid JSON array of objects. Schema:\n" +
                "[ { \"id\": \"unique_id\", \"name\": \"Clear task description\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\", \"priority\": integer } ]\n";

        String response = aiService.sendRequest(context.getOrchestrator(), prompt, context);
        context.log("[PLANNER] AI response received.");

        int start = response.indexOf("[");
        int end = response.lastIndexOf("]");

        if (start == -1 || end == -1) {
            context.log("[PLANNER] Error: AI response is not a valid JSON array. Response: " + response);
            return new ArrayList<>();
        }

        JSONArray jsonArray = new JSONArray(response.substring(start, end + 1));
        List<Task> tasks = new ArrayList<>();
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Task task = factory.createTask();
            task.setId(obj.optString("id", "sd-task-" + System.currentTimeMillis() + "-" + i));
            task.setName(obj.optString("name", "Unnamed Improvement Task"));
            task.setType(obj.optString("taskType", "llm"));
            task.setPriority(obj.optInt("priority", 1));
            tasks.add(task);
        }

        context.log("[PLANNER] Generated " + tasks.size() + " tasks.");
        return tasks;
    }
}
