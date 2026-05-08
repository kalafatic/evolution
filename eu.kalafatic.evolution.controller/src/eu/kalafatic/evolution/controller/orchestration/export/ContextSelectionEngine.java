package eu.kalafatic.evolution.controller.orchestration.export;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.ContextBuilder;
import eu.kalafatic.evolution.controller.orchestration.ContextPackage;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.controller.tools.FileTool;

/**
 * Selects relevant source files and builds high-density context.
 */
public class ContextSelectionEngine {
    private final FileTool fileTool = new FileTool();

    public Map<String, String> selectContext(String goal, JSONObject analysis, TaskContext context) throws Exception {
        context.log("[EXPORT] Selecting relevant context for goal: " + goal);

        Task tempTask = OrchestrationFactory.eINSTANCE.createTask();
        tempTask.setGoal(goal);
        tempTask.setName("Export Context Gathering");
        tempTask.setDescription(analysis.optString("refinedPrompt", goal));

        ContextPackage pkg = ContextBuilder.build(tempTask, context);
        Set<String> paths = new HashSet<>(pkg.getScope());

        Map<String, String> fileContents = new HashMap<>();
        for (String path : paths) {
            try {
                String content = fileTool.execute("READ " + path, context.getProjectRoot(), context);
                fileContents.put(path, content);
            } catch (Exception e) {
                context.log("[EXPORT] Skipping file " + path + " due to error: " + e.getMessage());
            }
        }

        return fileContents;
    }
}
