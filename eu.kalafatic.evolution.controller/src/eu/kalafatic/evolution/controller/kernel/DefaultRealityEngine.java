package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.File;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceDeltaAnalyzer;

public class DefaultRealityEngine implements RealityEngine {
    private final File projectRoot;
    private final TaskContext context;

    public DefaultRealityEngine(File projectRoot, TaskContext context) {
        this.projectRoot = projectRoot;
        this.context = context;
    }

    @Override
    public WorkspaceDeltaAnalyzer.DeltaAnalysis analyze(String baseCommit) throws Exception {
        WorkspaceDeltaAnalyzer analyzer = new WorkspaceDeltaAnalyzer(projectRoot, context);
        return analyzer.analyze(baseCommit);
    }
}
