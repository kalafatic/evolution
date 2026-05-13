package eu.kalafatic.evolution.controller.orchestration.flows;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.mediated.analysis.ContextCurator;
import eu.kalafatic.evolution.controller.orchestration.mediated.analysis.PromptSynthesizer;
import eu.kalafatic.evolution.controller.orchestration.mediated.analysis.SemanticExtractor;
import eu.kalafatic.evolution.controller.orchestration.mediated.model.TargetDescriptor;
import eu.kalafatic.evolution.controller.orchestration.mediated.scanner.TargetScanner;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;

/**
 * Iterative Mediated Analysis Flow.
 * Performs multiple analysis passes on a target to evolve understanding.
 */
public class MediatedAnalysisFlow implements IOrchestrationFlow {
    private final AiService aiService;
    private final IterationManager manager;

    public MediatedAnalysisFlow(AiService aiService, IterationManager manager) {
        this.aiService = aiService;
        this.manager = manager;
    }

    @Override
    public OrchestratorResponse execute(String request, TaskContext context) throws Exception {
        context.log("[MEDIATED] Starting General Mediated Evolutionary Analysis.");
        manager.transition(SystemState.ANALYZING, context);

        File root = context.getProjectRoot();
        TargetDescriptor target = new TargetDescriptor(root.getAbsolutePath());

        // Pass 1: Surface Structure (Scanner)
        runPass(context, "Structure Scanning", "Recursively scanning target structure...", () -> {
            TargetScanner scanner = new TargetScanner();
            TargetDescriptor scanned = scanner.scan(root);
            target.getFiles().addAll(scanned.getFiles());
            target.getDetectedTechnologies().addAll(scanned.getDetectedTechnologies());

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-scan-" + System.currentTimeMillis(),
                "SURFACE_SCAN",
                "TargetScanner",
                List.of(root.getAbsolutePath()),
                List.of("files=" + target.getFiles().size(), "techs=" + String.join(",", target.getDetectedTechnologies())),
                1.0,
                "Completed surface scan of " + target.getFiles().size() + " files."
            ));
        });

        // Pass 2: Semantic Clustering (Extractor)
        runPass(context, "Semantic Extraction", "Extracting semantic markers and patterns...", () -> {
            SemanticExtractor extractor = new SemanticExtractor();
            extractor.extract(target, root);

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-extraction-" + System.currentTimeMillis(),
                "SEMANTIC_EXTRACTION",
                "SemanticExtractor",
                target.getDetectedTechnologies(),
                List.of("architecture=" + target.getArchitectureInference()),
                1.0,
                "Extracted semantic patterns and inferred initial architecture."
            ));
        });

        // Pass 3: Architecture Inference
        runPass(context, "Architecture Inference", "Inferring architectural relationships...", () -> {
            // Inference is already done in SemanticExtractor for Phase 1
            context.log("[MEDIATED] Inferred: " + target.getArchitectureInference());
        });

        // Pass 4: Context Curation
        List<String> curatedFiles = new ArrayList<>();
        runPass(context, "Context Curation", "Selecting high-value context files...", () -> {
            ContextCurator curator = new ContextCurator();
            List<String> curated = curator.curate(target);
            curatedFiles.addAll(curated);
            context.log("[MEDIATED] Curated " + curated.size() + " high-signal files.");

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-curation-" + System.currentTimeMillis(),
                "CONTEXT_CURATION",
                "ContextCurator",
                List.of("target=" + target.getRootPath()),
                curated,
                1.0,
                "Selected " + curated.size() + " files based on architectural relevance."
            ));
        });

        // Pass 5: Prompt Synthesis
        StringBuilder promptBuilder = new StringBuilder();
        runPass(context, "Prompt Synthesis", "Generating architecturally informed prompts...", () -> {
            PromptSynthesizer synthesizer = new PromptSynthesizer();
            String synthesizedPrompt = synthesizer.synthesize(request, target, curatedFiles);
            promptBuilder.append(synthesizedPrompt);

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-synthesis-" + System.currentTimeMillis(),
                "PROMPT_SYNTHESIS",
                "PromptSynthesizer",
                List.of(request),
                List.of("synthesized-prompt"),
                1.0,
                "Generated architecturally aware prompt for external LLM."
            ));
        });

        context.getOrchestrationState().getMetadata().put("mediatedTarget", target);
        context.getOrchestrationState().getMetadata().put("mediatedCuratedFiles", curatedFiles);
        context.getOrchestrationState().getMetadata().put("mediatedSynthesizedPrompt", promptBuilder.toString());

        // Update Results View (Event based)
        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.EXPORT_READY,
            context.getSessionId(),
            "MediatedFlow",
            promptBuilder.toString())
            .withMetadata("target", target.getRootPath()));

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);
        response.setSummary("### Mediated Analysis Complete\n\n" +
                           "**Technologies:** " + String.join(", ", target.getDetectedTechnologies()) + "\n\n" +
                           "**Architecture:** " + target.getArchitectureInference() + "\n\n" +
                           "**Curated Context:** " + curatedFiles.size() + " files selected.");

        manager.transition(SystemState.DONE, context);
        return response;
    }

    private void runPass(TaskContext context, String name, String desc, Runnable action) {
        Task task = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createTask();
        task.setId("mediated-pass-" + System.currentTimeMillis());
        task.setName(name);
        task.setDescription(desc);
        task.setStatus(TaskStatus.RUNNING);
        context.getOrchestrator().getTasks().add(task);

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, context.getSessionId(), "MediatedFlow", task.getId()));

        try {
            action.run();
            task.setStatus(TaskStatus.DONE);
            RuntimeEventBus.getInstance().publish(new RuntimeEvent(RuntimeEventType.TASK_COMPLETED, context.getSessionId(), "MediatedFlow", task.getId()));
        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            context.log("[ERROR] Pass " + name + " failed: " + e.getMessage());
        }
    }
}
