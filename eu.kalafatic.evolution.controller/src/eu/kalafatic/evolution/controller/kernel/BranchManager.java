package eu.kalafatic.evolution.controller.kernel;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Interface for managing branch provisioning and worktrees.
 */
public interface BranchManager {
    void provisionBranch(String base, String newBranch) throws Exception;
    void createWorktree(String branch, String path) throws Exception;
    void removeWorktree(String path) throws Exception;
    void cleanupWorktrees() throws Exception;
}
