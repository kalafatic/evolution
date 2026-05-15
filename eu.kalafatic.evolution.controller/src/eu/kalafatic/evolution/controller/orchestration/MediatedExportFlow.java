package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.util.Map;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorResolver;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.export.*;
import eu.kalafatic.evolution.controller.workflow.*;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;

public class MediatedExportFlow implements IOrchestrationFlow {
    private final AiService aiService;
    private final IterationManager manager;

    public MediatedExportFlow(AiService aiService, IterationManager manager) {
        this.aiService = aiService;
        this.manager = manager;
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        context.log("[KERNEL] Executing Mediated Export Flow.");
        context.getOrchestrator().getTasks().clear();

        BehaviorProfile profile = context.getBehaviorProfile();
        OrchestrationState state = context.getOrchestrationState();

        // Mediation suppression for internal preparation steps
        AiMode originalMode = context.getOrchestrator().getAiMode();
        boolean mediated = profile.hasTrait(BehaviorTrait.SUPERVISION_MEDIATED);
        if (mediated) {
            context.log("[KERNEL] Temporarily suppressing MEDIATED mode for internal prep.");
            context.getOrchestrator().setAiMode(AiMode.LOCAL);
        }

        try {
            manager.transition(SystemState.ANALYZING, context);
            return executeInternal(request, context);
        } finally {
            if (mediated) {
                context.getOrchestrator().setAiMode(originalMode);
            }
        }
    }

    private OrchestratorResponse executeInternal(String request, TaskContext context) throws Exception {
        context.log("[EXPORT] Starting repository-aware iterative export preparation.");

        JSONObject analysis = null;
        while (true) {
            // 1. Analysis Task
            Task analysisTask = createVirtualTask("Analysis", "Analyze user request and select files.");
            context.getOrchestrator().getTasks().add(analysisTask);
            analysisTask.setStatus(TaskStatus.RUNNING);
            notifyTask(context, analysisTask, RuntimeEventType.TASK_STARTED);

            SelfDevRequestAnalyzer analyzer = new SelfDevRequestAnalyzer();
            analysis = analyzer.analyze(request, context);
            manager.transition(SystemState.EXPORTING, context);

            if (checkStep(context, "mediated_flow", "ANALYSIS", "Verify export analysis and file selection.") == WorkflowStatus.RETRY) {
                context.getOrchestrator().getTasks().remove(analysisTask);
                continue;
            }
            analysisTask.setStatus(TaskStatus.DONE);
            notifyTask(context, analysisTask, RuntimeEventType.TASK_COMPLETED);
            break;
        }
        String architectureSummary = null;
        Map<String, String> contextFiles = null;
        while (true) {
            // 2. Context Selection Task
            Task contextTask = createVirtualTask("Context", "Summarize architecture and select context files.");
            context.getOrchestrator().getTasks().add(contextTask);
            contextTask.setStatus(TaskStatus.RUNNING);
            notifyTask(context, contextTask, RuntimeEventType.TASK_STARTED);

            ArchitectureSummarizer summarizer = new ArchitectureSummarizer();
            architectureSummary = summarizer.summarize(context, aiService);

            context.log("[EXPORT] Selecting high-density repository context.");
            ContextSelectionEngine contextEngine = new ContextSelectionEngine();
            contextFiles = contextEngine.selectContext(request, analysis, context);

            if (checkStep(context, "mediated_flow", "CONTEXT_SELECTION", "Review selected context files.") == WorkflowStatus.RETRY) {
                context.getOrchestrator().getTasks().remove(contextTask);
                continue;
            }
            contextTask.setStatus(TaskStatus.DONE);
            notifyTask(context, contextTask, RuntimeEventType.TASK_COMPLETED);
            break;
        }

        String optimizedPrompt = null;
        while (true) {
            // 3. Prompt Optimization Task
            Task optTask = createVirtualTask("Optimization", "Optimize the final prompt for LLM consumption.");
            context.getOrchestrator().getTasks().add(optTask);
            optTask.setStatus(TaskStatus.RUNNING);
            notifyTask(context, optTask, RuntimeEventType.TASK_STARTED);

            PromptOptimizer optimizer = new PromptOptimizer();
            optimizedPrompt = optimizer.optimize(request, architectureSummary, context, aiService);

            if (checkStep(context, "mediated_flow", "PROMPT_GENERATION", "Review optimized prompt before export.") == WorkflowStatus.RETRY) {
                context.getOrchestrator().getTasks().remove(optTask);
                continue;
            }
            optTask.setStatus(TaskStatus.DONE);
            notifyTask(context, optTask, RuntimeEventType.TASK_COMPLETED);
            break;
        }

        File zipFile = null;
        while (true) {
            // 4. Export Task
            Task exportTask = createVirtualTask("Export", "Build and save the ZIP export package.");
            context.getOrchestrator().getTasks().add(exportTask);
            exportTask.setStatus(TaskStatus.RUNNING);
            notifyTask(context, exportTask, RuntimeEventType.TASK_STARTED);

            ExportPackageBuilder builder = new ExportPackageBuilder();
            zipFile = builder.build(request, analysis, optimizedPrompt, architectureSummary, contextFiles, context);

            if (checkStep(context, "zip_export", "EXPORT_READY", "Export package generated at: " + zipFile.getName()) == WorkflowStatus.RETRY) {
                context.getOrchestrator().getTasks().remove(exportTask);
                continue;
            }
            exportTask.setStatus(TaskStatus.DONE);
            notifyTask(context, exportTask, RuntimeEventType.TASK_COMPLETED);
            break;
        }

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);
        String summary = "### Export Complete\n\nLocation: `" + zipFile.getAbsolutePath() + "`";
        response.setSummary(summary);
        response.setContent(summary);
        manager.transition(SystemState.DONE, context);
        context.log("[EXPORT] Mediated export package ready for repository: " + context.getProjectRoot().getName());
        return response;
    }

    private Task createVirtualTask(String name, String description) {
        Task t = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createTask();
        t.setId("mediated-" + name.toLowerCase() + "-" + System.currentTimeMillis());
        t.setName(name);
        t.setDescription(description);
        t.setType("mediated");
        return t;
    }

    private void notifyTask(TaskContext context, Task task, RuntimeEventType type) {
        RuntimeEventBus.getInstance().publish(
            new RuntimeEvent(type, context.getSessionId(), "MediatedExportFlow", task.getId())
        );
    }

    private WorkflowStatus checkStep(TaskContext context, String entityId, String type, String description) throws Exception {
        if (context.getOrchestrator().getAiChat() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions() != null &&
            context.getOrchestrator().getAiChat().getPromptInstructions().isStepMode()) {

            WorkflowStep step = new WorkflowStep("step-" + System.currentTimeMillis(), entityId, type);
            step.setDescription(description);
            WorkflowStatus result = StepModeController.getInstance().waitForStep(context.getSessionId(), step, context);
            if (result == WorkflowStatus.FAILED) {
                throw new Exception("Step failed or rejected by user: " + description);
            }
            return result;
        }
        return WorkflowStatus.COMPLETED;
    }
}
