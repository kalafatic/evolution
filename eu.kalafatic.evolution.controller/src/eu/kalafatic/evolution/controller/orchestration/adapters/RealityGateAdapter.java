package eu.kalafatic.evolution.controller.orchestration.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.controller.agents.RealityDiscoveryAgent;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.dto.RealityResult;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceDeltaAnalyzer;

public class RealityGateAdapter {

    public void refineTargetReality(String goal, TaskContext context, RealityDiscoveryAgent realityDiscoveryAgent) throws Exception {
        context.log("[KERNEL] Recursive Discovery: Refining Target Reality Model based on new iteration evidence.");
        TargetSnapshot snapshot = (TargetSnapshot) context.getOrchestrationState().getMetadata().get("mediatedSnapshot");
        TargetRealityModel existingModel = (TargetRealityModel) context.getOrchestrationState().getMetadata().get("targetRealityModel");

        if (snapshot == null || existingModel == null) return;

        // Recursive Reconstruction Loop: iterate discovery until completeness threshold or convergence
        double lastCompleteness = existingModel.getRealityCompleteness();
        int pass = 1;
        while (pass <= 3 && existingModel.getRealityCompleteness() < 0.85) {
            context.log("[KERNEL] Discovery Loop Pass " + pass + " (Completeness: " + existingModel.getRealityCompleteness() + ")");

            // Targeted Discovery driven by Knowledge Gaps and coverage scores
            existingModel = realityDiscoveryAgent.discover(goal, context, snapshot.getRootPath(), existingModel);
            context.getOrchestrationState().getMetadata().put("targetRealityModel", existingModel);

            if (Math.abs(existingModel.getRealityCompleteness() - lastCompleteness) < 0.05) {
                context.log("[KERNEL] Discovery converged.");
                break;
            }
            lastCompleteness = existingModel.getRealityCompleteness();
            pass++;
        }
    }

    public RealityResult check(RealityLevel level, File projectRoot, TaskContext context) {
        WorkspaceDeltaAnalyzer analyzer = new WorkspaceDeltaAnalyzer(projectRoot, context);
        return new RealityResult(true, 1.0, new ArrayList<>(), new ArrayList<>(), level);
    }
}
