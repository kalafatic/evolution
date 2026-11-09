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

        eu.kalafatic.evolution.model.orchestration.Orchestrator orchestrator = context != null ? context.getOrchestrator() : null;

        // 1. Evolution repository (Actual EVO project sources)
        File evolutionRepo = null;
        if (orchestrator != null && orchestrator.getGit() != null && orchestrator.getGit().getLocalPath() != null && !orchestrator.getGit().getLocalPath().trim().isEmpty()) {
            evolutionRepo = new File(orchestrator.getGit().getLocalPath().trim()).getAbsoluteFile();
        } else if (context != null && context.getRepositoryRoot() != null) {
            evolutionRepo = context.getRepositoryRoot().getAbsoluteFile();
        } else {
            evolutionRepo = rm.getPath(EvoPath.EVO_GIT_REPOSITORY).toFile().getAbsoluteFile();
        }
        if (isValidSourceCandidate(evolutionRepo, context)) {
            addRepositoryIfUnique("evolution", evolutionRepo);
        }

        // 2. Supervisor repository (Only if explicitly configured in EMF with distinct source path)
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null && orchestrator.getSupervisorSettings().getGit() != null) {
            String supPath = orchestrator.getSupervisorSettings().getGit().getLocalPath();
            if (supPath != null && !supPath.trim().isEmpty()) {
                File supRepo = new File(supPath.trim()).getAbsoluteFile();
                if (isValidSourceCandidate(supRepo, context)) {
                    addRepositoryIfUnique("supervisor", supRepo);
                }
            }
        }

        // 3. LLM repository (Forge / LLM repository only if explicitly configured in EMF with distinct source path)
        if (orchestrator != null && !orchestrator.getForgeSessions().isEmpty()) {
            for (eu.kalafatic.evolution.model.orchestration.ForgeSession fs : orchestrator.getForgeSessions()) {
                if (fs.getGit() != null && fs.getGit().getLocalPath() != null && !fs.getGit().getLocalPath().trim().isEmpty()) {
                    File llmRepo = new File(fs.getGit().getLocalPath().trim()).getAbsoluteFile();
                    if (isValidSourceCandidate(llmRepo, context)) {
                        addRepositoryIfUnique("llm", llmRepo);
                    }
                    break;
                }
            }
        }
    }

    private boolean isValidSourceCandidate(File candidate, SelfDevContext context) {
        if (candidate == null) {
            return false;
        }
        String pathStr = candidate.getAbsolutePath().replace('\\', '/');
        // Exclude runtime, product, build, and export directories
        if (pathStr.contains(".product") || pathStr.contains("/runtime-") || pathStr.contains("/runtime/")) {
            return false;
        }
        if (context != null) {
            if (context.getRuntimeDirectory() != null && isSameOrSubdirectory(context.getRuntimeDirectory(), candidate)) {
                return false;
            }
            if (context.getBuildDirectory() != null && isSameOrSubdirectory(context.getBuildDirectory(), candidate)) {
                return false;
            }
            if (context.getExportDirectory() != null && isSameOrSubdirectory(context.getExportDirectory(), candidate)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSameOrSubdirectory(File base, File candidate) {
        try {
            String baseCanon = base.getCanonicalPath();
            String candidateCanon = candidate.getCanonicalPath();
            return candidateCanon.equals(baseCanon) || candidateCanon.startsWith(baseCanon + File.separator);
        } catch (Exception e) {
            return false;
        }
    }

    private void addRepositoryIfUnique(String name, File repoDir) {
        try {
            File canon = repoDir.getCanonicalFile();
            for (File existing : repositories.values()) {
                if (existing.getCanonicalFile().equals(canon)) {
                    return; // Already registered
                }
            }
            repositories.put(name, repoDir);
        } catch (Exception e) {
            repositories.put(name, repoDir);
        }
    }

    @Override
    protected TaskResult preValidate(SelfDevContext context) throws Exception {
        if (repositories.isEmpty()) {
            return TaskResult.failure(id, "[GIT_CHECK] Pre-validation failed: No target repositories resolved.", null);
        }
        if (!repositories.containsKey("evolution")) {
            return TaskResult.failure(id, "[GIT_CHECK] Pre-validation failed: Canonical 'evolution' repository must be resolved. Found: " + repositories.keySet(), null);
        }
        return new TaskResult.Builder(id)
                .status(TaskStatus.READY)
                .message("Global Git Check initialized for " + repositories.size() + " repositories (" + String.join(", ", repositories.keySet()) + ").")
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
            String workingTreeStatus = sourceProvider.getStatus(repoPath);
            boolean revisionChanged = !revisionBefore.equalsIgnoreCase(revisionAfter);

            log("[GIT_CHECK] revisionAfter=" + revisionAfter);
            log("[GIT_CHECK] revisionChanged=" + revisionChanged);
            log("[GIT_CHECK] workingTreeStatus=" + workingTreeStatus);

            if ("FAILED".equalsIgnoreCase(workingTreeStatus)) {
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
    }
}
