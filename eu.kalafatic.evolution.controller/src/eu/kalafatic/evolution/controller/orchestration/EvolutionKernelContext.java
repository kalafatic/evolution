package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.trajectory.SignalBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;

/**
 * Shared container for kernel-scoped services.
 * Ensures that all components in a single evolution cycle share the same
 * authority, memory, and audit trail instances.
 */
public class EvolutionKernelContext {
    private final AuthorityController authority;
    private IterationMemoryService memoryService;
    private final File projectRoot;
    private final RuntimeEventBus eventBus;
    private final SignalBus signalBus;
    private GitManager gitManager;

    public EvolutionKernelContext(File projectRoot, RuntimeEventBus eventBus, SignalBus signalBus, IterationMemoryService memoryService) {
        this.projectRoot = projectRoot;
        this.eventBus = eventBus;
        this.signalBus = signalBus;
        this.memoryService = memoryService != null ? memoryService : new IterationMemoryService(projectRoot);
        this.authority = new AuthorityController();
    }

    public AuthorityController getAuthority() {
        return authority;
    }

    public RuntimeEventBus getEventBus() {
        return eventBus;
    }

    public SignalBus getSignalBus() {
        return signalBus;
    }

    public IterationMemoryService getMemoryService() {
        return memoryService;
    }

    public void setMemoryService(IterationMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public GitManager getGitManager() {
        return gitManager;
    }

    public void setGitManager(GitManager gitManager) {
        this.gitManager = gitManager;
    }
}
