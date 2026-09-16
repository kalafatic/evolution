package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class GitCheckTask extends AbstractSelfDevTask {
    private final GitSourceProvider sourceProvider;
    private final Map<String, File> repositories = new LinkedHashMap<>();

    public GitCheckTask(String id) {
        super(id, "Global Git Repository Verification (" + id + ")");
        this.sourceProvider = new GitSourceProvider();
    }

    @Override
    protected void resolveResources(SelfDevContext context) throws Exception {
        ResourceManager rm = (context != null && context.getResourceManager() != null)
                ? context.getResourceManager()
                : ResourceManager.getInstance();
        repositories.clear();
        File repo = (context != null && context.getRepositoryRoot() != null)
                ? context.getRepositoryRoot().getAbsoluteFile()
                : rm.getPath(EvoPath.EVO_GIT_REPOSITORY).toFile().getAbsoluteFile();
        repositories.put("evolution", repo);
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (repositories.isEmpty()) {
            return TaskResult.failure(id, "Pre-validation failed: No target repositories resolved.", null);
        }
        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Global Git Check initialized for " + repositories.size() + " repositories.")
                .build();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        log("[GIT_CHECK] Starting global repository verification");
        int verifiedCount = 0;
        int totalRepos = repositories.size();

        for (Map.Entry<String, File> entry : repositories.entrySet()) {
            String repoName = entry.getKey();
            File repoPath = entry.getValue();

            log("[GIT_CHECK] Repository: " + repoName);

            boolean exists = repoPath != null && repoPath.exists();
            boolean isGit = exists && new GitManager(repoPath).isGitRepository();

            log("[GIT_CHECK] repo=" + repoName);
            log("[GIT_CHECK] path=" + (repoPath != null ? repoPath.getAbsolutePath() : "null"));
            log("[GIT_CHECK] exists=" + exists);
            log("[GIT_CHECK] gitRepository=" + isGit);

            if (!exists) {
                log("[GIT_CHECK] Global result: FAILURE");
                log("[GIT_CHECK] Verified repositories: " + verifiedCount + "/" + totalRepos);
                log("[GIT_CHECK] Failed repository: " + repoName);
                String reason = "Directory does not exist at " + (repoPath != null ? repoPath.getAbsolutePath() : "null");
                log("[GIT_CHECK] FAILED\nRepository: " + (repoPath != null ? repoPath.getAbsolutePath() : repoName) + "\nReason: " + reason);
                return TaskResult.failure(id, "[GIT_CHECK] FAILED\nRepository: " + (repoPath != null ? repoPath.getAbsolutePath() : repoName) + "\nReason: " + reason, null);
            }

            if (!isGit) {
                log("[GIT_CHECK] Global result: FAILURE");
                log("[GIT_CHECK] Verified repositories: " + verifiedCount + "/" + totalRepos);
                log("[GIT_CHECK] Failed repository: " + repoName);
                String reason = "Directory is not a valid Git repository";
                log("[GIT_CHECK] FAILED\nRepository: " + repoPath.getAbsolutePath() + "\nReason: " + reason);
                return TaskResult.failure(id, "[GIT_CHECK] FAILED\nRepository: " + repoPath.getAbsolutePath() + "\nReason: " + reason, null);
            }

            String branch = sourceProvider.getBranch(repoPath);
            String revisionBefore = sourceProvider.getSourceRevision(repoPath);
            String remote = sourceProvider.getRemote(repoPath);

            log("[GIT_CHECK] branch=" + branch);
            log("[GIT_CHECK] revisionBefore=" + revisionBefore);
            log("[GIT_CHECK] remote=" + remote);

            TaskResult updateRes = sourceProvider.updateRepository(repoPath);
            boolean updateSuccess = updateRes.isSuccess();
            log("[GIT_CHECK] update=" + (updateSuccess ? "success" : "failed"));

            if (!updateSuccess) {
                log("[GIT_CHECK] Global result: FAILURE");
                log("[GIT_CHECK] Verified repositories: " + verifiedCount + "/" + totalRepos);
                log("[GIT_CHECK] Failed repository: " + repoName);
                String reason = "Remote update failed: " + updateRes.getMessage();
                log("[GIT_CHECK] FAILED\nRepository: " + repoPath.getAbsolutePath() + "\nReason: " + reason);
                return TaskResult.failure(id, "[GIT_CHECK] FAILED\nRepository: " + repoPath.getAbsolutePath() + "\nReason: " + reason, null);
            }

            String revisionAfter = sourceProvider.getSourceRevision(repoPath);
            String status = sourceProvider.getStatus(repoPath);

            log("[GIT_CHECK] revisionAfter=" + revisionAfter);
            log("[GIT_CHECK] status=" + status);

            if ("FAILED".equalsIgnoreCase(status)) {
                log("[GIT_CHECK] Global result: FAILURE");
                log("[GIT_CHECK] Verified repositories: " + verifiedCount + "/" + totalRepos);
                log("[GIT_CHECK] Failed repository: " + repoName);
                String reason = "Repository status check failed";
                log("[GIT_CHECK] FAILED\nRepository: " + repoPath.getAbsolutePath() + "\nReason: " + reason);
                return TaskResult.failure(id, "[GIT_CHECK] FAILED\nRepository: " + repoPath.getAbsolutePath() + "\nReason: " + reason, null);
            }

            verifiedCount++;
        }

        log("[GIT_CHECK] Global result: SUCCESS");
        log("[GIT_CHECK] Verified repositories: " + verifiedCount + "/" + totalRepos);

        File evolutionRepo = repositories.get("evolution");
        if (evolutionRepo != null && evolutionRepo.exists()) {
            context.setSourceRevision(sourceProvider.getSourceRevision(evolutionRepo));
        }

        return new TaskResult.Builder(id)
                .status(TaskStatus.SUCCESS)
                .message("[GIT_CHECK] Global result: SUCCESS. Verified repositories: " + verifiedCount + "/" + totalRepos)
                .workingDirectory(repositories.get("evolution"))
                .build();
    }

    private void log(String msg) {
        Log.log(msg);
        System.out.println(msg);
    }
}
