package eu.kalafatic.evolution.controller.orchestration;

import eu.kalafatic.evolution.controller.supervision.AuthorityController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import java.io.File;

/**
 * Shared container for kernel-scoped services.
 * Ensures that all components in a single evolution cycle share the same
 * authority, memory, and audit trail instances.
 */
public class EvolutionKernelContext {
    private final AuthorityController authority;
    private final IterationMemoryService memoryService;
    private final File projectRoot;

    public EvolutionKernelContext(File projectRoot) {
        this.projectRoot = projectRoot;
        this.memoryService = new IterationMemoryService(projectRoot);
        this.authority = new AuthorityController();
    }

    public AuthorityController getAuthority() {
        return authority;
    }

    public IterationMemoryService getMemoryService() {
        return memoryService;
    }

    public File getProjectRoot() {
        return projectRoot;
    }
}
