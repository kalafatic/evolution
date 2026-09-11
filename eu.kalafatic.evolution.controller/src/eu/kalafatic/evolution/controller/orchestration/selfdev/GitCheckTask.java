package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public class GitCheckTask extends AbstractSelfDevTask {
    private final SourceProvider sourceProvider;

    public GitCheckTask(String id) {
        super(id, "Git Repository Verification (" + id + ")");
        this.sourceProvider = new GitSourceProvider();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        File repoRoot = context.getProjectRoot();
        if ("GIT_SUPERVISOR".equalsIgnoreCase(id)) {
            File supervisorSource = context.getSourceDirectory();
            if (supervisorSource != null && supervisorSource.exists() && sourceProvider.validateRepository(supervisorSource).isSuccess()) {
                repoRoot = supervisorSource;
            }
        }

        TaskResult valRes = sourceProvider.validateRepository(repoRoot);
        if (valRes.isSuccess()) {
            String rev = sourceProvider.getSourceRevision(repoRoot);
            String branch = sourceProvider.getBranch(repoRoot);
            context.setSourceRevision(rev);
            return new TaskResult.Builder(id)
                    .status(TaskStatus.SUCCESS)
                    .message("Git check OK. Branch: " + branch + ", Revision: " + rev)
                    .diagnostic("revision", rev)
                    .diagnostic("branch", branch)
                    .build();
        } else {
            return valRes;
        }
    }
}
