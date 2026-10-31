package eu.kalafatic.evolution.controller.orchestration.develop.vcs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutor;
import eu.kalafatic.evolution.controller.orchestration.develop.executor.ProcessExecutorImpl;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitManager;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

/**
 * Concrete GitService implementation using GitVersionControlProvider and GitManager.
 */
public class GitServiceImpl implements GitService {

    private final GitVersionControlProvider gitProvider = new GitVersionControlProvider();
    private final ProcessExecutor processExecutor = new ProcessExecutorImpl();

    @Override
    public void cloneRepository(String repoUrl, String branch, File destinationDir) throws Exception {
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            ensureInitialCommit(destinationDir);
            return;
        }

        String targetBranch = (branch != null && !branch.trim().isEmpty()) ? branch.trim() : "main";
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        try {
            ProcessExecutor.ProcessResult res = processExecutor.execute("git clone --branch " + targetBranch + " " + repoUrl.trim() + " .", destinationDir, null, 120000);
            if (!res.isSuccess()) {
                processExecutor.execute("git clone " + repoUrl.trim() + " .", destinationDir, null, 120000);
            }
        } catch (Exception ex) {
            ensureInitialCommit(destinationDir);
        }
    }

    @Override
    public void createAndCheckoutBranch(File repoDir, String branchName) throws Exception {
        GitManager gitManager = new GitManager(repoDir);
        gitManager.ensureInitialCommit();
        if (branchName != null && !branchName.trim().isEmpty()) {
            try {
                gitManager.createBranch(branchName.trim());
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getDiff(File repoDir, String revision) throws Exception {
        if (!repoDir.exists()) return "";
        String ref = (revision != null && !revision.trim().isEmpty()) ? revision.trim() : "HEAD";
        return gitProvider.getDiff(repoDir, ref);
    }

    @Override
    public List<String> getChangedFiles(File repoDir, String revision) throws Exception {
        if (!repoDir.exists()) return new ArrayList<>();
        String ref = (revision != null && !revision.trim().isEmpty()) ? revision.trim() : "HEAD";
        return gitProvider.getChangedFiles(repoDir, ref);
    }

    @Override
    public void commitChanges(File repoDir, String message) throws Exception {
        if (!repoDir.exists()) throw new IllegalStateException("Repository directory does not exist: " + repoDir);
        String msg = (message != null && !message.trim().isEmpty()) ? message.trim() : "Develop autonomous update";
        gitProvider.commitChanges(repoDir, msg);
    }

    @Override
    public void push(File repoDir, String remote, String branch) throws Exception {
        if (!repoDir.exists()) throw new IllegalStateException("Repository directory does not exist: " + repoDir);
        gitProvider.push(repoDir);
    }

    @Override
    public boolean isGitRepository(File repoDir) {
        GitManager gitManager = new GitManager(repoDir);
        return gitManager.isGitRepository();
    }

    @Override
    public void ensureInitialCommit(File repoDir) throws Exception {
        GitManager gitManager = new GitManager(repoDir);
        gitManager.ensureInitialCommit();
    }

    @Override
    public String createPullRequest(File repoDir, String branch, String targetBranch, String title, String body, String gitHubToken) throws Exception {
        if (repoDir == null || !repoDir.exists()) {
            throw new IllegalArgumentException("Repository directory does not exist: " + repoDir);
        }

        String remoteUrl = "";
        try {
            ProcessExecutor.ProcessResult res = processExecutor.execute("git config --get remote.origin.url", repoDir, null, 10000);
            if (res.isSuccess()) {
                remoteUrl = res.getStdout().trim();
            }
        } catch (Exception ignored) {}

        String repoSlug = "";
        if (remoteUrl.contains("github.com")) {
            int idx = remoteUrl.indexOf("github.com");
            String pathPart = remoteUrl.substring(idx + 10);
            if (pathPart.startsWith(":") || pathPart.startsWith("/")) {
                pathPart = pathPart.substring(1);
            }
            if (pathPart.endsWith(".git")) {
                pathPart = pathPart.substring(0, pathPart.length() - 4);
            }
            repoSlug = pathPart;
        }

        if (gitHubToken != null && !gitHubToken.trim().isEmpty() && !repoSlug.isEmpty()) {
            // Attempt GitHub REST API PR creation
            try {
                java.net.URI uri = java.net.URI.create("https://api.github.com/repos/" + repoSlug + "/pulls");
                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("title", title != null ? title : "Develop Task PR");
                payload.put("head", branch);
                payload.put("base", targetBranch != null ? targetBranch : "main");
                payload.put("body", body != null ? body : "Autonomous PR prepared by EVO Develop.");

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Authorization", "Bearer " + gitHubToken.trim())
                        .header("Accept", "application/vnd.github+json")
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 201 || resp.statusCode() == 200) {
                    org.json.JSONObject respObj = new org.json.JSONObject(resp.body());
                    return respObj.optString("html_url", "");
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback: GitHub web compare/PR URL
        if (!repoSlug.isEmpty()) {
            String base = (targetBranch != null && !targetBranch.trim().isEmpty()) ? targetBranch.trim() : "main";
            return "https://github.com/" + repoSlug + "/compare/" + base + "..." + branch + "?expand=1";
        }

        return "pr-prepared://" + branch;
    }
}
