package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class GitCheckTask extends AbstractSelfDevTask {
    private final SourceProvider sourceProvider;

    public GitCheckTask(String id) {
        super(id, "Git Repository Verification (" + id + ")");
        this.sourceProvider = new GitSourceProvider();
    }

    private File targetRepo;

    @Override
    protected void resolveResources(SelfDevContext context) throws Exception {
        ResolvedSelfDevResources res = context != null ? context.getResolvedResources() : null;
        if ("GIT_SUPERVISOR".equalsIgnoreCase(id)) {
            File supervisorSource = (res != null && res.getSupervisorDirectory() != null) ? res.getSupervisorDirectory() : (context != null ? context.getSupervisorDirectory() : null);
            this.targetRepo = (supervisorSource != null && supervisorSource.exists()) ? supervisorSource : (context != null ? context.getProjectRoot() : null);
        } else {
            this.targetRepo = (res != null && res.getRepositoryRoot() != null) ? res.getRepositoryRoot() : (context != null ? context.getProjectRoot() : null);
        }
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (targetRepo == null || !targetRepo.exists()) {
            return TaskResult.failure(id, "Git check pre-validation failed: target repository directory does not exist at " + (targetRepo != null ? targetRepo.getAbsolutePath() : "null"), null);
        }
        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Git target directory exists: " + targetRepo.getAbsolutePath())
                .workingDirectory(targetRepo)
                .build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        TaskResult valRes = sourceProvider.validateRepository(targetRepo);
        if (valRes.isSuccess()) {
            String rev = sourceProvider.getSourceRevision(targetRepo);
            String branch = sourceProvider.getBranch(targetRepo);
            context.setSourceRevision(rev);
            return new TaskResult.Builder(id)
                    .status(TaskStatus.SUCCESS)
                    .message("Git check OK. Branch: " + branch + ", Revision: " + rev)
                    .workingDirectory(targetRepo)
                    .diagnostic("revision", rev)
                    .diagnostic("branch", branch)
                    .build();
        } else {
            return valRes;
        }
    }
}
