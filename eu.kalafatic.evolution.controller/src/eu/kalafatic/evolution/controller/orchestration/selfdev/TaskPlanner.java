package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;

public class TaskPlanner extends BaseAiAgent {

    public TaskPlanner() {
        super("TaskPlanner", "SelfDevPlanner");
    }

    @Override
    protected String getAgentInstructions() {
        return "You are acting as a Task Planner Agent for self-development workflows.";
    }

    public List<Task> generateTasks(TaskContext context, String strategy) throws Exception {
        String initialRequest = null;
        if (context.getOrchestrator().getSelfDevSession() != null) {
            initialRequest = context.getOrchestrator().getSelfDevSession().getInitialRequest();
        }

        boolean isIterative = context.getOrchestrator().isIterativeMode();
        boolean isSelfIterative = context.getOrchestrator().isSelfIterativeMode();

        String prompt;
        if (isIterative && initialRequest != null && !initialRequest.isEmpty() && !"Analyze the project and suggest improvements.".equals(initialRequest)) {
            context.log("[PLANNER] Analyzing project to fulfill iterative request: " + initialRequest);
            prompt = "You are an Iterative Development Task Planner. Your goal is to fulfill the following user request: \"" + initialRequest + "\"\n" +
                    (strategy != null ? "Strategy to follow: " + strategy + "\n" : "") +
                    "Analyze the project structure and provided context. Generate 1 to 5 atomic, independent tasks to achieve this goal.\n" +
                    "Tasks can include code changes, test creation, or documentation.\n" +
                    "Forbidden: Changing build config (pom.xml) unless explicitly requested, core orchestrator engine, or deployment scripts.\n\n" +
                    "Output MUST be a valid JSON array of objects. Schema:\n" +
                    "[ { \"id\": \"unique_id\", \"name\": \"Clear task description\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\", \"priority\": integer, \"rationale\": \"string\" } ]\n";
        } else {
            context.log("[PLANNER] Analyzing project to generate autonomous improvement tasks...");
            prompt = "You are a Self-Development Task Planner. Your goal is to improve the codebase autonomously.\n" +
                    "Analyze the project structure and provided context. Generate 1 to 5 atomic, independent improvement tasks.\n" +
                    "Tasks should focus on code quality, documentation, test coverage, or minor feature enhancements.\n" +
                    "Forbidden: Changing build config (pom.xml), core orchestrator engine, or deployment scripts.\n\n" +
                    "Output MUST be a valid JSON array of objects. Schema:\n" +
                    "[ { \"id\": \"unique_id\", \"name\": \"Clear task description\", \"taskType\": \"llm\"|\"file\"|\"git\"|\"maven\", \"priority\": integer, \"rationale\": \"string\" } ]\n";

            if (isSelfIterative && initialRequest != null && !initialRequest.isEmpty() && !"Analyze the project and suggest improvements.".equals(initialRequest)) {
                prompt += "\nUser provided additional context/focus for this autonomous session: \"" + initialRequest + "\"";
            }
            if (strategy != null) {
                prompt += "\nStrategy to follow: " + strategy;
            }
        }

        // Use structured prompt building to include memory
        String fullPrompt = buildPrompt("Generate improvement tasks", context, null);
        fullPrompt = fullPrompt.replace("INSTRUCTIONS:\n" + getAgentInstructions(), "INSTRUCTIONS:\n" + prompt);
        fullPrompt = fullPrompt.replace("CURRENT TASK:\nGenerate improvement tasks", "CURRENT TASK:\nAnalyze current state and plan next steps.");

        String response = aiService.sendRequest(context.getOrchestrator(), fullPrompt, context);
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
            task.setRationale(obj.optString("rationale", "No rationale provided by planner."));
            tasks.add(task);
        }

        context.log("[PLANNER] Generated " + tasks.size() + " tasks.");
        return tasks;
    }
}
