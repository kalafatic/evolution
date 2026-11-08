package eu.kalafatic.evolution.controller.orchestration.develop.agent;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.develop.DevelopPermissions;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTask;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopTaskManager;

/**
 * AI-assisted task planning and decomposition advisor.
 * Analyzes compound user requests and proposes:
 * - Granular sub-tasks
 * - Task dependencies
 * - Suggested execution order
 * - Parallelization opportunities
 * - Conflict predictions
 *
 * Notice: The AI plan is advisory; the scheduler remains authoritative.
 */
public class DevelopTaskPlanner {

    public static class PlannedSubTask {
        public String title;
        public String description;
        public int priority = 5;
        public List<String> dependsOnTitles = new ArrayList<>();
        public boolean parallelEligible = true;
        public String predictedConflictArea = "";

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("title", title);
            obj.put("description", description);
            obj.put("priority", priority);
            obj.put("dependsOnTitles", new JSONArray(dependsOnTitles));
            obj.put("parallelEligible", parallelEligible);
            obj.put("predictedConflictArea", predictedConflictArea);
            return obj;
        }
    }

    public static class ExecutionPlanProposal {
        public String summary;
        public List<PlannedSubTask> subTasks = new ArrayList<>();
        public String parallelizationAdvice;

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            obj.put("summary", summary);
            JSONArray arr = new JSONArray();
            for (PlannedSubTask st : subTasks) {
                arr.put(st.toJson());
            }
            obj.put("subTasks", arr);
            obj.put("parallelizationAdvice", parallelizationAdvice);
            return obj;
        }
    }

    public ExecutionPlanProposal planCompoundRequest(String compoundInstruction, String repository, String branch) {
        ExecutionPlanProposal proposal = new ExecutionPlanProposal();
        proposal.summary = "AI-assisted decomposition proposal for instruction: " + compoundInstruction;

        if (compoundInstruction == null || compoundInstruction.trim().isEmpty()) {
            return proposal;
        }

        String lower = compoundInstruction.toLowerCase();
        List<PlannedSubTask> tasks = new ArrayList<>();

        if (lower.contains("analyze") || lower.contains("architecture")) {
            PlannedSubTask t1 = new PlannedSubTask();
            t1.title = "Analyze Architecture";
            t1.description = "Perform static analysis, component discovery, and architecture inspection.";
            t1.priority = 8;
            t1.parallelEligible = true;
            tasks.add(t1);
        }

        if (lower.contains("auth") || lower.contains("fix") || lower.contains("feature") || lower.contains("bug")) {
            PlannedSubTask t2 = new PlannedSubTask();
            t2.title = "Implement Changes";
            t2.description = "Apply required code changes according to user specifications.";
            t2.priority = 7;
            t2.parallelEligible = false;
            t2.predictedConflictArea = "Source codebase / services";
            if (!tasks.isEmpty()) {
                t2.dependsOnTitles.add(tasks.get(0).title);
            }
            tasks.add(t2);
        }

        if (lower.contains("test") || lower.contains("verify")) {
            PlannedSubTask t3 = new PlannedSubTask();
            t3.title = "Add & Run Tests";
            t3.description = "Synthesize automated unit / integration tests and verify test suite pass rates.";
            t3.priority = 6;
            t3.parallelEligible = false;
            if (tasks.size() >= 2) {
                t3.dependsOnTitles.add(tasks.get(1).title);
            }
            tasks.add(t3);
        }

        if (lower.contains("doc") || lower.contains("update documentation")) {
            PlannedSubTask t4 = new PlannedSubTask();
            t4.title = "Update Documentation";
            t4.description = "Update project markdown docs, release notes, and architecture diagrams.";
            t4.priority = 4;
            t4.parallelEligible = true;
            tasks.add(t4);
        }

        if (tasks.isEmpty()) {
            PlannedSubTask single = new PlannedSubTask();
            single.title = compoundInstruction;
            single.description = compoundInstruction;
            single.priority = 5;
            tasks.add(single);
        }

        proposal.subTasks = tasks;
        proposal.parallelizationAdvice = "Read-only analysis and documentation tasks can run in parallel; implementation and test tasks should execute sequentially to avoid write conflicts.";
        return proposal;
    }

    public List<DevelopTask> materializePlan(ExecutionPlanProposal proposal, String repository, String branch, String userContext) {
        List<DevelopTask> createdTasks = new ArrayList<>();
        java.util.Map<String, String> titleToIdMap = new java.util.HashMap<>();

        DevelopTaskManager tm = DevelopTaskManager.getInstance();

        for (PlannedSubTask pst : proposal.subTasks) {
            DevelopPermissions perms = new DevelopPermissions(true, true, true, false);
            DevelopTask task = tm.createTask(pst.title, pst.description, repository, branch, perms);
            task.setPriority(pst.priority);
            task.setUserContext(userContext);

            List<String> depIds = new ArrayList<>();
            for (String depTitle : pst.dependsOnTitles) {
                String depId = titleToIdMap.get(depTitle);
                if (depId != null) {
                    depIds.add(depId);
                }
            }
            task.setDependsOnTaskIds(depIds);

            titleToIdMap.put(pst.title, task.getId());
            createdTasks.add(task);

            try {
                eu.kalafatic.evolution.controller.orchestration.develop.DevelopSession session =
                        eu.kalafatic.evolution.controller.orchestration.develop.DevelopSessionManager.getInstance().createSession(task);
                tm.registerSessionForTask(task.getId(), session);
            } catch (Exception ex) {
                eu.kalafatic.evolution.controller.log.Log.log("[PLANNER] Failed to initialize session for task " + task.getId() + ": " + ex.getMessage());
            }

            tm.submitTaskForExecution(task.getId());
        }

        return createdTasks;
    }
}
