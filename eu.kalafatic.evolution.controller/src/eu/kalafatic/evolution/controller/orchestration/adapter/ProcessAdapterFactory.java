package eu.kalafatic.evolution.controller.orchestration.adapter;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker;
import eu.kalafatic.evolution.controller.orchestration.ContextProcessTracker.ProcessInfo;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ADarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DarwinEngineRegistry;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Factory for resolving the active process adapter corresponding to the current session process type
 * (Darwin Evolution Iterations, Forge LLM Training Epochs, or Self-Dev Workflow Steps).
 */
public class ProcessAdapterFactory {

    private static final ProcessAdapterFactory INSTANCE = new ProcessAdapterFactory();

    public static ProcessAdapterFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Resolves an appropriate process adapter for the given session ID and orchestrator.
     */
    public IProcessAdapter getAdapter(String sessionId, Orchestrator orchestrator, File projectRoot) {
        String sid = (sessionId != null && !sessionId.isEmpty()) ? sessionId : "Default";

        // 1. Check if Forge training is active for this session
        ForgeSession forgeSession = ForgeSessionManager.getInstance().findSession(sid);
        if (forgeSession != null && (forgeSession.getStatus() == ForgeStatus.TRAINING ||
                ForgeSessionManager.getInstance().getTelemetry(sid) != null)) {
            return new ForgeProcessAdapter(sid, orchestrator);
        }

        // 2. Check ContextProcessTracker for explicit Forge process state
        ProcessInfo proc = ContextProcessTracker.getInstance().getProcessState(sid);
        if (proc != null && ("TRAINING".equals(proc.getPhase()) || "Forge LLM Training".equals(proc.getProcessName()))) {
            return new ForgeProcessAdapter(sid, orchestrator);
        }

        // 3. Check if Darwin engine is registered for this session
        ADarwinEngine darwinEngine = DarwinEngineRegistry.get(sid);
        if (darwinEngine != null || (orchestrator != null && orchestrator.isDarwinMode())) {
            return new DarwinProcessAdapter(sid, orchestrator, projectRoot);
        }

        // 4. Default to Darwin process adapter if memory service records exist
        File iterDir = projectRoot != null ? new File(projectRoot, "shared/iterations") : null;
        if (iterDir != null && iterDir.exists() && iterDir.list() != null && iterDir.list().length > 0) {
            return new DarwinProcessAdapter(sid, orchestrator, projectRoot);
        }

        // 5. Fall back to Workflow process adapter
        if (proc != null && ("STEP_EXECUTION".equals(proc.getPhase()) || "Workflow Flow".equals(proc.getProcessName()))) {
            return new WorkflowProcessAdapter(sid, orchestrator);
        }

        // Fallback default: DarwinProcessAdapter
        return new DarwinProcessAdapter(sid, orchestrator, projectRoot);
    }
}
