package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator;
import eu.kalafatic.evolution.controller.mediation.analysis.MediatedDarwinEngine;
import eu.kalafatic.evolution.controller.mediation.analysis.PromptSynthesizer;
import eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor;
import eu.kalafatic.evolution.controller.mediation.analysis.StagingValidator;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.controller.workflow.MediatedExportManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;

/**
 * Reformulated Mediated Analysis Flow.
 * Coordinates: Project Mapping -> Context Selection -> Prompt Synthesis -> Export Packaging.
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
        try {
            return executeInternal(request, context);
        } catch (Exception e) {
            manager.getGitManager().rollback();
            manager.transition(SystemState.FAILED, context);
            throw e;
        }
    }

    private OrchestratorResponse executeInternal(String request, TaskContext context) throws Exception {
        context.log("[MEDIATED] Starting Reformulated Mediated Context Export Flow.");
        manager.transition(SystemState.ANALYZING, context);

        File root = context.getProjectRoot();
        TargetSnapshot.TargetType type = root.getAbsolutePath().contains("evolution") ? TargetSnapshot.TargetType.SELF : TargetSnapshot.TargetType.PROJECT;

        final TargetSnapshot[] snapshot_ref = new TargetSnapshot[1];

        // Pass 1: Project Map Generation (Scanner)
        runPass(context, "Project Map Generation", "Building lightweight semantic map...", () -> {
            TargetScanner scanner = new TargetScanner();
            snapshot_ref[0] = scanner.scanToSnapshot(root, type);

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-map-gen-" + System.currentTimeMillis(),
                "PROJECT_MAP_GENERATION",
                "TargetScanner",
                List.of(root.getAbsolutePath()),
                List.of("nodes=" + snapshot_ref[0].getNodes().size()),
                1.0,
                "Built initial project map snapshot."
            ));
        });

        TargetSnapshot snapshot = snapshot_ref[0];

        // Pass 2: Semantic Indexing (Extractor)
        runPass(context, "Semantic Indexing", "Extracting structures and relationships...", () -> {
            // Metadata Synchronization
            MetadataAgent generator = new MetadataAgent();
            generator.generate(root);

            SemanticExtractor extractor = new SemanticExtractor();
            extractor.extractToSnapshot(snapshot);

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-indexing-" + System.currentTimeMillis(),
                "SEMANTIC_INDEXING",
                "SemanticExtractor",
                (List<String>)snapshot.getMetadata().get("detectedTechnologies"),
                List.of("architecture=" + snapshot.getMetadata().get("architectureInference")),
                1.0,
                "Populated snapshot with semantic metadata and graph edges."
            ));
        });

        // Pass 3: Mediated Darwin Reasoning (Metadata-only)
        final List<MediatedDarwinEngine.Hypothesis> hypotheses = new ArrayList<>();
        runPass(context, "Mediated Darwin Engine", "Running evolutionary reasoning on snapshot...", () -> {
            MediatedDarwinEngine engine = new MediatedDarwinEngine();
            hypotheses.addAll(engine.runDarwinLoop(snapshot, request));

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-darwin-" + System.currentTimeMillis(),
                "DARWIN_REASONING",
                "MediatedDarwinEngine",
                List.of(request),
                hypotheses.stream().map(h -> h.description).collect(Collectors.toList()),
                1.0,
                "Generated hypotheses based on structural analysis."
            ));

            try {
                manager.checkStep("mediated-darwin", "DARWIN_REASONING", "Review generated hypotheses.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Pass 4: Staging & Safety (Validator)
        final StagingValidator.ValidationResult[] vResult = new StagingValidator.ValidationResult[1];
        runPass(context, "Staging & Safety", "Assessing risks and safety boundaries...", () -> {
            StagingValidator validator = new StagingValidator();
            vResult[0] = validator.validate(snapshot, hypotheses);

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-safety-" + System.currentTimeMillis(),
                "SAFETY_VALIDATION",
                "StagingValidator",
                hypotheses.stream().map(h -> h.description).collect(Collectors.toList()),
                List.of("risk=" + vResult[0].riskLevel),
                1.0,
                "Assessed safety risk: " + vResult[0].riskLevel
            ));

            try {
                manager.checkStep("mediated-safety", "SAFETY_VALIDATION", "Review safety assessment.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Pass 5: Context Selection & Prompt Synthesis
        final List<String> selectedPaths = new ArrayList<>();
        final String[] optimizedPrompt = new String[1];
        runPass(context, "Context Builder", "Selecting optimal context and synthesizing prompt...", () -> {
            ContextCurator curator = new ContextCurator();
            selectedPaths.addAll(curator.selectContext(snapshot, request, 16));

            PromptSynthesizer synthesizer = new PromptSynthesizer();
            optimizedPrompt[0] = synthesizer.synthesizeOptimized(request, snapshot, selectedPaths);

            // Darwinian Refinement Loop
            try {
                for (int i = 0; i < 2; i++) {
                    context.log("[MEDIATED] Darwinian Refinement Iteration " + (i + 1));
                    JSONObject critique = manager.getAnalyticAgent().analyze("CRITIQUE THIS PROMPT BASED ON CONTEXT: " + optimizedPrompt[0], context);
                    if (critique.optDouble("confidence", 0.0) > 0.9) break;

                    optimizedPrompt[0] = manager.getAiService().sendRequest(context.getOrchestrator(),
                        "Improve this prompt based on the following critique: " + critique.optString("refinedPrompt") + "\n\nOriginal Prompt:\n" + optimizedPrompt[0], context);
                }
            } catch (Exception e) {
                context.log("[MEDIATED] Refinement skipped due to error: " + e.getMessage());
            }

            context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
                "mediated-selection-" + System.currentTimeMillis(),
                "CONTEXT_SELECTION",
                "ContextCurator",
                List.of(request),
                selectedPaths,
                1.0,
                "Selected " + selectedPaths.size() + " files for export."
            ));

            try {
                manager.checkStep("mediated-context", "CONTEXT_BUILDER", "Review selected context and optimized prompt.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Pass 6: Export Packaging (Final Step)
        final File[] exportPackage = new File[1];
        runPass(context, "Export Packaging", "Creating ZIP bundle for external LLM...", () -> {
            // Final explicit approval for packaging in mediated mode
            if (context.getBehaviorProfile().hasTrait(BehaviorTrait.SUPERVISION_MEDIATED)) {
                try {
                    boolean approved = context.requestApproval("Final review: Ready to generate export package with " + selectedPaths.size() + " files?").get();
                    if (!approved) throw new Exception("Export cancelled by user.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                String sessionId = context.getSessionId();
                String outputPath = null;
                if (context.getOrchestrator().getAiChat() != null) {
                    ChatSession session = context.getOrchestrator().getAiChat().getSessions().stream()
                            .filter(s -> s != null && s.getId() != null && s.getId().equals(sessionId))
                            .findFirst().orElse(null);
                    outputPath = session != null ? session.getOutputPath() : null;
                }

                MediatedExportManager exportManager = new MediatedExportManager();
                exportPackage[0] = exportManager.createExportPackage(context.getSessionId(), optimizedPrompt[0], selectedPaths, root, outputPath);
                context.log("[MEDIATED] Export bundle ready: " + (exportPackage[0] != null ? exportPackage[0].getName() : "FAILED"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create export package", e);
            }
        });

        // Pass 7: Final Synthesis
        StringBuilder summaryBuilder = new StringBuilder();
        runPass(context, "Final Synthesis", "Preparing final results summary...", () -> {
            summaryBuilder.append("### Mediated Context Export Complete\n\n");
            summaryBuilder.append("**Target Type:** ").append(snapshot.getTargetType()).append("\n");
            summaryBuilder.append("**Inferred Architecture:** ").append(snapshot.getMetadata().get("architectureInference")).append("\n\n");
            if (exportPackage[0] != null) {
                summaryBuilder.append("**Export Package:** `").append(exportPackage[0].getName()).append("`\n");
            } else {
                summaryBuilder.append("**Export Package:** `FAILED`\n");
            }
            summaryBuilder.append("**Selected Files:** ").append(selectedPaths.size()).append(" (Hard limit: 16)\n\n");

            summaryBuilder.append("**Safety Risk:** ").append(vResult[0].riskLevel).append("\n");
            for (String warning : vResult[0].warnings) {
                summaryBuilder.append("- ⚠️ ").append(warning).append("\n");
            }

            summaryBuilder.append("\n**Proposed Improvements (Darwin Reasoning):**\n");
            for (MediatedDarwinEngine.Hypothesis h : hypotheses) {
                summaryBuilder.append("- ").append(h.description).append(" [").append(h.riskLevel).append("]\n");
            }
        });

        context.getOrchestrationState().getMetadata().put("mediatedSnapshot", snapshot);
        context.getOrchestrationState().getMetadata().put("mediatedHypotheses", hypotheses);
        context.getOrchestrationState().getMetadata().put("mediatedValidation", vResult[0]);
        if (exportPackage[0] != null) {
            context.getOrchestrationState().getMetadata().put("mediatedExportFile", exportPackage[0].getAbsolutePath());
        }

        // Update Results View (Event based)
        RuntimeEvent eventBusMsg = new RuntimeEvent(
            RuntimeEventType.EXPORT_READY,
            context.getSessionId(),
            "MediatedFlow",
            summaryBuilder.toString())
            .withMetadata("target", snapshot.getRootPath());
        if (exportPackage[0] != null) {
            eventBusMsg.withMetadata("exportFile", exportPackage[0].getAbsolutePath());
        }
        RuntimeEventBus.getInstance().publish(eventBusMsg);

        OrchestratorResponse response = new OrchestratorResponse();
        response.setResultType(ResultType.CHAT);
        response.setSummary(summaryBuilder.toString());

        manager.transition(SystemState.DONE, context);
        return response;
    }

    private void runPass(TaskContext context, String name, String desc, Runnable action) {
        Task task = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createTask();
        task.setId("mediated-pass-" + System.currentTimeMillis() + "-" + name.hashCode());
        task.setName(name);
        task.setDescription(desc);
        task.setStatus(TaskStatus.RUNNING);
        context.getOrchestrator().getTasks().add(task);
        context.log("[PASS_START] " + name);

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(RuntimeEventType.TASK_STARTED, context.getSessionId(), "MediatedFlow", task.getId()));

        try {
            action.run();
            task.setStatus(TaskStatus.DONE);
            context.log("[PASS_DONE] " + name);
            RuntimeEventBus.getInstance().publish(new RuntimeEvent(RuntimeEventType.TASK_COMPLETED, context.getSessionId(), "MediatedFlow", task.getId()));
        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED);
            context.log("[ERROR] Pass " + name + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
