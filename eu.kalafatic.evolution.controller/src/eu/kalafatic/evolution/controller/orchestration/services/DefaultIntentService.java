package eu.kalafatic.evolution.controller.orchestration.services;

import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.OrchestrationState;
import eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorTrait;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator;
import eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionEngine;
import java.util.List;

public class DefaultIntentService implements IntentService {
    @Override
    public void expandIntent(TaskContext context, IterationManager manager) throws Exception {
        String request = context.getOrchestrationState().getRawInput();
        OrchestrationState state = context.getOrchestrationState();
        eu.kalafatic.evolution.controller.orchestration.behavior.BehaviorProfile profile = context.getBehaviorProfile();

        // ADAPTIVE KERNEL: Ensure execution profile is initialized before access
        if (context.getExecutionProfile() == null) {
            eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile_init =
                eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator.calculate(context, manager.getActiveTrajectory(context), null);
            context.getOrchestrationState().setExecutionProfile(profile_init);
        }

        // 1. DISCOVERY phase logic migrated from DarwinEngine
        if (!profile.hasTrait(BehaviorTrait.REASONING_ATOMIC)) {
            if (manager.getGitManager().isGitRepository()) {
                manager.transition(SystemState.ANALYZING, context);
                context.log("[DARWIN] Discovery: Inspecting repository structure.");
                String projectStructure = manager.getStructureAgent().process("Provide a concise summary of the project structure and technology stack.", context, null);
                state.getMetadata().put("projectStructure", projectStructure);

                WorkspaceArtifact archArtifact = new WorkspaceArtifact("arch-summary-" + System.currentTimeMillis(), "architecture-summary");
                archArtifact.setContent(projectStructure);
                archArtifact.getSemanticTags().add("architecture");
                archArtifact.getSemanticTags().add("structure");
                context.getSemanticWorkspace().addArtifact(archArtifact);

                // Formal Reality Discovery
                context.log("[DARWIN] Discovery: Building semantic repository snapshot.");
                TargetScanner scanner = new TargetScanner();
                TargetSnapshot.TargetType type = context.getProjectRoot().getAbsolutePath().contains("evolution") ? TargetSnapshot.TargetType.SELF : TargetSnapshot.TargetType.PROJECT;
                TargetSnapshot snapshot = scanner.scanToSnapshot(context.getProjectRoot(), type);

                // TWO-STAGE SELECTION: Heuristic pick 32 candidates for deep analysis
                ContextCurator curator = new ContextCurator();
                List<String> candidates = curator.selectContext(snapshot, request, 32);

                context.log("[DARWIN] Discovery: Selective deep analysis of " + candidates.size() + " high-signal candidates.");
                SemanticExtractor extractor = new SemanticExtractor();
                extractor.extractToSnapshot(snapshot, candidates);

                state.getMetadata().put("mediatedSnapshot", snapshot);

                // Construct formal TargetRealityModel
                context.log("[DARWIN] Discovery: Formalizing Target Reality Model.");
                eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel realityModel = manager.getRealityDiscoveryAgent().discover(request, context, context.getProjectRoot().getAbsolutePath());
                state.getMetadata().put("targetRealityModel", realityModel);

                if (profile.hasTrait(BehaviorTrait.WORKFLOW_EXPORT_ONLY)) {
                    context.log("[DARWIN] Mediated Mode: Triggering MetadataAgent repository cognition.");
                    eu.kalafatic.evolution.controller.agents.MetadataAgent metadataAgent = new eu.kalafatic.evolution.controller.agents.MetadataAgent();
                    metadataAgent.generate(context.getProjectRoot());
                }

                context.getOrchestrationState().addDiagnostic("[DarwinTrace] Discovery complete. Target Reality Model initialized.");
            }
        }

        manager.transition(SystemState.ANALYZING, context);

        // Original IntentService logic
        IntentExpansionEngine intentEngine = manager.getIntentExpansionEngine();
        intentEngine.expand(request, context);
    }
}
